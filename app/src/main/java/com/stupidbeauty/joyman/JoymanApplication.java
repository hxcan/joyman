package com.stupidbeauty.joyman;

import android.app.Application;
import android.util.Log;
import com.stupidbeauty.crashdetector.CrashHandler;

/**
 * JoyMan Application class.
 * Initializes global crash detector for exception handling.
 */
public class JoymanApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize global crash detector - logs crashes to external storage
        try {
            CrashHandler.init(this);
            Log.i("JoymanApplication", "✅ Crash Detector initialized (v2026.4.5)");
        } catch (Exception e) {
            Log.e("JoymanApplication", "❌ Failed to initialize CrashHandler", e);
        }
    }
}