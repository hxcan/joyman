        // 声明为 final 以在 lambda 表达式中使用
        final Long projectId;
        try {
            String projectIdStr = params.get("project_id");
            projectId = (projectIdStr != null && !projectIdStr.isEmpty()) ? Long.parseLong(projectIdStr) : null;
        } catch (NumberFormatException e) {
            logUtils.w(TAG, "getIssues: Invalid project_id: " + params.get("project_id"));
        }
        
        final Integer statusId;
        try {
            String statusIdStr = params.get("status_id");
            statusId = (statusIdStr != null && !statusIdStr.isEmpty()) ? Integer.parseInt(statusIdStr) : null;
        } catch (NumberFormatException e) {
            logUtils.w(TAG, "getIssues: Invalid status_id: " + params.get("status_id"));
        }