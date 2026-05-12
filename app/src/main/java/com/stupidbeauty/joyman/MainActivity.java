    /**
     * 检查电池优化状态并引导用户配置
     * 仅在 Android 6.0+ 上有效
     * 使用增强的引导对话框，支持努比亚双层检测
     */
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return; // Android 6.0 以下没有电池优化机制
        }
        
        if (isFinishing()) {
            return;
        }
        
        // 使用增强的引导对话框（包含努比亚特别说明）
        BatteryOptimizationHelper.showEnhancedGuideDialog(this);
    }