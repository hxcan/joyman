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
 * 使得可以使用现有的 Redmine 工具链操作 JoyMan
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.2
 * @since 2026-04-05
 */
public class JoyManApiService extends NanoHTTPD {
    
    private static final String TAG = "JoyManApiService";
    private static final int DEFAULT_PORT = 8080;
    
    private Context context;
    private LogUtils logUtils;
    private ApiManager apiManager;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    
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
        this.apiManager = ApiManager.getInstance(context);
        this.taskRepository = TaskRepository.getInstance(context);
        this.projectRepository = ProjectRepository.getInstance(context);
        logUtils.d(TAG, "Constructor: JoyMan API server initialized");
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
                    "{\"error\":\"Invalid or missing API key\"}");
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
                "{\"message\":\"JoyMan API Server\",\"version\":\"1.0.2\",\"endpoints\":[\"/issues.json\",\"/projects.json\"]}");
        } else {
            response = createCorsResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
            logUtils.w(TAG, "Unknown endpoint: " + uri);
        }
        
        return response;
    }
    
    /**
     * 认证中间件
     */
    private boolean authenticate(IHTTPSession session) {
        // 从 Header 获取 API Key
        Map<String, String> headers = session.getHeaders();
        String apiKey = headers.get("x-redmine-api-key");
        
        // 如果 Header 中没有，尝试从 URL 参数获取
        if (apiKey == null || apiKey.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            try {
                session.parseUri(params);
                apiKey = params.get("key");
            } catch (IOException e) {
                logUtils.e(TAG, "authenticate: Error parsing URI", e);
            }
        }
        
        // 验证 API Key
        if (apiKey == null || apiKey.isEmpty()) {
            logUtils.w(TAG, "authenticate: No API key provided");
            return false;
        }
        
        boolean isValid = apiManager.validateApiKey(apiKey);
        
        if (isValid) {
            logUtils.d(TAG, "authenticate: Success (key: " + apiKey.substring(0, 8) + "...)");
        } else {
            logUtils.w(TAG, "authenticate: Invalid API key provided");
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
     * 支持过滤、分页、排序
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
        
        // 排序（简化实现）
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
            
            // 获取完整的项目信息
            Project project = null;
            if (newTask.getProjectId() != null) {
                // 简化处理，实际应该查询项目
                project = null;
            }
            
            // 返回创建的任务
            JsonObject issueJson = ApiJsonConverter.taskToIssueJson(newTask, project);
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
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Redmine-API-Key, Authorization");
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