    /**
     * 处理 POST /issues/{id}/relations.json 请求 - 创建新关系
     */
    private Response createRelation(IHTTPSession session, String uri) throws IOException, org.json.JSONException
    {
        // 解析任务 ID
        Matcher matcher = ISSUE_RELATIONS_PATTERN.matcher(uri);
        if (!matcher.matches())
        {
            logUtils.w(TAG, "createRelation: Invalid URI pattern: " + uri);
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Invalid URI format\"}");
        }

        long issueId = Long.parseLong(matcher.group(1));
        logUtils.d(TAG, "createRelation: Creating relation for issue " + issueId);

        // 1. 验证主任务是否存在
        Task task = taskRepository.getTaskById(issueId);
        if (task == null)
        {
            logUtils.w(TAG, "createRelation: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Issue not found\"}");
        }

        // 2. 解析请求体
        Map<String, String> files = new HashMap<>();
        try
        {
            session.parseBody(files);
        }
        catch (IOException e)
        {
            logUtils.e(TAG, "createRelation: Failed to parse request body", e);
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Failed to parse request body\"}");
        }
        catch (fi.iki.elonen.NanoHTTPD.ResponseException e)
        {
            logUtils.e(TAG, "createRelation: Response exception while parsing body", e);
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Failed to parse request body\"}");
        }

        String postData = files.get("postData");
        if (postData == null || postData.isEmpty())
        {
            logUtils.w(TAG, "createRelation: Missing request body");
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Missing request body\"}");
        }

        logUtils.d(TAG, "createRelation: Request body: " + postData);

        // 3. 解析 relation 对象
        org.json.JSONObject json;
        try
        {
            json = new org.json.JSONObject(postData);
        }
        catch (org.json.JSONException e)
        {
            logUtils.e(TAG, "createRelation: Invalid JSON format", e);
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Invalid JSON format\"}");
        }

        if (!json.has("relation"))
        {
            logUtils.w(TAG, "createRelation: Missing 'relation' object");
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Missing 'relation' object\"}");
        }

        org.json.JSONObject relationJson = json.getJSONObject("relation");
        long issueToId;
        try
        {
            issueToId = relationJson.getLong("issue_to_id");
        }
        catch (org.json.JSONException e)
        {
            logUtils.w(TAG, "createRelation: Missing 'issue_to_id' field");
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Missing 'issue_to_id' field\"}");
        }

        String relationType = relationJson.optString("relation_type", "blocks");

        // 4. 验证关联任务是否存在
        Task relatedTask = taskRepository.getTaskById(issueToId);
        if (relatedTask == null)
        {
            logUtils.w(TAG, "createRelation: Related issue " + issueToId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Related issue not found: " + issueToId + "\"}");
        }

        // 5. 创建关系
        try
        {
            com.stupidbeauty.joyman.data.database.entity.Relation relation = new com.stupidbeauty.joyman.data.database.entity.Relation(issueId, issueToId, relationType);
            long relationId = taskRepository.getRelationDao().insert(relation);

            logUtils.i(TAG, "✅ 创建关系成功：" + relationId + " (" + issueId + " " + relationType + " " + issueToId + ")");

            // 6. 返回创建的 Relation 对象
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("id", relationId);
            responseJson.addProperty("issue_id", issueId);
            responseJson.addProperty("issue_to_id", issueToId);
            responseJson.addProperty("type", relationType);
            responseJson.addProperty("created_at", relation.createdAt);

            return createCorsResponse(Response.Status.CREATED, "application/json", responseJson.toString());
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ 创建关系失败", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Failed to create relation: " + e.getMessage() + "\"}");
        }
    }