package com.stupidbeauty.joyman.api;

import android.content.Context;
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
 * 
 * 实现与 Redmine 兼容的 REST API 接口
 * 使用 HTTP Basic Auth 认证（用户名 + 密码）
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.4
 * @since 2026-04-05
 */
public class JoyManApiService extends NanoHTTPD {
    
    private static final String TAG = "JoyManApiService";
    private static final int DEFAULT_PORT = 8080;
    
    // 默认管理员账号
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    
    // URL 路径正则表达式
    private static final Pattern ISSUE_ID_PATTERN = Pattern.compile("^issues/(\\d+)\\.json$");
    
    private Context context;
    private LogUtils logUtils;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    
    // 管理员账号
    private String adminUsername;
    private String adminPassword;
    
    /**
     * 构造函数
     */
    public JoyManApiService(Context context) {
        super(DEFAULT_PORT);
        init(context);
    }
    
    /**
     * 构造函数（自定义端口）
     */
    public JoyManApiService(Context context, int port) {
        super(port);
        init(context);
    }
    
    /**
     * 初始化
     */
    private void init(Context context) {
        this.context = context;
        this.logUtils = LogUtils.getInstance();
        this.taskRepository = TaskRepository.getInstance(context);
        this.projectRepository = ProjectRepository.getInstance(context);
        
        this.adminUsername = DEFAULT_ADMIN_USERNAME;
        this.adminPassword = DEFAULT_ADMIN_PASSWORD;
        
        logUtils.i(TAG, "Constructor: JoyMan API server initialized");
        logUtils.i(TAG, "Admin username: " + adminUsername);
        logUtils.w(TAG, "⚠️ WARNING: Using default admin credentials. Change in production!");
    }
    
    /**
     * 设置管理员账号
     */
    public void setAdminCredentials(String username, String password) {
        this.adminUsername = username;
        this.adminPassword = password;
        logUtils.i(TAG, "setAdminCredentials: Admin username updated to " + username);
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        String clientIp = session.getRemoteIpAddress();
        
        logUtils.i(TAG, "Request: " + method + " " + uri + " from " + clientIp);
        
        // 处理 OPTIONS 预检请求
        if (Method.OPTIONS == method) {
            return createCorsResponse(Response.Status.OK, "text/plain", "");
        }
        
        // 跳过根路径的认证（用于健康检查）
        if (!uri.equals("/") && !uri.equals("")) {
            if (!authenticate(session)) {
                logUtils.w(TAG, "Authentication failed for " + uri);
                return createCorsResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\":\"Invalid or missing credentials\"}");
            }
        }
        
        // 路由分发
        Response response;
        
        // 检查是否是单个任务的路径（/issues/:id.json）
        Matcher matcher = ISSUE_ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            long issueId = Long.parseLong(matcher.group(1));
            response = handleSingleIssue(session, method, issueId);
        } else if (uri.startsWith("issues.json")) {
            response = handleIssues(session, method);
        } else if (uri.startsWith("projects.json")) {
            response = handleProjects(session, method);
        } else if (uri.equals("/") || uri.equals("")) {
            response = createCorsResponse(Response.Status.OK, "application/json", 
                "{\"message\":\"JoyMan API Server\",\"version\":\"1.0.4\",\"auth\":\"HTTP Basic Auth\",\"endpoints\":[\"/issues.json\",\"/issues/:id.json\",\"/projects.json\"]}");
        } else {
            response = createCorsResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
            logUtils.w(TAG, "Unknown endpoint: " + uri);
        }
        
        return response;
    }
    
    /**
     * 处理单个任务相关请求
     */
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
    
    /**
     * GET /issues/:id.json - 获取单个任务详情
     */
    private Response getIssue(IHTTPSession session, long issueId) {
        logUtils.d(TAG, "getIssue: Getting issue " + issueId);
        
        // 查询任务
        Task task = taskRepository.getTaskById(issueId);
        
        if (task == null) {
            logUtils.w(TAG, "getIssue: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", 
                "{\"error\":\"Issue not found\"}");
        }
        
        // 获取项目信息（如果有）
        Project project = null;
        if (task.getProjectId() != null) {
            // 简化处理，实际应该查询项目
            project = null;
        }
        
        // 转换为 JSON
        JsonObject issueJson = ApiJsonConverter.taskToIssueJson(task, project);
        JsonObject responseJson = new JsonObject();
        responseJson.add("issue", issueJson);
        
        logUtils.i(TAG, "getIssue: Returned issue " + issueId);
        
        return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());
    }
    
    /**
     * PUT /issues/:id.json - 更新任务
     */
    private Response updateIssue(IHTTPSession session, long issueId) {
        logUtils.d(TAG, "updateIssue: Updating issue " + issueId);
        
        try {
            // 解析请求体
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            
            if (postData == null || postData.isEmpty()) {
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\":\"No data provided\"}");
            }
            
            logUtils.d(TAG, "updateIssue: Received data: " + postData);
            
            // 查询现有任务
            Task existingTask = taskRepository.getTaskById(issueId);
            if (existingTask == null) {
                return createCorsResponse(Response.Status.NOT_FOUND, "application/json", 
                    "{\"error\":\"Issue not found\"}");
            }
            
            // 解析 JSON 并更新任务
            JsonObject requestJson = com.google.gson.JsonParser.parseString(postData).getAsJsonObject();
            
            if (requestJson.has("issue")) {
                JsonObject issueJson = requestJson.getAsJsonObject("issue");
                
                // 更新标题
                if (issueJson.has("subject")) {
                    existingTask.setTitle(issueJson.get("subject").getAsString());
                }
                
                // 更新描述
                if (issueJson.has("description")) {
                    existingTask.setDescription(issueJson.get("description").getAsString());
                }
                
                // 更新状态
                if (issueJson.has("status_id")) {
                    existingTask.setStatus(issueJson.get("status_id").getAsInt());
                }
                
                // 更新优先级
                if (issueJson.has("priority")) {
                    existingTask.setPriority(issueJson.get("priority").getAsInt());
                }
                
                // 更新项目
                if (issueJson.has("project_id")) {
                    existingTask.setProjectId(issueJson.get("project_id").getAsLong());
                }
                
                // 更新父任务
                if (issueJson.has("parent_issue_id")) {
                    existingTask.setParentId(issueJson.get("parent_issue_id").getAsLong());
                }
            }
            
            // 保存更新
            taskRepository.update(existingTask);
            
            logUtils.i(TAG, "updateIssue: Updated issue " + issueId);
            
            // 返回更新后的任务
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
    
    /**
     * DELETE /issues/:id.json - 删除任务
     */
    private Response deleteIssue(IHTTPSession session, long issueId) {
        logUtils.d(TAG, "deleteIssue: Deleting issue " + issueId);
        
        // 查询现有任务
        Task existingTask = taskRepository.getTaskById(issueId);
        if (existingTask == null) {
            logUtils.w(TAG, "deleteIssue: Issue " + issueId + " not found");
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", 
                "{\"error\":\"Issue not found\"}");
        }
        
        // 删除任务
        taskRepository.deleteById(issueId);
        
        logUtils.i(TAG, "deleteIssue: Deleted issue " + issueId);
        
        // 返回成功响应（Redmine 风格：返回空内容）
        return createCorsResponse(Response.Status.OK, "application/json", "{}");
    }
    
    /**
     * 认证中间件 - HTTP Basic Auth
     */
    private boolean authenticate(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        
        // 方式 1: HTTP Basic Auth Header
        String authHeader = headers.get("authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            return authenticateBasic(authHeader.substring(6));
        }
        
        // 方式 2: URL 参数（仅用于调试）
        Map<String, String> params = new HashMap<>();
        try {
            session.parseUri(params);
            String username = params.get("username");
            String password = params.get("password");
            
            if (username != null && password != null) {
                logUtils.w(TAG, "authenticate: URL parameters used (not recommended for production)");
                return validateCredentials(username, password);
            }
        } catch (IOException e) {
            logUtils.e(TAG, "authenticate: Error parsing URI", e);
        }
        
        logUtils.w(TAG, "authenticate: No credentials provided");
        return false;
    }
    
    /**
     * HTTP Basic Auth 认证
     */
    private boolean authenticateBasic(String base64Credentials) {
        try {
            String credentials = new String(
                java.util.Base64.getDecoder().decode(base64Credentials), 
                "UTF-8"
            );
            
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
    
    /**
     * 验证用户名和密码
     */
    private boolean validateCredentials(String username, String password) {
        boolean isValid = adminUsername.equals(username) && adminPassword.equals(password);
        
        if (isValid) {
            logUtils.d(TAG, "validateCredentials: Success for user " + username);
        } else {
            logUtils.w(TAG, "validateCredentials: Failed for user " + username);
        }
        
        return isValid;
    }
    
    /**
     * 处理任务列表相关请求
     */
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
    
    /**
     * 处理项目相关请求
     */
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
    
    /**
     * GET /issues.json - 获取任务列表
     */
    private Response getIssues(IHTTPSession session) {
        logUtils.d(TAG, "getIssues: Listing all issues");
        
        Map<String, String> params = new HashMap<>();
        try {
            session.parseUri(params);
        } catch (IOException e) {
            logUtils.e(TAG, "getIssues: Error parsing query parameters", e);
        }
        
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
        
        List<Task> allTasks = taskRepository.getAllTasksLive().getValue();
        if (allTasks == null) {
            allTasks = new ArrayList<>();
        }
        
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
    
    /**
     * POST /issues.json - 创建新任务
     */
    private Response createIssue(IHTTPSession session) {
        logUtils.d(TAG, "createIssue: Creating new issue");
        
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            
            if (postData == null || postData.isEmpty()) {
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\":\"No data provided\"}");
            }
            
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
            
        } catch (IOException e) {
            logUtils.e(TAG, "createIssue: Error reading request body", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\":\"Internal server error\"}");
        } catch (NumberFormatException e) {
            logUtils.e(TAG, "createIssue: Invalid parameter format", e);
            return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\":\"Invalid parameter format\"}");
        }
    }
    
    /**
     * GET /projects.json - 获取项目列表
     */
    private Response getProjects(IHTTPSession session) {
        logUtils.d(TAG, "getProjects: Listing all projects");
        
        List<Project> projects = projectRepository.getAllProjectsLive().getValue();
        if (projects == null) {
            projects = new ArrayList<>();
        }
        
        JsonObject responseJson = ApiJsonConverter.projectsToJson(projects);
        
        logUtils.i(TAG, "getProjects: Returned " + projects.size() + " projects");
        
        return createCorsResponse(Response.Status.OK, "application/json", responseJson.toString());
    }
    
    /**
     * POST /projects.json - 创建新项目
     */
    private Response createProject(IHTTPSession session) {
        logUtils.d(TAG, "createProject: Creating new project");
        
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            
            if (postData == null || postData.isEmpty()) {
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\":\"No data provided\"}");
            }
            
            logUtils.d(TAG, "createProject: Received data: " + postData);
            
            String jsonResponse = "{\"project\":{\"id\":0,\"message\":\"TODO: Implement project creation\"}}";
            return createCorsResponse(Response.Status.CREATED, "application/json", jsonResponse);
            
        } catch (IOException e) {
            logUtils.e(TAG, "createProject: Error reading request body", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\":\"Internal server error\"}");
        }
    }
    
    /**
     * 创建带 CORS 头的响应
     */
    private Response createCorsResponse(Response.Status status, String mimeType, String message) {
        Response response = newFixedLengthResponse(status, mimeType, message);
        
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.addHeader("Access-Control-Max-Age", "86400");
        
        return response;
    }
    
    /**
     * 启动 API 服务
     */
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
    
    /**
     * 停止 API 服务
     */
    public void stopService() {
        stop();
        logUtils.i(TAG, "stopService: JoyMan API server stopped");
    }
}