package com.stupidbeauty.joyman;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.stupidbeauty.crashdetector.CrashHandler;
import com.stupidbeauty.joyman.api.ApiForegroundService;
import com.stupidbeauty.joyman.api.ApiManager;
import com.stupidbeauty.joyman.api.BatteryOptimizationHelper;
import com.stupidbeauty.joyman.util.LogUtils;

/**
 * JoyMan Application class.
 * Initializes global crash detector and REST API service with foreground service.
 * Auto-enables API in development mode for faster testing.
 */
public class JoymanApplication extends Application {
    
    private static final String TAG = "JoymanApplication";
    private static final String PREF_NAME = "joyman_settings";
    private static final String KEY_FIRST_LAUNCH = "first_launch_with_api";
    
    private ApiManager apiManager;
    private LogUtils logUtils;
    
    @Override
    public void onCreate() {
        super.onCreate();
        logUtils = LogUtils.getInstance();
        
        logUtils.i(TAG, "onCreate: JoyMan Application starting...");
        
        // Initialize global crash detector
        try {
            CrashHandler.init(this);
            logUtils.i(TAG, "✅ Crash Detector initialized (v2026.4.5)");
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to initialize CrashHandler", e);
        }
        
        // Initialize and start REST API service
        try {
            apiManager = ApiManager.getInstance(this);
            
            // Development mode: Auto-enable API on first launch
            boolean isFirstLaunch = isFirstLaunchWithApi();
            logUtils.d(TAG, "isFirstLaunchWithApi: " + isFirstLaunch);
            
            if (isFirstLaunch || apiManager.isApiEnabled()) {
                // Enable API if not already enabled
                if (!apiManager.isApiEnabled()) {
                    apiManager.setApiEnabled(true);
                    logUtils.i(TAG, "🔧 Development mode: Auto-enabled REST API");
                }
                
                // Check battery optimization status
                if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
                    logUtils.w(TAG, "⚠️ Battery optimization is enabled, requesting ignore...");
                    
                    // Show toast to inform user
                    Toast.makeText(this, 
                        "正在启动 REST API 服务...\n请允许关闭电池优化以确保后台运行", 
                        Toast.LENGTH_LONG).show();
                    
                    // Auto-open settings to guide user
                    BatteryOptimizationHelper.openBrandSpecificSettings(this);
                }
                
                // Start foreground service for API
                ApiForegroundService.start(this);
                
                logUtils.i(TAG, "✅ REST API foreground service started");
                logUtils.i(TAG, "📡 API URL: http://localhost:" + apiManager.getApiPort());
                logUtils.i(TAG, "🔐 Authentication: HTTP Basic Auth (admin/admin)");
                logUtils.i(TAG, "💡 Tip: Keep app in foreground or disable battery optimization");
                
                // Show success toast
                Toast.makeText(this, 
                    "REST API 已启动\n访问：http://localhost:" + apiManager.getApiPort(), 
                    Toast.LENGTH_LONG).show();
                
                // Mark as not first launch anymore
                setFirstLaunchCompleted();
                
            } else {
                logUtils.i(TAG, "ℹ️ REST API is disabled (enable in settings)");
            }
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to initialize REST API service", e);
        }
        
        logUtils.i(TAG, "JoyMan Application initialized successfully");
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        logUtils.i(TAG, "onTerminate: Stopping services...");
        
        // Stop foreground service
        ApiForegroundService.stop(this);
        logUtils.i(TAG, "✅ REST API foreground service stopped");
        
        logUtils.i(TAG, "JoyMan Application terminated");
    }
    
    /**
     * Check if this is the first launch with API feature
     */
    private boolean isFirstLaunchWithApi() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    /**
     * Mark first launch as completed
     */
    private void setFirstLaunchCompleted() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        logUtils.d(TAG, "First launch marked as completed");
    }
}