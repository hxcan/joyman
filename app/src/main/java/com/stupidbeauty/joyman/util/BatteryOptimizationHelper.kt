package com.stupidbeauty.joyman.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.stupidbeauty.joyman.R

/**
 * 电池优化辅助工具
 * 
 * 用于检测和应用是否被系统电池优化限制，并引导用户手动关闭优化。
 * 
 * 问题背景：
 * 国产 Android ROM（如努比亚、小米、华为等）默认对后台应用进行严格的电池优化，
 * 导致即使配置了前台服务，应用在后台时网络访问仍可能被系统阻断。
 * 
 * 解决方案：
 * 1. 检测应用是否被电池优化限制
 * 2. 引导用户跳转到系统设置页面
 * 3. 用户手动将应用设置为"不管控"或"无限制"模式
 */
object BatteryOptimizationHelper {

    /**
     * 检查应用是否忽略了电池优化
     * 
     * @param context 上下文
     * @return true 如果已忽略电池优化（即未被限制），false 如果仍受限制
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            // Android 6.0 以下没有电池优化机制，视为已忽略
            true
        }
    }

    /**
     * 获取跳转到电池优化设置页面的 Intent
     * 
     * @param context 上下文
     * @return 可启动的 Intent，用于跳转到系统设置页面
     */
    fun getBatteryOptimizationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            // Android 6.0 以下跳转到应用详情页面
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /**
     * 显示电池优化引导对话框
     * 
     * @param context Activity 上下文
     * @param onDismiss 对话框关闭后的回调
     */
    fun showBatteryOptimizationGuideDialog(
        context: androidx.appcompat.app.AppCompatActivity,
        onDismiss: (() -> Unit)? = null
    ) {
        if (!isIgnoringBatteryOptimizations(context)) {
            AlertDialog.Builder(context)
                .setTitle("电池优化限制检测")
                .setMessage(
                    "检测到系统对 JoyMan 启用了电池优化，这可能导致后台网络访问被限制。\n\n" +
                    "请点击\"去设置\"，然后将电池策略设置为\"不管控\"或\"无限制\"，\n" +
                    "以确保 API 服务能够持续运行。"
                )
                .setPositiveButton("去设置") { _, _ ->
                    val intent = getBatteryOptimizationSettingsIntent(context)
                    context.startActivity(intent)
                }
                .setNegativeButton("稍后提醒") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .setCancelable(false)
                .show()
        } else {
            onDismiss?.invoke()
        }
    }

    /**
     * 检查并引导用户关闭电池优化
     * 
     * @param context Activity 上下文
     * @return true 如果已忽略电池优化或用户已完成设置，false 如果仍需用户操作
     */
    fun checkAndGuide(context: androidx.appcompat.app.AppCompatActivity): Boolean {
        val isIgnored = isIgnoringBatteryOptimizations(context)
        
        if (!isIgnored) {
            showBatteryOptimizationGuideDialog(context)
        }
        
        return isIgnored
    }
}