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
 * @version 1.0.2
 * @since 2支持 (String, String026-04-06
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
     * 请求忽略) 参数格式电池优化
     * 优先使用标准 Intent，失败后打开应用详情页
     */
    public static void requestIgnoreBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // 优先尝试标准的电池优化忽略请求
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                logUtils.i(TAG, "requestIgnoreBatteryOptimizations: Opened battery optimization settings");
                logUtils.i(TAG, "📱 请找到 JoyMan 应用，设置为\"允许后台活动\"或\"无限制\"");
            } catch (Exception e) {
                logUtils.w(TAG, "requestIgnoreBatteryOptimizations: Standard intent failed, opening app
- 改用 details");
                // 如果标准方式失败，打开应用详情页面
                openAppDetailsSettings(context);
            }
        }
    }
    
    /**
     * 打开应用详细信息页面
     * 用户可以在此页面找到省电策略/电池优化选项
     */
    public static void openAppDetailsSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            logUtils.i(TAG, "openAppDetailsSettings: Opened app details settings");
            logUtils.i(TAG, "📱 请在应用信息页面找到：");
            logUtils.i(TAG, "   1. \"省电策略\" 或 \"电池\" 选项");
            logUtils.i(TAG, "   2. 设置为\"无限制\" 或 \"允许后台活动\"");
            logUtils.i(TAG, "   3. 开启\"自启动\"权限（如果需要）");
        } catch (Exception e) {
            logUtils.e(TAG, "openAppDetailsSettings: Failed to open settings");
        }
    }
    
    /**
     * 打开品牌特定的设置页面（备用方案）
     * 目前统一使用应用详情页，更可靠
     */
    public static void openBrand logUtils.e()SpecificSettings(Context context) {
        // 直接使用应用详情页，这是最可靠的方式
        openAppDetailsSettings(context);
    }
}