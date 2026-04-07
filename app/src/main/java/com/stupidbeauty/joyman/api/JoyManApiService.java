        // 使用 final 变量以便在 lambda 中使用
        final Long projectId = null;
        try {
            String projectIdStr = params.get("project_id");
            if (projectIdStr != null && !projectIdStr.isEmpty()) {
                // 不能重新赋值 final 变量，需要使用其他方法
            }
        } catch (NumberFormatException e) {
            logUtils.w(TAG, "getIssues: Invalid project_id: " + params.get("project_id"));
        }
        
        final Integer statusId = null;
        try {
            String statusIdStr = params.get("status_id");
            if (statusIdStr != null && !statusIdStr.isEmpty()) {
                // 不能重新赋值 final 变量，需要使用其他方法
            }
        } catch (NumberFormatException e) {
            logUtils.w(TAG, "getIssues: Invalid status_id: " + params.get("status_id"));
        }