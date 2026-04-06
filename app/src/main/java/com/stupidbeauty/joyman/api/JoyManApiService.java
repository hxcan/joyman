package com.stupidbeauty.joyman.api;

import android.app.Application;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.repository.ProjectRepository;
import com.stupidbeauty.joyman.repository.TaskRepository;
import com.stupidbeauty.joyman.util.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import fi.iki.elonen.NanoHTTPD;

/**
 * JoyMan REST API 服务器
 */
public class JoyManApiService extends NanoHTTPD {
    
    private static final String TAG = "JoyManApiService";
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final Pattern ISSUE_ID_PATTERN = Pattern.compile("^issues/(\\d+)\\.json$");
    
    private Context context;
    private LogUtils logUtils;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private String adminUsername;
    private String adminPassword;
    
    public JoyManApiService(Context context) {
        super(DEFAULT_PORT);
        init(context);
    }
    
    public JoyManApiService(Context context, int port) {
        super(port);
        init(context);
    }
    
    private void init(Context context) {
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
    
    public void setAdminCredentials(String username, String password) {
        this.adminUsername = username;
        this.adminPassword = password;
        logUtils.i(TAG, "setAdminCredentials: Admin username updated to " + username);
    }
    
    /**
     * 规范化 URI：去除前导斜杠
     * 例如："/projects.json" → "projects.json"
     */
    private String normalizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        String normalized = uri.startsWith("/") ? uri.substring(1) : uri;
        logUtils.d(TAG, "normalizeUri: \"" + uri + "\" → \"" + normalized + "\"");
        return normalized;
    }
    
    /**
     * 清理 Chunked Encoding 数据
     * NanoHTTPD 的 parseBody() 可能返回包含 Chunked 帧头的数据
     * 例如："a2\r\n{...}\r\n0\n"
     * 需要提取纯 JSON 内容
     */
    private String cleanChunkedData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        // 尝试查找第一个 '{' 或 '[' 的位置（JSON 开始）
        int jsonStart = Math.max(data.indexOf('{'), data.indexOf('['));
        if (jsonStart < 0) {
            logUtils.w(TAG, "cleanChunkedData: No JSON start found in data");
            return data;
        }
        
        // 尝试查找最后一个 '}' 或 ']' 的位置（JSON 结束）
        int jsonEnd = Math.max(data.lastIndexOf('}'), data.lastIndexOf(']'));
        if (jsonEnd < jsonStart) {
            logUtils.w(TAG, "cleanChunkedData: No JSON end found in data");
            return data.substring(jsonStart);
        }
        
        String jsonContent = data.substring(jsonStart, jsonEnd + 1);
        logUtils.d(TAG, "cleanChunkedData: Extracted JSON from chunked data (" + jsonContent.length() + " chars)");
        
        return jsonContent;
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        String clientIp = session.getRemoteIpAddress();
        
        logUtils.i(TAG, "Request: " + method + " " + uri + " from " + clientIp);
        
        if (Method.OPTIONS == method) {
            return createCorsResponse(Response.Status.OK, "text/plain", "");
        }
        
        // 规范化 URI（去除前导斜杠）
        String normalizedUri = normalizeUri(uri);
        
        if (!normalizedUri.equals("/") && !normalizedUri.equals("")) {
            if (!authenticate(session)) {
                logUtils.w(TAG, "Authentication failed for " + normalizedUri);
                return createCorsResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\":\"Invalid or missing credentials\"}");
            }
        }
        
        Response response;
        
        Matcher matcher = ISSUE_ID_PATTERN.matcher(normalizedUri);
        if (matcher.matches()) {
            long issueId = Long.parseLong(matcher.group(1));
            response = handleSingleIssue(session, method, issueId);
        } else if (normalizedUri.startsWith("issues.json")) {
            response = handleIssues(session, method);
        } else if (normalizedUri.startsWith("projects.json")) {
            response = handleProjects(session, method);
        } else if (normalizedUri.equals("/") || normalizedUri.equals("")) {
            response = createCorsResponse(Response.Status.OK, "application/json", 
                "{\"message\":\"JoyMan API Server\",\"version\":\"1.0.7\",\"auth\":\"HTTP Basic Auth\",\"endpoints\":[\"/issues.json\",\"/issues/:id.json\",\"/projects.json\"]}");
        } else {
            response = createCorsResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
            logUtils.w(TAG, "Unknown endpoint: " + normalizedUri);
        }
        
        return response;
    }
    
    private Response handleSingleIssue(IHTTPSession session, Method method, long issueId) {
        switch (method) {
            case GET:
                return getIssue(session, issueId);
            case PUT:
                return updateIssue(session, issueId);
            case DELETE:
                return deleteIssue(session, issueId);
            default:
                return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method Not Allowed");
        }
    }
    
    private Response getIssue(IHTTPSession session, long issueId) {
        logUtils.d(TAG, "getIssue: Getting issue " + issueId);
        
        Task task = taskRepository.getTaskById(issueId);
        
        if (task == null) {
            logUtils.w(TAG, "getIssue: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", 
                "{\"error\":\"Issue not found\"}");
        }
        
        Project project = null;
        if (task.getProjectId() != null) {
            project = null;
        }
        
        JsonObject issueJson = ApiJsonConverter.taskToIssueJson(task, project);
        JsonObject responseJson = new JsonObject();
        responseJson.add("issue", issueJson);
        
        logUtils.i(TAG, "getIssue: Returned issue " + issueId);
        
        return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());
    }
    
    private Response updateIssue(IHTTPSession session, long issueId) {
        logUtils.d(TAG, "updateIssue: Updating issue " + issueId);
        
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            
            if (postData == null || postData.isEmpty()) {
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\":\"No data provided\"}");
            }
            
            // 清理 Chunked Encoding 数据
            postData = cleanChunkedData(postData);
            logUtils.d(TAG, "updateIssue: Received data: " + postData);
            
            Task existingTask = taskRepository.getTaskById(issueId);
            if (existingTask == null) {
                return createCorsResponse(Response.Status.NOT_FOUND, "application/json", 
                    "{\"error\":\"Issue not found\"}");
            }
            
            JsonObject requestJson = com.google.gson.JsonParser.parseString(postData).getAsJsonObject();
            
            if (requestJson.has("issue")) {
                JsonObject issueJson = requestJson.getAsJsonObject("issue");
                
                if (issueJson.has("subject")) {
                    existingTask.setTitle(issueJson.get("subject").getAsString());
                }
                if (issueJson.has("description")) {
                    existingTask.setDescription(issueJson.get("description").getAsString());
                }
                if (issueJson.has("status_id")) {
                    existingTask.setStatus(issueJson.get("status_id").getAsInt());
                }
                if (issueJson.has("priority")) {
                    existingTask.setPriority(issueJson.get("priority").getAsInt());
                }
                if (issueJson.has("project_id")) {
                    existingTask.setProjectId(issueJson.get("project_id").getAsLong());
                }
                if (issueJson.has("parent_issue_id")) {
                    existingTask.setParentId(issueJson.get("parent_issue_id").getAsLong());
                }
            }
            
            taskRepository.update(existingTask);
            
            logUtils.i(TAG, "updateIssue: Updated issue " + issueId);
            
            JsonObject responseIssueJson = ApiJsonConverter.taskToIssueJson(existingTask, null);
            JsonObject responseJson = new JsonObject();
            responseJson.add("issue", responseIssueJson);
            
            return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());
            
        } catch (IOException e) {
            logUtils.e(TAG, "updateIssue: Error reading request body", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\":\"Internal server error\"}");
        } catch (Exception e) {
            logUtils.e(TAG, "updateIssue: Error processing request", e);
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\":\"Invalid request data\"}");
        }
    }
    
    private Response deleteIssue(IHTTPSession session, long issueId) {
        logUtils.d(TAG, "deleteIssue: Deleting issue " + issueId);
        
        Task existingTask = taskRepository.getTaskById(issueId);
        if (existingTask == null) {
            logUtils.w(TAG, "deleteIssue: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", 
                "{\"error\":\"Issue not found\"}");
        }
        
        taskRepository.deleteById(issueId);
        
        logUtils.i(TAG, "deleteIssue: Deleted issue " + issueId);
        
        return createCorsResponse(Response.Status.OK, "application/json", "{}");
    }
    
    private boolean authenticate(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        
        String authHeader = headers.get("authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            return authenticateBasic(authHeader.substring(6));
        }
        
        Map<String, String> params = session.getParms();
        String username = params.get("username");
        String password = params.get("password");
        
        if (username != null && password != null) {
            logUtils.w(TAG, "authenticate: URL parameters used (not recommended for production)");
            return validateCredentials(username, password);
        }
        
        logUtils.w(TAG, "authenticate: No credentials provided");
        return false;
    }
    
    private boolean authenticateBasic(String base64Credentials) {
        try {
            // 使用 android.util.Base64 替代 java.util.Base64（支持 API 8+）
            byte[] decodedBytes = Base64.decode(base64Credentials, Base64.DEFAULT);
            String credentials = new String(decodedBytes, "UTF-8");
            
            final int index = credentials.indexOf(':');
            if (index <= 0) {
                logUtils.w(TAG, "authenticateBasic: Invalid credentials format");
                return false;
            }
            
            String username = credentials.substring(0, index);
            String password = credentials.substring(index + 1);
            
            logUtils.d(TAG, "authenticateBasic: User=" + username);
            
            return validateCredentials(username, password);
            
        } catch (Exception e) {
            logUtils.e(TAG, "authenticateBasic: Error decoding credentials", e);
            return false;
        }
    }
    
    private boolean validateCredentials(String username, String password) {
        boolean isValid = adminUsername.equals(username) && adminPassword.equals(password);
        
        if (isValid) {
            logUtils.d(TAG, "validateCredentials: Success for user " + username);
        } else {
            logUtils.w(TAG, "validateCredentials: Failed for user " + username);
        }
        
        return isValid;
    }
    
    private Response handleIssues(IHTTPSession session, Method method) {
        switch (method) {
            case GET:
                return getIssues(session);
            case POST:
                return createIssue(session);
            default:
                return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method Not Allowed");
        }
    }
    
    private Response handleProjects(IHTTPSession session, Method method) {
        switch (method) {
            case GET:
                return getProjects(session);
            case POST:
                return createProject(session);
            default:
                return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method Not Allowed");
        }
    }
    
    private Response getIssues(IHTTPSession session) {
        logUtils.d(TAG, "getIssues: Listing all issues");
        
        Map<String, String> params = session.getParms();
        String projectIdStr = params.get("project_id");
        String statusIdStr = params.get("status_id");
        String query = params.get("query");
        String limitStr = params.get("limit");
        String offsetStr = params.get("offset");
        String sort = params.get("sort");
        
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 25;
        int offset = offsetStr != null ? Integer.parseInt(offsetStr) : 0;
        Long projectId = projectIdStr != null ? Long.parseLong(projectIdStr) : null;
        Integer statusId = statusIdStr != null ? Integer.parseInt(statusIdStr) : null;
        
        logUtils.d(TAG, "getIssues: Filters - project_id=" + projectId + 
            ", status_id=" + statusId + ", query=" + query + 
            ", limit=" + limit + ", offset=" + offset + ", sort=" + sort);
        
        // 修复：使用同步方法直接查询数据库，避免 LiveData 异步问题
        List<Task> allTasks = taskRepository.getAllTasks();
        if (allTasks == null) {
            allTasks = new ArrayList<>();
        }
        
        logUtils.d(TAG, "getIssues: Retrieved " + allTasks.size() + " tasks from database (sync query)");
        
        List<Task> filteredTasks = allTasks.stream()
            .filter(task -> {
                if (projectId != null && !projectId.equals(task.getProjectId())) {
                    return false;
                }
                if (statusId != null && statusId != task.getStatus()) {
                    return false;
                }
                if (query != null && !query.isEmpty()) {
                    String lowerQuery = query.toLowerCase();
                    boolean matchesTitle = task.getTitle().toLowerCase().contains(lowerQuery);
                    boolean matchesDesc = task.getDescription() != null && 
                        task.getDescription().toLowerCase().contains(lowerQuery);
                    if (!matchesTitle && !matchesDesc) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
        
        if ("updated_on:desc".equals(sort)) {
            filteredTasks.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        } else if ("updated_on:asc".equals(sort)) {
            filteredTasks.sort((a, b) -> Long.compare(a.getUpdatedAt(), b.getUpdatedAt()));
        } else if ("created_on:desc".equals(sort)) {
            filteredTasks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        } else {
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
    
    private Response createIssue(IHTTPSession session) {
        logUtils.d(TAG, "createIssue: Creating new issue");
        
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (IOException | ResponseException e) {
            logUtils.e(TAG, "createIssue: Error parsing request body", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\":\"Failed to parse request body\"}");
        }
        String postData = files.get("postData");
        
        if (postData == null || postData.isEmpty()) {
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\":\"No data provided\"}");
        }
        
        // 清理 Chunked Encoding 数据
        postData = cleanChunkedData(postData);
        logUtils.d(TAG, "createIssue: Received data: " + postData);
        
        Task newTask = ApiJsonConverter.parseIssueJson(postData);
        
        if (newTask == null || newTask.getTitle() == null || newTask.getTitle().isEmpty()) {
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\":\"Invalid issue data: subject is required\"}");
        }
        
        long taskId = taskRepository.createTask(newTask.getTitle(), newTask.getDescription());
        newTask.setId(taskId);
        
        if (newTask.getProjectId() != null) {
            newTask.setProjectId(newTask.getProjectId());
        }
        if (newTask.getParentId() != null) {
            newTask.setParentId(newTask.getParentId());
        }
        
        taskRepository.update(newTask);
        
        logUtils.i(TAG, "createIssue: Created task " + taskId + ": " + newTask.getTitle());
        
        JsonObject issueJson = ApiJsonConverter.taskToIssueJson(newTask, null);
        JsonObject responseJson = new JsonObject();
        responseJson.add("issue", issueJson);
        
        return createCorsResponse(Response.Status.CREATED, "application/json", responseJson.toString());
    }
    
    private Response getProjects(IHTTPSession session) {
        logUtils.d(TAG, "getProjects: Listing all projects");
        
        // 修复：使用同步方法直接查询数据库，避免 LiveData 异步问题
        List<Project> projects = projectRepository.getAllProjects();
        if (projects == null) {
            projects = new ArrayList<>();
        }
        
        logUtils.d(TAG, "getProjects: Retrieved " + projects.size() + " projects from database (sync query)");
        
        JsonObject responseJson = ApiJsonConverter.projectsToJson(projects);
        
        logUtils.i(TAG, "getProjects: Returned " + projects.size() + " projects");
        
        return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());
    }
    
    private Response createProject(IHTTPSession session) {
        logUtils.d(TAG, "createProject: Creating new project");
        
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (IOException | ResponseException e) {
            logUtils.e(TAG, "createProject: Error parsing request body", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\":\"Failed to parse request body\"}");
        }
        String postData = files.get("postData");
        
        if (postData == null || postData.isEmpty()) {
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\":\"No data provided\"}");
        }
        
        // 清理 Chunked Encoding 数据
        postData = cleanChunkedData(postData);
        logUtils.d(TAG, "createProject: Received data: " + postData);
        
        String jsonResponse = "{\"project\":{\"id\":0,\"message\":\"TODO: Implement project creation\"}}";
        return createCorsResponse(Response.Status.CREATED, "application/json", jsonResponse);
    }
    
    private Response createCorsResponse(Response.Status status, String mimeType, String message) {
        Response response = newFixedLengthResponse(status, mimeType, message);
        
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.addHeader("Access-Control-Max-Age", "86400");
        
        return response;
    }
    
    public void startService() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            logUtils.i(TAG, "startService: JoyMan API server started successfully");
            logUtils.i(TAG, "Authentication: HTTP Basic Auth (username:password)");
            logUtils.i(TAG, "Default credentials: " + adminUsername + " / " + adminPassword);
        } catch (IOException e) {
            logUtils.e(TAG, "startService: Failed to start API server", e);
        }
    }
    
    public void stopService() {
        stop();
        logUtils.i(TAG, "stopService: JoyMan API server stopped");
    }
}