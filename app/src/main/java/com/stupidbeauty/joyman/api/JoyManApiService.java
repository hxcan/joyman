        Map<String, String> params = session.getParms();
        String include = params.get("include");

        // 🔍 [DEBUG] 第 1 行日志
        logUtils.i(TAG, "🔍 [DEBUG] include=" + include);

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

            // 🔍 [DEBUG] 第 2 行日志
            logUtils.i(TAG, "🔍 [DEBUG] comments count=" + comments.size());

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

        // 🔍 [DEBUG] 第 3 行日志
        logUtils.i(TAG, "🔍 [DEBUG] has journals=" + responseJson.has("journals"));

        return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());