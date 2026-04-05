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
import java.util.stream.Collectors;

import fi.iki.elonen.NanoHTTPD;

/**
 * JoyMan REST API 服务器
 * 
 * 实现与 Redmine 兼容的 REST API 接口
 * 使用 HTTP Basic Auth 认证（用户名 + 密码）
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.3
 * @since 2026-04-05
 */
public class JoyManApiService extends NanoHTTPD {
    
    private static final String TAG = "JoyManApiService";
    private static final int DEFAULT_PORT = 8080;
    
    // 默认管理员账号（可配置）
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    
    private Context context;
    private LogUtils logUtils;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    
    // 管理员账号（可从配置读取）
    private String adminUsername;
    private String adminPassword;
    
    /**
     * 构造函数
     * @param context 应用上下文
     */
    public JoyManApiService(Context context) {
        super(DEFAULT_PORT);
        init(context);
    }
    
    /**
     * 构造函数（自定义端口）
     * @param context 应用上下文
     * @param port 端口号
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
        
        // 读取管理员账号（简化实现，后续可从 SharedPreferences 读取）
        this.adminUsername = DEFAULT_ADMIN_USERNAME;
        this.adminPassword = DEFAULT_ADMIN_PASSWORD;
        
        logUtils.i(TAG, "Constructor: JoyMan API server initialized");
        logUtils.i(TAG, "Admin username: " + adminUsername);
        logUtils.i(TAG, "⚠️ WARNING: Using default admin credentials. Change in production!");
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
            // 认证检查
            if (!authenticate(session)) {
                logUtils.w(TAG, "Authentication failed for " + uri);
                return createCorsResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\":\"Invalid or missing credentials\"}");
            }
        }
        
        // 路由分发
        Response response;
        if (uri.startsWith("issues.json")) {
            response = handleIssues(session, method);
        } else if (uri.startsWith("projects.json")) {
            response = handleProjects(session, method);
        } else if (uri.equals("/") || uri.equals("")) {
            response = createCorsResponse(Response.Status.OK, "application/json", 
                "{\"message\":\"JoyMan API Server\",\"version\":\"1.0.3\",\"auth\":\"HTTP Basic Auth\",\"endpoints\":[\"/issues.json\",\"/projects.json\"]}");
        } else {
            response = createCorsResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
            logUtils.w(TAG, "Unknown endpoint: " + uri);
        }
        
        return response;
    }
    
    /**
     * 认证中间件 - HTTP Basic Auth
     * 支持两种认证方式：
     * 1. HTTP Basic Auth (Authorization: Basic base64(username:password))
     * 2. URL 参数 (username=xxx&password=yyy) - 仅用于测试
     */
    private boolean authenticate(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        
        // 方式 1: HTTP Basic Auth Header
        String authHeader = headers.get("authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            return authenticateBasic(authHeader.substring(6));
        }
        
        // 方式 2: URL 参数（仅用于调试，不推荐生产环境使用）
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
     * @param base64Credentials Base64 编码的 "username:password"
     */
    private boolean authenticateBasic(String base64Credentials) {
        try {
            // 解码 Base64
            String credentials = new String(
                java.util.Base64.getDecoder().decode(base64Credentials), 
                "UTF-8"
            );
            
            // 分离用户名和密码
            final int index = credentials.indexOf(':');
            if (index <= 0) {
                logUtils.w(TAG, "authenticateBasic: Invalid credentials format");
                return false;
            }
            
            String username = credentials.substring(0, index);
            String password = credentials.substring(index + 1);
            
            logUtils.d(TAG, "authenticateBasic: User=" + username);
            
            // 验证用户名和密码
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
     * 处理任务相关请求
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
        
        // 解析查询参数
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
        
        // 获取所有任务
        List<Task> allTasks = taskRepository.getAllTasksLive().getValue();
        if (allTasks == null) {
            allTasks = new ArrayList<>();
        }
        
        // 应用过滤器
        List<Task> filteredTasks = allTasks.stream()
            .filter(task -> {
                // 项目过滤
                if (projectId != null && !projectId.equals(task.getProjectId())) {
                    return false;
                }
                // 状态过滤
                if (statusId != null && statusId != task.getStatus()) {
                    return false;
                }
                // 关键词搜索
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
        
        // 排序
        if ("updated_on:desc".equals(sort)) {
            filteredTasks.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        } else if ("updated_on:asc".equals(sort)) {
            filteredTasks.sort((a, b) -> Long.compare(a.getUpdatedAt(), b.getUpdatedAt()));
        } else if ("created_on:desc".equals(sort)) {
            filteredTasks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        } else {
            // 默认按创建时间降序
            filteredTasks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        }
        
        // 分页
        int totalCount = filteredTasks.size();
        int fromIndex = Math.min(offset, totalCount);
        int toIndex = Math.min(offset + limit, totalCount);
        List<Task> paginatedTasks = fromIndex < toIndex ? filteredTasks.subList(fromIndex, toIndex) : new ArrayList<>();
        
        // 转换为 JSON
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
            // 解析请求体
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            
            if (postData == null || postData.isEmpty()) {
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\":\"No data provided\"}");
            }
            
            logUtils.d(TAG, "createIssue: Received data: " + postData);
            
            // 解析 JSON 并创建任务
            Task newTask = ApiJsonConverter.parseIssueJson(postData);
            
            if (newTask == null || newTask.getTitle() == null || newTask.getTitle().isEmpty()) {
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\":\"Invalid issue data: subject is required\"}");
            }
            
            // 生成 ID 并保存
            long taskId = taskRepository.createTask(newTask.getTitle(), newTask.getDescription());
            newTask.setId(taskId);
            
            // 更新其他字段
            if (newTask.getProjectId() != null) {
                newTask.setProjectId(newTask.getProjectId());
            }
            if (newTask.getParentId() != null) {
                newTask.setParentId(newTask.getParentId());
            }
            
            taskRepository.update(newTask);
            
            logUtils.i(TAG, "createIssue: Created task " + taskId + ": " + newTask.getTitle());
            
            // 返回创建的任务
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
        
        // 获取所有项目
        List<Project> projects = projectRepository.getAllProjectsLive().getValue();
        if (projects == null) {
            projects = new ArrayList<>();
        }
        
        // 转换为 JSON
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
            // 解析请求体
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            
            if (postData == null || postData.isEmpty()) {
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\":\"No data provided\"}");
            }
            
            logUtils.d(TAG, "createProject: Received data: " + postData);
            
            // TODO: 解析 JSON 并创建项目
            
            // 临时响应
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
        
        // CORS 头
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Redmine-API-Key");
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