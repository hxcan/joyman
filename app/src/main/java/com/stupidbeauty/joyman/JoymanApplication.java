package com.stupidbeauty.joyman;

import android.app.Application;
import android.util.Log;

import com.stupidbeauty.crashdetector.CrashHandler;
import com.stupidbeauty.joyman.api.ApiManager;
import com.stupidbeauty.joyman.util.LogUtils;

/**
 * JoyMan Application class.
 * Initializes global crash detector and REST API service.
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
        
        // Initialize and start REST API service
        try {
            apiManager = ApiManager.getInstance(this);
            
            if (apiManager.isApiEnabled()) {
                apiManager.startApiService();
                Log.i(TAG, "✅ REST API server started on port " + apiManager.getApiPort());
                Log.i(TAG, "📡 API URL: " + apiManager.getApiUrl());
                Log.i(TAG, "🔑 API Key: " + (apiManager.getApiKey() != null ? 
                    apiManager.getApiKey().substring(0, 8) + "..." : "Not generated"));
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
        
        // Stop API service
        if (apiManager != null) {
            apiManager.stopApiService();
            Log.i(TAG, "✅ REST API server stopped");
        }
        
        Log.i(TAG, "JoyMan Application terminated");
    }
}