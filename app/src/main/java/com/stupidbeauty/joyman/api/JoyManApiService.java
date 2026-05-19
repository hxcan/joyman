    /**
     * 处理 /issues/{id}/relations.json 请求
     */
    private Response handleIssueRelations(IHTTPSession session, Method method, String uri)
    {
        if (!Method.GET.equals(method))
        {
            logUtils.w(TAG, "handleIssueRelations: Method not allowed: " + method);
            return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", "{\"error\":\"Method not allowed\"}");
        }

        // 解析任务 ID
        Matcher matcher = ISSUE_RELATIONS_PATTERN.matcher(uri);
        if (!matcher.matches())
        {
            logUtils.w(TAG, "handleIssueRelations: Invalid URI pattern: " + uri);
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Invalid URI format\"}");
        }

        long issueId = Long.parseLong(matcher.group(1));
        logUtils.d(TAG, "handleIssueRelations: Getting relations for issue " + issueId);

        // 验证任务是否存在
        Task task = taskRepository.getTaskById(issueId);
        if (task == null)
        {
            logUtils.w(TAG, "handleIssueRelations: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Issue not found\"}");
        }

        try
        {
            // ✅ 查询该任务的所有阻塞关系
            List<Relation> relations = taskRepository.getTaskDao().getRelationsByIssueId(issueId);
            
            // 构建 Redmine 格式的 JSON 响应
            JsonArray relationsArray = new JsonArray();
            if (relations != null)
            {
                for (Relation relation : relations)
                {
                    JsonObject relJson = new JsonObject();
                    relJson.addProperty("id", relation.getId());
                    relJson.addProperty("type", relation.getType());
                    relJson.addProperty("issue_id", relation.getRelatedIssueId());
                    relationsArray.add(relJson);
                }
            }
            
            JsonObject response = new JsonObject();
            response.add("relations", relationsArray);

            logUtils.i(TAG, "handleIssueRelations: Returned " + relationsArray.size() + " relations for issue " + issueId);
            return createCorsResponse(Response.Status.OK, "application/json", response.toString());
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "handleIssueRelations: Error querying relations", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 处理 /issues/{id}/relations/{relation_id}.json 请求
     */
    private Response handleIssueRelationDetail(IHTTPSession session, Method method, String uri)
    {
        // 解析任务 ID 和关系 ID
        Matcher matcher = ISSUE_RELATION_DETAIL_PATTERN.matcher(uri);
        if (!matcher.matches())
        {
            logUtils.w(TAG, "handleIssueRelationDetail: Invalid URI pattern: " + uri);
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Invalid URI format\"}");
        }

        long issueId = Long.parseLong(matcher.group(1));
        long relationId = Long.parseLong(matcher.group(2));
        logUtils.d(TAG, "handleIssueRelationDetail: " + method + " relation " + relationId + " for issue " + issueId);

        // 验证任务是否存在
        Task task = taskRepository.getTaskById(issueId);
        if (task == null)
        {
            logUtils.w(TAG, "handleIssueRelationDetail: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Issue not found\"}");
        }

        if (Method.DELETE.equals(method))
        {
            try
            {
                // ✅ 删除指定的关系
                int deletedCount = taskRepository.getTaskDao().deleteRelation(relationId);
                
                if (deletedCount > 0)
                {
                    logUtils.i(TAG, "handleIssueRelationDetail: Deleted relation " + relationId);
                    return createCorsResponse(Response.Status.OK, "application/json", "{\"status\":\"deleted\"}");
                }
                else
                {
                    logUtils.w(TAG, "handleIssueRelationDetail: Relation " + relationId + " not found");
                    return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Relation not found\"}");
                }
            }
            catch (Exception e)
            {
                logUtils.e(TAG, "handleIssueRelationDetail: Error deleting relation", e);
                return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
            }
        }
        else
        {
            logUtils.w(TAG, "handleIssueRelationDetail: Method not allowed: " + method);
            return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", "{\"error\":\"Method not allowed\"}");
        }
    }