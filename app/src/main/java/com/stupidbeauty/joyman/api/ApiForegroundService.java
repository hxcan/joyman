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

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.stupidbeauty.joyman.MainActivity;
import com.stupidbeauty.joyman.R;
import com.stupidbeauty.joyman.util.LogUtils;

/**
 * API 前台服务
 * 
 * 将 REST API 服务以前台服务方式运行，避免被系统杀死
 * 显示持久通知，告知用户 API 正在运行
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.2
 * @since 2026-04-06
 */
public class ApiForegroundService extends Service {
    
    private static final String TAG = "ApiForegroundService";
    private static final String CHANNEL_ID = "joyman_api_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int DEFAULT_PORT = 8080;
    
    private JoyManApiService apiService;
    private LogUtils logUtils;
    
    /**
     * 启动服务的方法
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, ApiForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    /**
     * 停止服务的方法
     */
    public static void stop(Context context) {
        Intent intent = new Intent(context, ApiForegroundService.class);
        context.stopService(intent);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        logUtils = LogUtils.getInstance();
        logUtils.i(TAG, "onCreate: API Foreground Service created");
        
        // 创建通知渠道（必须在显示通知前创建）
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logUtils.i(TAG, "onStartCommand: Starting API foreground service");
        
        // 检查通知权限（Android 13+）
        // 注意：不在 Service 中请求权限，只在 MainActivity 中请求
        if (!checkNotificationPermission()) {
            logUtils.w(TAG, "onStartCommand: Notification permission not granted");
            // 即使权限未授予，也尝试启动服务（旧版本不需要此权限）
        }
        
        // 创建通知
        Notification notification = createNotification();
        
        // 以前台服务方式启动
        startForeground(NOTIFICATION_ID, notification);
        
        // 启动 API 服务
        try {
            apiService = new JoyManApiService(this, DEFAULT_PORT);
            apiService.startService();
            logUtils.i(TAG, "onStartCommand: JoyMan API server started on port " + DEFAULT_PORT);
        } catch (Exception e) {
            logUtils.e(TAG, "onStartCommand: Failed to start API server", e);
        }
        
        // 如果服务被杀死，自动重启
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        logUtils.i(TAG, "onDestroy: Stopping API foreground service");
        
        // 停止 API 服务
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
     * 检查通知权限（Android 13+）
     * 注意：只检查权限，不请求权限。权限请求应该在 Activity 中进行。
     */
    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            logUtils.d(TAG, "checkNotificationPermission: " + hasPermission);
            return hasPermission;
        }
        // Android 12 及以下不需要此权限
        return true;
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "JoyMan API 服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("REST API 服务运行状态通知");
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                logUtils.d(TAG, "createNotificationChannel: Channel created");
            } else {
                logUtils.e(TAG, "createNotificationChannel: NotificationManager is null");
            }
        } else {
            logUtils.d(TAG, "createNotificationChannel: Not needed for API < 26");
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        // 点击通知打开主界面
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JoyMan API 服务")
            .setContentText("REST API 正在运行于端口 " + DEFAULT_PORT)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 使用系统默认图标
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        Notification notification = builder.build();
        logUtils.d(TAG, "createNotification: Notification created");
        
        return notification;
    }
}