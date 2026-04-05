package com.stupidbeauty.joyman;

import android.app.Application;
import android.util.Log;

import com.stupidbeauty.crashdetector.CrashHandler;
import com.stupidbeauty.joyman.api.ApiForegroundService;
import com.stupidbeauty.joyman.api.ApiManager;
import com.stupidbeauty.joyman.api.BatteryOptimizationHelper;
import com.stupidbeauty.joyman.util.LogUtils;

/**
 * JoyMan Application class.
 * Initializes global crash detector and REST API service with foreground service.
 */
public class JoymanApplication extends Application {
    
    private static final String TAG = "JoymanApplication";
    private ApiManager apiManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i(TAG, "onCreate: JoyMan Application starting...");
        
        // Initialize global crash detector - logs crashes to external storage
        try {
            CrashHandler.init(this);
            Log.i(TAG, "✅ Crash Detector initialized (v2026.4.5)");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize CrashHandler", e);
        }
        
        // Initialize and start REST API service with foreground service
        try {
            apiManager = ApiManager.getInstance(this);
            
            if (apiManager.isApiEnabled()) {
                // Check battery optimization status
                if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
                    Log.w(TAG, "⚠️ Battery optimization is enabled, requesting ignore...");
                    // Auto-open settings to guide user to disable battery optimization
                    BatteryOptimizationHelper.openBrandSpecificSettings(this);
                }
                
                // Start foreground service for API
                ApiForegroundService.start(this);
                
                Log.i(TAG, "✅ REST API foreground service started");
                Log.i(TAG, "📡 API URL: http://localhost:" + apiManager.getApiPort());
                Log.i(TAG, "🔐 Authentication: HTTP Basic Auth (admin/admin)");
                Log.i(TAG, "💡 Tip: Keep app in foreground or disable battery optimization");
            } else {
                Log.i(TAG, "ℹ️ REST API is disabled (enable in settings)");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize REST API service", e);
        }
        
        Log.i(TAG, "JoyMan Application initialized successfully");
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        Log.i(TAG, "onTerminate: Stopping services...");
        
        // Stop foreground service
        ApiForegroundService.stop(this);
        Log.i(TAG, "✅ REST API foreground service stopped");
        
        Log.i(TAG, "JoyMan Application terminated");
    }
}