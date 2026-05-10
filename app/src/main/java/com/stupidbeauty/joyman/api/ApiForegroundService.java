package com.stupidbeauty.joyman.api;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Service;
import android.os.IBinder;

import com.stupidbeauty.joyman.MainActivity;
import com.stupidbeauty.joyman.util.LogUtils;

/**
 * API 前台服务
 */
public class ApiForegroundService extends Service {
    
    private static final String TAG = "ApiForegroundService";
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
        
        // 注意：不再在这里检查通知或显示对话框
        // 对话框应该在 MainActivity 中显示
        
        Intent intent = new Intent(context, ApiForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    /**
     * 检查应用的通知权限是否被用户或系统禁用
     * 可以在任何 Context 中调用
     */
    public static boolean isNotificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            boolean enabled = manager.areNotificationsEnabled();
            
            LogUtils logUtils = LogUtils.getInstance();
            if (logUtils != null) {
                logUtils.d(TAG, "isNotificationsEnabled: " + enabled);
                
                if (!enabled) {
                    logUtils.e(TAG, "❌ NOTIFICATIONS ARE DISABLED!");
                    logUtils.e(TAG, "💡 User must manually enable in Settings → Apps → JoyMan → Notifications");
                }
            }
            
            return enabled;
        }
        
        return true;
    }
    
    /**
     * 在 MainActivity 中显示通知设置对话框
     * 必须在 Activity 环境中调用
     */
    public static void showNotificationSettingsDialog(MainActivity activity) {
        LogUtils logUtils = LogUtils.getInstance();
        if (logUtils != null) {
            logUtils.w(TAG, "📢 Showing notification settings dialog in MainActivity...");
        }
        
        if (activity == null || activity.isFinishing()) {
            if (logUtils != null) {
                logUtils.e(TAG, "❌ Cannot show dialog: Activity is null or finishing");
            }
            return;
        }
        
        try {
            new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("⚠️ 通知权限已关闭")
                .setMessage(
                    "JoyMan 需要通知权限来保持 API 服务在前台运行。\n\n" +
                    "检测到您的设备已关闭 JoyMan 的通知权限，这会导致：\n" +
                    "• ❌ 无法看到 API 服务运行状态\n" +
                    "• ❌ 服务可能被系统自动杀死\n" +
                    "• ❌ 日志功能可能受影响\n\n" +
                    "请点击「去设置」手动开启通知权限：\n" +
                    "1. 点击「允许通知」开关\n" +
                    "2. 确保「JoyMan API 服务」渠道已启用\n" +
                    "3. 将重要性设置为「高」或「紧急」\n\n" +
                    "💡 提示：这是 Android 系统的安全机制，需要用户手动授权。"
                )
                .setPositiveButton("⚙️ 去设置", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                    intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, CHANNEL_ID);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                    
                    if (logUtils != null) {
                        logUtils.i(TAG, "✅ Opened notification settings page");
                    }
                })
                .setNegativeButton("❌ 取消", (dialog, which) -> {
                    if (logUtils != null) {
                        logUtils.w(TAG, "⚠️ User declined to open notification settings");
                    }
                    Toast.makeText(activity, 
                        "⚠️ 通知未开启，API 服务可能无法正常运行", 
                        Toast.LENGTH_LONG).show();
                })
                .setNeutralButton("❓ 为什么需要通知？", (dialog, which) -> {
                    new androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setTitle("为什么需要通知权限？")
                        .setMessage(
                            "Android 系统要求前台服务必须显示持久通知，这是为了：\n\n" +
                            "✅ 透明度：让用户知道有服务在后台运行\n" +
                            "✅ 可控性：用户可以随时通过通知管理或停止服务\n" +
                            "✅ 电池优化：系统可以智能管理后台服务的电量消耗\n\n" +
                            "JoyMan 的 API 服务需要持续运行以提供 REST API 接口，" +
                            "因此必须显示通知来告知用户服务正在运行。\n\n" +
                            "🔒 隐私说明：该通知不会收集或泄露任何个人信息。"
                        )
                        .setPositiveButton("明白了", null)
                        .show();
                })
                .setCancelable(false)
                .show();
                
            if (logUtils != null) {
                logUtils.i(TAG, "✅ Dialog shown successfully");
            }
        } catch (Exception e) {
            if (logUtils != null) {
                logUtils.e(TAG, "❌ Failed to show dialog", e);
            }
            Toast.makeText(activity, "⚠️ 请手动到设置中开启通知权限", Toast.LENGTH_LONG).show();
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
        
        // 检查但不显示对话框（对话框已在 MainActivity 中显示）
        if (!isNotificationsEnabled(this)) {
            logUtils.e(TAG, "⚠️️ CRITICAL: Notifications are DISABLED!");
            Toast.makeText(this, 
                "⚠️ 通知权限未开启！请下拉到设置中手动开启", 
                Toast.LENGTH_LONG).show();
        }
        
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
    
    private void deleteNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
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
                    
                    if (channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                        logUtils.e(TAG, "❌ CRITICAL: Channel importance is NONE!");
                    }
                    
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
            
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "JoyMan API 服务",
                NotificationManager.IMPORTANCE_HIGH
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
                
                NotificationChannel createdChannel = manager.getNotificationChannel(CHANNEL_ID);
                if (createdChannel != null) {
                    if (createdChannel.getImportance() == NotificationManager.IMPORTANCE_HIGH) {
                        logUtils.i(TAG, "✅ SUCCESS: Channel importance correctly set to HIGH (4)");
                    } else {
                        logUtils.e(TAG, "❌ FAILED: Expected importance 4 but got " + createdChannel.getImportance());
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