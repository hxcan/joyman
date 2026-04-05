package com.stupidbeauty.joyman.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.stupidbeauty.joyman.util.LogUtils;

/**
 * 电池优化帮助工具
 * 
 * 检测和引导用户关闭电池优化，确保 API 服务在后台持续运行
 * 支持各大手机品牌的特定设置页面
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-04-06
 */
public class BatteryOptimizationHelper {
    
    private static final String TAG = "BatteryOptimizationHelper";
    private static final LogUtils logUtils = LogUtils.getInstance();
    
    /**
     * 检查是否忽略了电池优化
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                boolean ignoring = pm.isIgnoringBatteryOptimizations(context.getPackageName());
                logUtils.d(TAG, "isIgnoringBatteryOptimizations: " + ignoring);
                return ignoring;
            }
        }
        // Android 6.0 以下默认忽略电池优化
        return true;
    }
    
    /**
     * 请求忽略电池优化
     * 会跳转到系统设置页面
     */
    public static void requestIgnoreBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                logUtils.i(TAG, "requestIgnoreBatteryOptimizations: Opened system settings");
            } catch (Exception e) {
                logUtils.e(TAG, "requestIgnoreBatteryOptimizations: Failed to open settings", e);
                // 如果通用方式失败，尝试品牌特定方式
                openBrandSpecificSettings(context);
            }
        }
    }
    
    /**
     * 打开应用详细信息页面
     * 用户可以在此页面手动关闭电池优化
     */
    public static void openAppDetailsSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            logUtils.i(TAG, "openAppDetailsSettings: Opened app details");
        } catch (Exception e) {
            logUtils.e(TAG, "openAppDetailsSettings: Failed to open settings", e);
        }
    }
    
    /**
     * 打开品牌特定的设置页面
     * 支持小米、华为、OPPO、vivo 等品牌
     */
    public static void openBrandSpecificSettings(Context context) {
        String brand = Build.BRAND.toLowerCase();
        logUtils.d(TAG, "openBrandSpecificSettings: Brand = " + brand);
        
        try {
            if (brand.contains("xiaomi") || brand.contains("redmi")) {
                // 小米/红米手机
                openXiaomiSettings(context);
            } else if (brand.contains("huawei")) {
                // 华为手机
                openHuaweiSettings(context);
            } else if (brand.contains("oppo")) {
                // OPPO 手机
                openOppoSettings(context);
            } else if (brand.contains("vivo")) {
                // vivo 手机
                openVivoSettings(context);
            } else if (brand.contains("samsung")) {
                // 三星手机
                openSamsungSettings(context);
            } else {
                // 其他品牌，打开应用详情页面
                openAppDetailsSettings(context);
            }
        } catch (Exception e) {
            logUtils.e(TAG, "openBrandSpecificSettings: Failed, fallback to app details", e);
            openAppDetailsSettings(context);
        }
    }
    
    /**
     * 小米手机设置
     */
    private static void openXiaomiSettings(Context context) {
        try {
            // MIUI 省电策略设置
            Intent intent = new Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST_MENU");
            intent.setClassName("com.miui.securitycenter", 
                "com.miui.permcenter.autostart.AutoStartManagementActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            logUtils.i(TAG, "openXiaomiSettings: Opened MIUI power settings");
        } catch (Exception e) {
            logUtils.w(TAG, "openXiaomiSettings: MIUI specific intent failed, trying alternative");
            try {
                // 备用方案：打开应用详情
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ex) {
                logUtils.e(TAG, "openXiaomiSettings: All attempts failed", ex);
            }
        }
    }
    
    /**
     * 华为手机设置
     */
    private static void openHuaweiSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.huawei.systemmanager", 
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            logUtils.i(TAG, "openHuaweiSettings: Opened Huawei startup settings");
        } catch (Exception e) {
            logUtils.w(TAG, "openHuaweiSettings: Huawei specific intent failed");
            openAppDetailsSettings(context);
        }
    }
    
    /**
     * OPPO 手机设置
     */
    private static void openOppoSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.coloros.safecenter", 
                "com.coloros.safecenter.permission.startup.StartupAppListActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            logUtils.i(TAG, "openOppoSettings: Opened OPPO startup settings");
        } catch (Exception e) {
            logUtils.w(TAG, "openOppoSettings: OPPO specific intent failed");
            openAppDetailsSettings(context);
        }
    }
    
    /**
     * vivo 手机设置
     */
    private static void openVivoSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.vivo.permissionmanager", 
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            logUtils.i(TAG, "openVivoSettings: Opened vivo startup settings");
        } catch (Exception e) {
            logUtils.w(TAG, "openVivoSettings: vivo specific intent failed");
            openAppDetailsSettings(context);
        }
    }
    
    /**
     * 三星手机设置
     */
    private static void openSamsungSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.samsung.android.lool", 
                "com.samsung.android.sm.ui.battery.BatteryActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            logUtils.i(TAG, "openSamsungSettings: Opened Samsung battery settings");
        } catch (Exception e) {
            logUtils.w(TAG, "openSamsungSettings: Samsung specific intent failed");
            openAppDetailsSettings(context);
        }
    }
    
    /**
     * 显示引导对话框（可选）
     * 在实际使用中，可以结合 AlertDialog 使用
     */
    public interface BatteryOptimizationCallback {
        void onOpenedSettings();
        void onFailed(Exception e);
    }
    
    /**
     * 请求忽略电池优化，带回调
     */
    public static void requestIgnoreBatteryOptimizations(Context context, 
                                                         BatteryOptimizationCallback callback) {
        try {
            requestIgnoreBatteryOptimizations(context);
            if (callback != null) {
                callback.onOpenedSettings();
            }
        } catch (Exception e) {
            logUtils.e(TAG, "requestIgnoreBatteryOptimizations: Failed", e);
            if (callback != null) {
                callback.onFailed(e);
            }
        }
    }
}