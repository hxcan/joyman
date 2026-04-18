    private Response getIssue(IHTTPSession session, long issueId)
    {
        logUtils.d(TAG, "getIssue: Getting issue " + issueId);
        Task task = taskRepository.getTaskById(issueId);
        if (task == null)
        {
            logUtils.w(TAG, "getIssue: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Issue not found\"}");
        }

        logUtils.i(TAG, "getIssue: Returned issue " + issueId);
        JsonObject issueJson = ApiJsonConverter.taskToIssueJson(task, null);
        JsonObject responseJson = new JsonObject();
        responseJson.add("issue", issueJson);

        Map<String, String> params = session.getParms();
        String include = params.get("include");
        
        // 🔍 添加详细日志：记录原始 include 参数
        logUtils.i(TAG, "🔍 [INCLUDE_PARAM] 原始 include 参数值：" + include);

        // 支持 children（子任务）
        if ("children".equals(include))
        {
            logUtils.d(TAG, "getIssue: include=children requested, fetching subtasks");
            List<Task> subtasks = taskRepository.getTaskDao().getSubtasksByParentId(issueId);
            if (subtasks == null)
            {
                subtasks = new ArrayList<>();
            }
            JsonArray childrenArray = ApiJsonConverter.tasksToIssuesJson(subtasks, subtasks.size(), 0, subtasks.size()).getAsJsonArray("issues");
            responseJson.add("children", childrenArray);
            logUtils.i(TAG, "getIssue: Included " + subtasks.size() + " children");
        }

        // ✅ 新增：支持 journals（评论列表）
        if ("journals".equals(include))
        {
            logUtils.d(TAG, "getIssue: include=journals requested, fetching comments");
            List<Comment> comments = taskRepository.getTaskDao().getCommentsByIssueId(issueId);
            if (comments == null)
            {
                comments = new ArrayList<>();
            }

            // 按 Redmine 格式返回 journals 数组
            JsonArray journalsArray = new JsonArray();
            for (Comment comment : comments)
            {
                JsonObject journal = new JsonObject();
                journal.addProperty("id", comment.getId());

                // user 字段（Redmine 格式）
                JsonObject user = new JsonObject();
                user.addProperty("id", 1);
                user.addProperty("name", comment.getAuthor() != null ? comment.getAuthor() : "admin");
                journal.add("user", user);

                // notes 字段（评论内容）
                journal.addProperty("notes", comment.getContent() != null ? comment.getContent() : "");

                // created_on 字段
                journal.addProperty("created_on", formatDateTime(comment.getCreatedOn()));

                journalsArray.add(journal);
            }

            responseJson.add("journals", journalsArray);
            logUtils.i(TAG, "getIssue: Included " + comments.size() + " journals/comments");
        }
        
        // 🔍 添加日志：记录最终响应中是否包含 journals
        if (responseJson.has("journals"))
        {
            logUtils.i(TAG, "✅ [RESPONSE] 响应中包含 journals 字段，数量：" + responseJson.getAsJsonArray("journals").size());
        }
        else
        {
            logUtils.w(TAG, "❌ [RESPONSE] 响应中不包含 journals 字段！include 参数值：" + include);
        }

        return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());
    }