    /**
     * 复制任务标题到剪贴板（包含描述）
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
        
        // 获取项目名称，格式：[项目名] 任务标题
        StringBuilder copyContent = new StringBuilder();
        if (task.getProjectId() != null && projectList != null) {
            long targetProjectId = task.getProjectId();
            for (Project p : projectList) {
                if (p != null && p.getId() == targetProjectId) {
                    copyContent.append("[").append(p.getName()).append("] ");
                    break;
                }
            }
        }
        copyContent.append(title);
        
        // 添加描述内容（如果有）
        String description = task.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            copyContent.append("\n\n").append(description);
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            LogUtils.getInstance().e(TAG, "copyTitleToClipboard: ClipboardManager is null");
            Toast.makeText(this, "无法访问剪贴板", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipData clip = ClipData.newPlainText("JoyMan 任务", copyContent.toString());
        clipboard.setPrimaryClip(clip);
        
        LogUtils.getInstance().i(TAG, "copyTitleToClipboard: Content copied successfully: " + copyContent.toString());
        
        String toastMessage = "已复制：" + title;
        if (description != null && !description.trim().isEmpty()) {
            toastMessage += " 和描述";
        }
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }