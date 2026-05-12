package com.stupidbeauty.joyman.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import com.stupidbeauty.joyman.util.LogUtils;
import java.lang.reflect.Method;

/**
 * 电池优化帮助工具 - 增强版
 * 
 * 检测和引导用户关闭电池优化，确保 API 服务在后台持续运行
 * 支持各大手机品牌的特定设置页面，特别是努比亚的双层电池管理机制
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.3
 * @since 2026-04-06
 */
public class BatteryOptimizationHelper {
    
    private static final String TAG = "BatteryOptimizationHelper";
    private static final LogUtils logUtils = LogUtils.getInstance();
    
    /**
     * 检查是否忽略了电池优化
     * 注意：此方法只能检测 Android 标准层，无法检测厂商自定义层
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                boolean ignoring = pm.isIgnoringBatteryOptimizations(context.getPackageName());
                logUtils.d(TAG, "isIgnoringBatteryOptimizations (Android standard): " + ignoring);
                
                // 如果是努比亚 ROM，额外检查自定义层
                if (isNubiaRom()) {
                    int nubiaStatus = getNubiaBatteryOptimizationStatus(context);
                    logUtils.d(TAG, "Nubia battery optimization status: " + nubiaStatus + 
                               " (0=Unknown, 1=Unrestricted, 2=Smart, 3=Extreme)");
                    
                    // 只有两层都通过才算真正忽略
                    return ignoring && (nubiaStatus == 1 || nubiaStatus == 0);
                }
                
                return ignoring;
            }
        }
        // Android 6.0 以下默认忽略电池优化
        return true;
    }
    
    /**
     * 检测是否为努比亚 ROM
     */
    public static boolean isNubiaRom() {
        String brand = Build.BRAND;
        String manufacturer = Build.MANUFACTURER;
        String display = Build.DISPLAY;
        
        boolean isNubia = "nubia".equalsIgnoreCase(brand) || 
                         "nubia".equalsIgnoreCase(manufacturer) ||
                         (display != null && display.toLowerCase().contains("nubia"));
        
        logUtils.d(TAG, "isNubiaRom: " + isNubia + 
                   " (Brand=" + brand + ", Manufacturer=" + manufacturer + ")");
        
        return isNubia;
    }
    
    /**
     * 尝试获取努比亚电池优化状态
     * @return 0=未知，1=不管控，2=智能管控，3=极限管控
     */
    public static int getNubiaBatteryOptimizationStatus(Context context) {
        try {
            // 尝试反射调用努比亚私有 API
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                Class<?> pmClass = pm.getClass();
                
                // 尝试查找 PowerManagerEx 或类似类
                try {
                    Class<?> nubiaPmClass = Class.forName("android.os.PowerManagerEx");
                    Method getInstanceMethod = nubiaPmClass.getMethod("getInstance");
                    Object nubiaPm = getInstanceMethod.invoke(null);
                    
                    if (nubiaPm != null) {
                        Method getModeMethod = nubiaPmClass.getMethod(
                            "getBatteryOptimizationMode", 
                            String.class
                        );
                        int mode = (int) getModeMethod.invoke(nubiaPm, context.getPackageName());
                        logUtils.i(TAG, "Successfully got Nubia battery mode via reflection: " + mode);
                        return mode;
                    }
                } catch (Exception e) {
                    logUtils.d(TAG, "Reflection method failed (expected on some ROMs): " + e.getMessage());
                }
                
                // 尝试读取系统设置
                try {
                    android.content.ContentResolver resolver = context.getContentResolver();
                    int mode = Settings.System.getInt(
                        resolver,
                        "nubia_battery_optimization_mode_" + context.getPackageName(),
                        0
                    );
                    if (mode > 0) {
                        logUtils.i(TAG, "Successfully got Nubia battery mode from Settings: " + mode);
                        return mode;
                    }
                } catch (Exception e) {
                    logUtils.d(TAG, "Settings read failed (expected): " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logUtils.w(TAG, "Failed to get Nubia battery optimization status", e);
        }
        
        // 无法检测时返回 0（未知）
        return 0;
    }
    
    /**
     * 请求忽略电池优化
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
                logUtils.w(TAG, "requestIgnoreBatteryOptimizations: Standard intent failed, opening app details");
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
     * 显示增强的引导对话框（推荐在 Activity 中使用）
     * 包含详细的图文说明和分步指导
     */
    public static void showEnhancedGuideDialog(androidx.appcompat.app.AppCompatActivity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        
        boolean isNubia = isNubiaRom();
        
        StringBuilder message = new StringBuilder();
        message.append("检测到系统对 JoyMan 启用了电池优化，这可能导致后台网络访问被限制。\n\n");
        
        if (isNubia) {
            message.append("⚠️ 努比亚手机特别说明：\n");
            message.append("您的设备有【两层】电池管理机制：\n\n");
            message.append("第 1 层：Android 标准优化（已处理 ✅）\n");
            message.append("第 2 层：努比亚省电方案（需要手动检查 ⚠️）\n\n");
            message.append("请按以下步骤操作：\n");
            message.append("1. 点击「去设置」打开应用信息页面\n");
            message.append("2. 找到「省电方案」或「电池」选项\n");
            message.append("3. 选择「不管控」（最上方选项）\n");
            message.append("4. 确认已开启「自启动」权限\n\n");
            message.append("💡 提示：即使您之前点击过「允许后台运行」，\n");
            message.append("   仍需手动将省电方案改为「不管控」才能完全解除限制。\n");
        } else {
            message.append("请点击「去设置」，然后将电池策略设置为「不管控」或「无限制」，\n");
            message.append("以确保 API 服务能够持续运行。\n\n");
            message.append("💡 提示：这是国产 ROM（小米、华为、OPPO、vivo 等）的常见设置。\n");
        }
        
        new AlertDialog.Builder(activity)
            .setTitle("⚠️ 电池优化限制检测")
            .setMessage(message.toString())
            .setPositiveButton("⚙️ 去设置", (dialog, which) -> {
                openAppDetailsSettings(activity);
                logUtils.i(TAG, "User clicked to open settings");
            })
            .setNegativeButton("❌ 稍后提醒", (dialog, which) -> {
                dialog.dismiss();
                logUtils.w(TAG, "User declined to configure battery optimization");
            })
            .setNeutralButton("❓ 为什么需要？", (dialog, which) -> {
                showDetailedExplanationDialog(activity);
            })
            .setCancelable(false)
            .show();
            
        logUtils.i(TAG, "Enhanced guide dialog shown (isNubia=" + isNubia + ")");
    }
    
    /**
     * 显示详细说明对话框
     */
    private static void showDetailedExplanationDialog(androidx.appcompat.app.AppCompatActivity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("为什么需要关闭电池优化？")
            .setMessage(
                "JoyMan 的 API 服务需要在后台持续运行，以便未来姐姐等应用可以随时访问。\n\n" +
                "Android 系统和手机厂商为了省电，会对后台应用进行限制：\n\n" +
                "• 限制网络访问\n" +
                "• 限制 CPU 使用\n" +
                "• 自动杀死后台进程\n\n" +
                "关闭电池优化后：\n" +
                "✅ API 服务可以持续运行\n" +
                "✅ 后台网络访问不受限制\n" +
                "✅ 不会被系统自动杀死\n\n" +
                "⚠️ 耗电量会增加一些，但这是保证功能正常所必需的。"
            )
            .setPositiveButton("明白了", null)
            .show();
    }
}