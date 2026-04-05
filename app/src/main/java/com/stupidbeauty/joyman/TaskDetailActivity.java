    /**
     * 复制任务标题到剪贴板（包含项目名称）
     */
    private void copyTitleToClipboard() {
        if (task == null) {
            LogUtils.getInstance().w(TAG, "copyTitleToClipboard: Task is null");
            Toast.makeText(this, "任务数据未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String title = task.getTitle();
        if (title == null || title.isEmpty()) {
            LogUtils.getInstance().w(TAG, "copyTitleToClipboard: Task title is empty");
            Toast.makeText(this, "任务标题为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 构建复制内容：[项目名] 任务标题
        String copyContent = title;
        if (task.getProjectId() != null && projectList != null) {
            for (Project p : projectList) {
                if (p != null && p.getId().equals(task.getProjectId())) {
                    copyContent = "[" + p.getName() + "] " + title;
                    break;
                }
            }
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            LogUtils.getInstance().e(TAG, "copyTitleToClipboard: ClipboardManager is null");
            Toast.makeText(this, "无法访问剪贴板", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipData clip = ClipData.newPlainText("JoyMan 任务标题", copyContent);
        clipboard.setPrimaryClip(clip);
        
        LogUtils.getInstance().i(TAG, "copyTitleToClipboard: Title copied successfully: " + copyContent);
        
        String toastMessage = "已复制：" + copyContent;
        if (toastMessage.length() > 50) {
            toastMessage = toastMessage.substring(0, 47) + "...";
        }
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }