        // 使用普通变量而非 final 变量，以便在 try-catch 中赋值
        Long projectId = null;
        try {
            String projectIdStr = params.get("project_id");
            if (projectIdStr != null && !projectIdStr.isEmpty()) {
                projectId = Long.parseLong(projectIdStr);
            }
        } catch (NumberFormatException e) {
            logUtils.w(TAG, "getIssues: Invalid project_id: " + params.get("project_id"));
        }
        
        Integer statusId = null;
        try {
            String statusIdStr = params.get("status_id");
            if (statusIdStr != null && !statusIdStr.isEmpty()) {
                statusId = Integer.parseInt(statusIdStr);
            }
        } catch (NumberFormatException e) {
            logUtils.w(TAG, "getIssues: Invalid status_id: " + params.get("status_id"));
        }