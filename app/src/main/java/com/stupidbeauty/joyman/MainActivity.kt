package com.stupidbeauty.joyman

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.stupidbeauty.joyman.util.BatteryOptimizationHelper

/**
 * 主 Activity
 * 
 * 应用启动入口，负责显示任务列表和初始化核心功能。
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 延迟检测电池优化状态，避免干扰 UI 初始化
        Handler(Looper.getMainLooper()).postDelayed({
            checkBatteryOptimization()
        }, 500L)
    }

    /**
     * 检查电池优化状态并引导用户配置
     */
    private fun checkBatteryOptimization() {
        // 如果已忽略电池优化，则不显示提示
        if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            return
        }

        // 显示引导对话框
        BatteryOptimizationHelper.showBatteryOptimizationGuideDialog(this)
    }

    override fun onResume() {
        super.onResume()
        // 用户从设置页面返回后，可以再次检查状态（可选）
        // 如果需要自动刷新 UI，可在此处添加逻辑
    }
}