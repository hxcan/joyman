    private void copyTitleToClipboard() {
        if (task == null) return;
        String title = task.getTitle();
        if (title == null || title.isEmpty()) return;
        
        // 添加任务编号前缀
        StringBuilder copyContent = new StringBuilder();
        copyContent.append("#").append(task.getId()).append(" ");
        
        if (task.getProjectId() != null && projectList != null) {
            for (Project p : projectList) {
                if (p != null && p.getId() == task.getProjectId()) {
                    copyContent.append("[").append(p.getName()).append("] ");
                    break;
                }
            }
        }
        copyContent.append(title);
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            copyContent.append("\n\n").append(task.getDescription());
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("JoyMan 任务", copyContent.toString()));
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
        }
    }