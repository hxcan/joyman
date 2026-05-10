package com.stupidbeauty.joyman.api;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.stupidbeauty.joyman.MainActivity;
import com.stupidbeauty.joyman.util.LogUtils;

/**
 * API 前台服务
 */
public class ApiForegroundService extends Service {
    
    private static final String TAG = "ApiForegroundService";
    // 使用新的渠道 ID 绕过 Android 9 的渠道重要性缓存
    private static final String CHANNEL_ID = "joyman_api_v2";
    private static final int NOTIFICATION_ID = 1001;
    private static final int DEFAULT_PORT = 8080;
    
    private static boolean isRunning = false;
    
    private JoyManApiService apiService;
    private LogUtils logUtils;
    
    public static void start(Context context) {
        if (isRunning) {
            logUtilsInfo(context, "⚠️ Service already running");
            return;
        }
        logUtilsInfo(context, "🚀 Starting ApiForegroundService");
        Intent intent = new Intent(context, ApiForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    public static void stop(Context context) {
        Intent intent = new Intent(context, ApiForegroundService.class);
        context.stopService(intent);
    }
    
    private static void logUtilsInfo(Context context, String message) {
        try {
            LogUtils logUtils = LogUtils.getInstance();
            if (logUtils != null) {
                logUtils.i(TAG, message);
            } else {
                android.util.Log.i(TAG, message);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error getting LogUtils", e);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        logUtils = LogUtils.getInstance();
        logUtils.i(TAG, "✅ onCreate: API Foreground Service on Android " + Build.VERSION.RELEASE);
        
        if (!checkStoragePermission()) {
            logUtils.w(TAG, "❌ Storage permission not granted");
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
        }
        
        // 删除旧渠道并创建新渠道
        deleteNotificationChannel();
        createNotificationChannel();
        checkNotificationChannelStatus();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) {
            logUtils.w(TAG, "⚠️ Service already running");
            return START_STICKY;
        }
        
        isRunning = true;
        logUtils.i(TAG, "▶️ Starting API foreground service");
        
        // Toast 备用通知
        Toast.makeText(this, "JoyMan API 服务已启动", Toast.LENGTH_LONG).show();
        logUtils.i(TAG, "💬 Showing Toast");
        
        Notification notification = createNotification();
        
        logUtils.i(TAG, "🔔 Calling startForeground");
        startForeground(NOTIFICATION_ID, notification);
        logUtils.i(TAG, "✅ startForeground() called - service is FOREGROUND");
        
        try {
            apiService = new JoyManApiService(this, DEFAULT_PORT);
            apiService.startService();
            logUtils.i(TAG, "🚀 API server started on port " + DEFAULT_PORT);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("EADDRINUSE")) {
                logUtils.w(TAG, "⚠️ Port " + DEFAULT_PORT + " already in use");
            } else {
                logUtils.e(TAG, "❌ Failed to start API server", e);
            }
        }
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        logUtils.i(TAG, "⏹️ Stopping API foreground service");
        isRunning = false;
        if (apiService != null) {
            apiService.stopService();
            apiService = null;
        }
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * 删除旧的通知渠道（解决 Android 9 渠道重要性缓存问题）
     */
    private void deleteNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                // 删除新旧所有可能的渠道
                manager.deleteNotificationChannel("joyman_api_channel");
                manager.deleteNotificationChannel(CHANNEL_ID);
                logUtils.d(TAG, "🗑️ Deleted old notification channels");
            }
        }
    }
    
    private void checkNotificationChannelStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
                if (channel != null) {
                    logUtils.i(TAG, "📊 Channel Status:");
                    logUtils.i(TAG, "   - Name: " + channel.getName());
                    logUtils.i(TAG, "   - Importance: " + channel.getImportance() + 
                        " (4=HIGH ✓, 3=DEFAULT, 2=LOW ✗)");
                    logUtils.i(TAG, "   - Lockscreen Visibility: " + channel.getLockscreenVisibility());
                    logUtils.i(TAG, "   - Show Badge: " + channel.canShowBadge());
                    
                    postTestNotification(manager);
                } else {
                    logUtils.e(TAG, "❌ Notification channel is NULL!");
                }
            }
        }
    }
    
    private void postTestNotification(NotificationManager manager) {
        try {
            NotificationCompat.Builder testBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("JoyMan 测试通知")
                .setContentText("如果您能看到这条通知，说明通知渠道工作正常！")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
            
            manager.notify(9999, testBuilder.build());
            logUtils.i(TAG, "✅ Test notification posted (ID: 9999)");
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to post test notification", e);
        }
    }
    
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return android.os.Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        } else {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            logUtils.d(TAG, "📺 Creating notification channel (ID: " + CHANNEL_ID + ")...");
            
            // 使用 IMPORTANCE_HIGH 确保通知显示
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "JoyMan API 服务",
                NotificationManager.IMPORTANCE_HIGH  // 4 = HIGH
            );
            channel.setDescription("REST API 服务运行状态通知 - 保持服务在前台运行");
            channel.setShowBadge(true);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.enableLights(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                logUtils.i(TAG, "✅ Notification channel created with HIGH importance (4)");
                
                // 验证设置是否生效
                NotificationChannel createdChannel = manager.getNotificationChannel(CHANNEL_ID);
                if (createdChannel != null) {
                    if (createdChannel.getImportance() == NotificationManager.IMPORTANCE_HIGH) {
                        logUtils.i(TAG, "✅ SUCCESS: Channel importance correctly set to HIGH (4)");
                    } else {
                        logUtils.e(TAG, "❌ FAILED: Expected importance 4 but got " + createdChannel.getImportance());
                        logUtils.e(TAG, "💡 This may be due to ROM-specific restrictions");
                    }
                }
            } else {
                logUtils.e(TAG, "❌ NotificationManager is null!");
            }
        }
    }
    
    private Notification createNotification() {
        logUtils.d(TAG, "🔨 Building notification...");
        
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JoyMan API 服务")
            .setContentText("REST API 正在运行于端口 " + DEFAULT_PORT + " - 点击打开应用")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setDefaults(Notification.DEFAULT_LIGHTS);
        
        Notification notification = builder.build();
        logUtils.i(TAG, "✅ Notification built");
        
        return notification;
    }
}