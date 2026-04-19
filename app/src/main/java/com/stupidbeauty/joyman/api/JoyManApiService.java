package com.stupidbeauty.joyman.api;

import android.app.Application;
import android.content.Context;
import android.util.Base64;
import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.data.database.entity.Comment;
import com.stupidbeauty.joyman.repository.ProjectRepository;
import com.stupidbeauty.joyman.repository.TaskRepository;
import com.stupidbeauty.joyman.util.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import fi.iki.elonen.NanoHTTPD;


/**
 * JoyMan REST API 服务器 - 带详细调试日志
 */
public class JoyManApiService extends NanoHTTPD
{
    private static final String TAG = "JoyManApiService";
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final Pattern ISSUE_ID_PATTERN = Pattern.compile("^issues\\/(\\d+)\\.json$");
    private Context context;
    private LogUtils logUtils;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private String adminUsername;
    private String adminPassword;

    public JoyManApiService(Context context)
    {
        super(DEFAULT_PORT);
        init(context);
    }

    public JoyManApiService(Context context, int port)
    {
        super(port);
        init(context);
    }

    private void init(Context context)
    {
        this.context = context;
        this.logUtils = LogUtils.getInstance();
        Application application = (Application) context.getApplicationContext();
        this.taskRepository = TaskRepository.getInstance(application);
        this.projectRepository = ProjectRepository.getInstance(application);
        this.adminUsername = DEFAULT_ADMIN_USERNAME;
        this.adminPassword = DEFAULT_ADMIN_PASSWORD;
        logUtils.i(TAG, "Constructor: JoyMan API server initialized");
        logUtils.w(TAG, "⚠️ WARNING: Using default admin credentials!");
    }

    public void setAdminCredentials(String username, String password)
    {
        this.adminUsername = username;
        this.adminPassword = password;
        logUtils.i(TAG, "setAdminCredentials: Admin username updated to " + username);
    }

    private String normalizeUri(String uri)
    {
        if (uri == null || uri.isEmpty())
        {
            return uri;
        }
        String normalized = uri.startsWith("/") ? uri.substring(1) : uri;
        logUtils.d(TAG, "normalizeUri: \"" + uri + "\" → \"" + normalized + "\"");
        return normalized;
    }

    
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
    }

    private Response updateIssue(IHTTPSession session, long issueId)
    {
        logUtils.d(TAG, "updateIssue: === START Updating issue " + issueId + " ===");

        String postData = null;
        Map<String, String> files = new HashMap<>();
        try
        {
            logUtils.d(TAG, "updateIssue: Calling session.parseBody()...");
            session.parseBody(files);
            postData = files.get("postData");
            if (postData == null || postData.isEmpty())
            {
                postData = files.get("content");
                if (postData != null && !postData.isEmpty())
                {
                    logUtils.d(TAG, "updateIssue: Got data from 'content' key (chunked encoding)");
                }
            }
            else
            {
                logUtils.d(TAG, "updateIssue: Got data from 'postData' key");
            }
            logUtils.d(TAG, "updateIssue: parseBody got postData: " + (postData != null ? (postData.length() > 100 ? postData.substring(0, 100) + "..." : postData) : "null"));
            logUtils.d(TAG, "updateIssue: parseBody files keys: " + (files != null ? files.keySet() : "null"));
        }
        catch (IOException | ResponseException e)
        {
            logUtils.e(TAG, "updateIssue: parseBody failed", e);
        }

        if (postData == null || postData.isEmpty())
        {
            logUtils.d(TAG, "updateIssue: parseBody returned null/empty, trying input stream...");
            postData = readRequestBodyFromStream(session);
            if (postData != null)
            {
                logUtils.d(TAG, "updateIssue: Got data from stream: " + (postData.length() > 100 ? postData.substring(0, 100) + "..." : postData));
            }
            else
            {
                logUtils.e(TAG, "updateIssue: readRequestBodyFromStream also returned null");
            }
        }

        if (postData == null || postData.isEmpty())
        {
            logUtils.e(TAG, "updateIssue: No data provided after all attempts");
            logUtils.d(TAG, "updateIssue: === END (failure: no data) ===");
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"No data provided\"}");
        }

        if (isFilePath(postData))
        {
            logUtils.d(TAG, "updateIssue: Detected file path, reading file content...");
            String fileContent = readFileContent(postData);
            if (fileContent != null)
            {
                postData = fileContent;
                logUtils.d(TAG, "updateIssue: Successfully read file content, length: " + postData.length());
            }
            else
            {
                logUtils.e(TAG, "updateIssue: Failed to read file content");
                logUtils.d(TAG, "updateIssue: === END (failure: can't read file) ===");
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Failed to read request data\"}");
            }
        }

        postData = cleanChunkedData(postData);
        logUtils.d(TAG, "updateIssue: Received data: " + postData);

        Task existingTask = taskRepository.getTaskById(issueId);
        if (existingTask == null)
        {
            logUtils.e(TAG, "updateIssue: Issue not found: " + issueId);
            logUtils.d(TAG, "updateIssue: === END (failure: not found) ===");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Issue not found\"}");
        }

        try
        {
            JsonObject requestJson = JsonParser.parseString(postData).getAsJsonObject();
            if (requestJson.has("issue"))
            {
                JsonObject issueJson = requestJson.getAsJsonObject("issue");

                if (issueJson.has("subject"))
                {
                    existingTask.setTitle(issueJson.get("subject").getAsString());
                    logUtils.d(TAG, "updateIssue: Set subject to: " + existingTask.getTitle());
                }

                if (issueJson.has("description"))
                {
                    existingTask.setDescription(issueJson.get("description").getAsString());
                }

                if (issueJson.has("status_id"))
                {
                    existingTask.setStatus(issueJson.get("status_id").getAsInt());
                    logUtils.d(TAG, "updateIssue: Set status_id to: " + existingTask.getStatus());
                }

                if (issueJson.has("priority"))
                {
                    existingTask.setPriority(issueJson.get("priority").getAsInt());
                }

                if (issueJson.has("priority_id"))
                {
                    existingTask.setPriority(issueJson.get("priority_id").getAsInt());
                    logUtils.d(TAG, "updateIssue: Set priority_id to: " + existingTask.getPriority());
                }

                if (issueJson.has("project_id"))
                {
                    existingTask.setProjectId(issueJson.get("project_id").getAsLong());
                }

                if (issueJson.has("parent_issue_id"))
                {
                    long parentId = issueJson.get("parent_issue_id").getAsLong();
                    if (parentId > 0)
                    {
                        existingTask.setParentId(parentId);
                    }
                    else
                    {
                        existingTask.setParentId(null);
                    }
                    logUtils.d(TAG, "updateIssue: Set parent_issue_id to: " + (existingTask.getParentId() != null ? existingTask.getParentId() : "null (removed)"));
                }

                // ✅ 新增：支持 notes 参数追加评论
                if (issueJson.has("notes"))
                {
                    String notes = issueJson.get("notes").getAsString();
                    if (notes != null && !notes.trim().isEmpty())
                    {
                        logUtils.d(TAG, "updateIssue: Adding notes: " + notes);
                        taskRepository.addNote(issueId, notes);
                        logUtils.i(TAG, "updateIssue: Notes added successfully to issue " + issueId);
                    }
                }
            }

            taskRepository.update(existingTask);
            logUtils.i(TAG, "updateIssue: Updated issue " + issueId);
            logUtils.d(TAG, "updateIssue: === END (success) ===");

            JsonObject responseIssueJson = ApiJsonConverter.taskToIssueJson(existingTask, null);
            JsonObject responseJson = new JsonObject();
            responseJson.add("issue", responseIssueJson);

            return createCorsResponse(Response.Status.OK, "application/json", responseIssueJson.toString());
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "updateIssue: Error processing request", e);
            logUtils.d(TAG, "updateIssue: === END (error) ===");
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Invalid request data: " + e.getMessage() + "\"}");
        }
    }

    private Response deleteIssue(IHTTPSession session, long issueId)
    {
        logUtils.d(TAG, "deleteIssue: Deleting issue " + issueId);
        Task existingTask = taskRepository.getTaskById(issueId);
        if (existingTask == null)
        {
            logUtils.w(TAG, "deleteIssue: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Issue not found\"}");
        }

        taskRepository.deleteById(issueId);
        logUtils.i(TAG, "deleteIssue: Deleted issue " + issueId);
        return createCorsResponse(Response.Status.OK, "application/json", "{}");
    }

    private boolean authenticate(IHTTPSession session)
    {
        Map<String, String> headers = session.getHeaders();
        String authHeader = headers.get("authorization");
        if (authHeader != null && authHeader.startsWith("Basic "))
        {
            return authenticateBasic(authHeader.substring(6));
        }

        Map<String, String> params = session.getParms();
        String username = params.get("username");
        String password = params.get("password");
        if (username != null && password != null)
        {
            logUtils.w(TAG, "authenticate: URL parameters used (not recommended)");
            return validateCredentials(username, password);
        }

        logUtils.w(TAG, "authenticate: No credentials provided");
        return false;
    }

    private boolean authenticateBasic(String base64Credentials)
    {
        try
        {
            byte[] decodedBytes = Base64.decode(base64Credentials, Base64.DEFAULT);
            String credentials = new String(decodedBytes, "UTF-8");
            final int index = credentials.indexOf(':');
            if (index > 0)
            {
                String username = credentials.substring(0, index);
                String password = credentials.substring(index + 1);
                logUtils.d(TAG, "authenticateBasic: User=" + username);
                return validateCredentials(username, password);
            }
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "authenticateBasic: Error decoding credentials", e);
        }
        return false;
    }

    private boolean validateCredentials(String username, String password)
    {
        boolean valid = adminUsername.equals(username) && adminPassword.equals(password);
        if (valid)
        {
            logUtils.d(TAG, "validateCredentials: Success for user " + username);
        }
        else
        {
            logUtils.w(TAG, "validateCredentials: Failed for user " + username);
        }
        return valid;
    }

    private int parseIntSafe(String value, int defaultValue)
    {
        if (value == null || value.isEmpty())
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            logUtils.w(TAG, "parseIntSafe: Invalid value '" + value + "', using default " + defaultValue);
            return defaultValue;
        }
    }

    private Response getIssues(IHTTPSession session)
    {
        logUtils.d(TAG, "getIssues: Listing all issues");
        logUtils.d(TAG, "getIssues: === 参数调试 START ===");
        String fullUri = session.getUri();
        String queryString = "";
        int queryIndex = fullUri.indexOf('?');
        if (queryIndex >= 0)
        {
            queryString = fullUri.substring(queryIndex + 1);
        }
        logUtils.d(TAG, "getIssues: Query String: " + queryString);
        logUtils.d(TAG, "getIssues: URI: " + fullUri);

        Map<String, String> params = session.getParms();
        logUtils.d(TAG, "getIssues: params size: " + (params != null ? params.size() : 0));
        if (params != null)
        {
            for (Map.Entry<String, String> entry : params.entrySet())
            {
                logUtils.d(TAG, "getIssues: params[" + entry.getKey() + "] = " + entry.getValue());
            }
        }
        logUtils.d(TAG, "getIssues: params.get(\"project_id\") = " + (params != null ? params.get("project_id") : "params is null"));
        logUtils.d(TAG, "getIssues: === 参数调试 END ===");

        int limit = parseIntSafe(params.get("limit"), 25);
        int offset = parseIntSafe(params.get("offset"), 0);

        Long projectIdTmp = null;
        try
        {
            String projectIdStr = params.get("project_id");
            if (projectIdStr != null && !projectIdStr.isEmpty())
            {
                projectIdTmp = Long.parseLong(projectIdStr);
            }
        }
        catch (NumberFormatException e)
        {
            logUtils.w(TAG, "getIssues: Invalid project_id: " + params.get("project_id"));
        }
        final Long projectId = projectIdTmp;

        Integer statusIdTmp = null;
        try
        {
            String statusIdStr = params.get("status_id");
            if (statusIdStr != null && !statusIdStr.isEmpty())
            {
                statusIdTmp = Integer.parseInt(statusIdStr);
            }
        }
        catch (NumberFormatException e)
        {
            logUtils.w(TAG, "getIssues: Invalid status_id: " + params.get("status_id"));
        }
        final Integer statusId = statusIdTmp;

        String query = params.get("query");
        String sort = params.get("sort");

        logUtils.d(TAG, "getIssues: Filters - project_id=" + projectId + ", status_id=" + statusId + ", query=" + query + ", limit=" + limit + ", offset=" + offset + ", sort=" + sort);

        List<Task> allTasks = taskRepository.getAllTasks();
        if (allTasks == null)
        {
            allTasks = new ArrayList<>();
        }
        logUtils.d(TAG, "getIssues: Retrieved " + allTasks.size() + " tasks from database (sync query)");

        List<Task> filteredTasks = allTasks.stream()
            .filter(task ->
            {
                if (projectId != null && !projectId.equals(task.getProjectId()))
                {
                    return false;
                }
                if (statusId != null && statusId != task.getStatus())
                {
                    return false;
                }
                if (query != null && !query.isEmpty())
                {
                    String lowerQuery = query.toLowerCase();
                    boolean matchesTitle = task.getTitle().toLowerCase().contains(lowerQuery);
                    boolean matchesDesc = task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerQuery);
                    if (!matchesTitle && !matchesDesc)
                    {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());

        if ("updated_on:desc".equals(sort))
        {
            filteredTasks.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        }
        else if ("updated_on:asc".equals(sort))
        {
            filteredTasks.sort((a, b) -> Long.compare(a.getUpdatedAt(), b.getUpdatedAt()));
        }
        else if ("created_on:desc".equals(sort))
        {
            filteredTasks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        }
        else
        {
            filteredTasks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        }

        int totalCount = filteredTasks.size();
        int fromIndex = Math.min(offset, totalCount);
        int toIndex = Math.min(offset + limit, totalCount);
        List<Task> paginatedTasks = fromIndex < toIndex ? filteredTasks.subList(fromIndex, toIndex) : new ArrayList<>();

        JsonObject responseJson = ApiJsonConverter.tasksToIssuesJson(paginatedTasks, totalCount, offset, limit);
        logUtils.i(TAG, "getIssues: Returned " + paginatedTasks.size() + " of " + totalCount + " issues");
        return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());
    }

    private Response createIssue(IHTTPSession session)
    {
        logUtils.d(TAG, "createIssue: Creating new issue");
        String postData = null;
        Map<String, String> files = new HashMap<>();
        try
        {
            session.parseBody(files);
            postData = files.get("postData");
            if (postData == null || postData.isEmpty())
            {
                postData = files.get("content");
                if (postData != null && !postData.isEmpty())
                {
                    logUtils.d(TAG, "createIssue: Got data from 'content' key (chunked encoding)");
                }
            }
        }
        catch (IOException | ResponseException e)
        {
            logUtils.e(TAG, "createIssue: parseBody failed, trying stream", e);
        }

        if (postData == null || postData.isEmpty())
        {
            postData = readRequestBodyFromStream(session);
        }

        if (postData == null || postData.isEmpty())
        {
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"No data provided\"}");
        }

        if (isFilePath(postData))
        {
            logUtils.d(TAG, "createIssue: Detected file path, reading file content...");
            String fileContent = readFileContent(postData);
            if (fileContent != null)
            {
                postData = fileContent;
                logUtils.d(TAG, "createIssue: Successfully read file content, length: " + postData.length());
            }
            else
            {
                logUtils.e(TAG, "createIssue: Failed to read file content");
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Failed to read request data\"}");
            }
        }

        postData = cleanChunkedData(postData);
        logUtils.d(TAG, "createIssue: Received data: " + postData);

        Task newTask = ApiJsonConverter.parseIssueJson(postData);
        if (newTask == null || newTask.getTitle() == null || newTask.getTitle().isEmpty())
        {
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Invalid issue data: subject is required\"}");
        }

        long taskId = taskRepository.createTask(newTask.getTitle(), newTask.getDescription());
        newTask.setId(taskId);

        if (newTask.getProjectId() != null)
        {
            newTask.setProjectId(newTask.getProjectId());
        }
        if (newTask.getParentId() != null)
        {
            newTask.setParentId(newTask.getParentId());
        }

        taskRepository.update(newTask);
        logUtils.i(TAG, "createIssue: Created task " + taskId + ": " + newTask.getTitle());

        JsonObject issueJson = ApiJsonConverter.taskToIssueJson(newTask, null);
        JsonObject responseJson = new JsonObject();
        responseJson.add("issue", issueJson);

        return createCorsResponse(Response.Status.CREATED, "application/json", responseJson.toString());
    }

    private Response getProjects(IHTTPSession session)
    {
        logUtils.d(TAG, "getProjects: Listing all projects");
        List<Project> projects = projectRepository.getAllProjects();
        if (projects == null)
        {
            projects = new ArrayList<>();
        }
        logUtils.d(TAG, "getProjects: Retrieved " + projects.size() + " projects from database (sync query)");

        JsonObject responseJson = ApiJsonConverter.projectsToJson(projects);
        logUtils.i(TAG, "getProjects: Returned " + projects.size() + " projects");
        return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());
    }

    private Response createProject(IHTTPSession session)
    {
        logUtils.d(TAG, "createProject: Creating new project");
        String postData = null;
        Map<String, String> files = new HashMap<>();
        try
        {
            session.parseBody(files);
            postData = files.get("postData");
            if (postData == null || postData.isEmpty())
            {
                postData = files.get("content");
                if (postData != null && !postData.isEmpty())
                {
                    logUtils.d(TAG, "createProject: Got data from 'content' key (chunked encoding)");
                }
            }
        }
        catch (IOException | ResponseException e)
        {
            logUtils.e(TAG, "createProject: parseBody failed", e);
        }

        if (postData == null || postData.isEmpty())
        {
            postData = readRequestBodyFromStream(session);
        }

        if (postData == null || postData.isEmpty())
        {
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"No data provided\"}");
        }

        if (isFilePath(postData))
        {
            logUtils.d(TAG, "createProject: Detected file path, reading file content...");
            String fileContent = readFileContent(postData);
            if (fileContent != null)
            {
                postData = fileContent;
                logUtils.d(TAG, "createProject: Successfully read file content, length: " + postData.length());
            }
            else
            {
                logUtils.e(TAG, "createProject: Failed to read file content");
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Failed to read request data\"}");
            }
        }

        postData = cleanChunkedData(postData);
        logUtils.d(TAG, "createProject: Received data: " + postData);

        String jsonResponse = "{\"project\":{\"id\":0,\"message\":\"TODO: Implement project creation\"}}";
        return createCorsResponse(Response.Status.CREATED, "application/json", jsonResponse);
    }

    private Response createCorsResponse(Response.Status status, String mimeType, String message)
    {
        Response response = newFixedLengthResponse(status, mimeType, message);
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.addHeader("Access-Control-Max-Age", "86400");
        response.addHeader("Content-Length", String.valueOf(message.getBytes().length));
        response.addHeader("Connection", "close");
        return response;
    }

    public void startService()
    {
        try
        {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            logUtils.i(TAG, "startService: JoyMan API server started successfully");
            logUtils.i(TAG, "Authentication: HTTP Basic Auth (username:password)");
            logUtils.i(TAG, "Default credentials: " + adminUsername + " / " + adminPassword);
        }
        catch (IOException e)
        {
            logUtils.e(TAG, "startService: Failed to start API server", e);
        }
    }

    public void stopService()
    {
        stop();
        logUtils.i(TAG, "stopService: JoyMan API server stopped");
    }
}