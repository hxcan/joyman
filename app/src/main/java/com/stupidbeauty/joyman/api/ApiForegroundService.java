    /**
     * 检查通知权限（Android 13+）
     */
    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            logUtils.d(TAG, "checkNotificationPermission: " + hasPermission);
            return hasPermission;
        }
        // Android 12 及以下不需要此权限
        return true;
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */