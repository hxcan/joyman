        // 声明为 final 以在 lambda 表达式中使用，初始化为 null
        final Long projectId = null;
        try {
            String projectIdStr = params.get("project_id");
            if (projectIdStr != null && !projectIdStr.isEmpty()) {
                projectId = Long.parseLong(projectIdStr);
            }
        } catch (NumberFormatException e) {
            logUtils.w(TAG, "getIssues: Invalid project_id: " + params.get("project_id"));
        }
        
        final Integer statusId = null;
        try {
            String statusIdStr = params.get("status_id");
            if (statusIdStr != null && !statusIdStr.isEmpty()) {
                statusId = Integer.parseInt(statusIdStr);
            }
        } catch (NumberFormatException e) {
            logUtils.w(TAG, "getIssues: Invalid status_id: " + params.get("status_id"));
        }