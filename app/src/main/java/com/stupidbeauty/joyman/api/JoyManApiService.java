package com.stupidbeauty.joyman.api;

import android.content.Context;
import android.util.Log;

import com.stupidbeauty.joyman.util.LogUtils;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * JoyMan REST API 服务器
 * 
 * 实现与 Redmine 兼容的 REST API 接口
 * 使得可以使用现有的 Redmine 工具链操作 JoyMan
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-04-05
 */
public class JoyManApiService extends NanoHTTPD {
    
    private static final String TAG = "JoyManApiService";
    private static final int DEFAULT_PORT = 8080;
    
    private Context context;
    private LogUtils logUtils;
    
    /**
     * 构造函数
     * @param context 应用上下文
     */
    public JoyManApiService(Context context) {
        super(DEFAULT_PORT);
        this.context = context;
        this.logUtils = LogUtils.getInstance();
        logUtils.d(TAG, "Constructor: JoyMan API server created on port " + DEFAULT_PORT);
    }
    
    /**
     * 构造函数（自定义端口）
     * @param context 应用上下文
     * @param port 端口号
     */
    public JoyManApiService(Context context, int port) {
        super(port);
        this.context = context;
        this.logUtils = LogUtils.getInstance();
        logUtils.d(TAG, "Constructor: JoyMan API server created on port " + port);
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        String clientIp = session.getRemoteIpAddress();
        
        logUtils.i(TAG, "Request: " + method + " " + uri + " from " + clientIp);
        
        // 添加 CORS 头（方便浏览器调试）
        Map<String, String> headers = session.getHeaders();
        
        // 处理 OPTIONS 预检请求
        if (Method.OPTIONS == method) {
            return createCorsResponse(Response.Status.OK, "text/plain", "");
        }
        
        // 路由分发
        Response response;
        if (uri.startsWith("issues.json")) {
            response = handleIssues(session, method);
        } else if (uri.startsWith("projects.json")) {
            response = handleProjects(session, method);
        } else if (uri.equals("/") || uri.equals("")) {
            response = createCorsResponse(Response.Status.OK, "application/json", 
                "{\"message\":\"JoyMan API Server\",\"version\":\"1.0.0\",\"endpoints\":[\"/issues.json\",\"/projects.json\"]}");
        } else {
            response = createCorsResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
            logUtils.w(TAG, "Unknown endpoint: " + uri);
        }
        
        return response;
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
            case PUT:
            case DELETE:
                // TODO: 实现单任务操作
                return createCorsResponse(Response.Status.NOT_IMPLEMENTED, "text/plain", "Not Implemented");
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
            case PUT:
            case DELETE:
                // TODO: 实现单项目操作
                return createCorsResponse(Response.Status.NOT_IMPLEMENTED, "text/plain", "Not Implemented");
            default:
                return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method Not Allowed");
        }
    }
    
    /**
     * GET /issues.json - 获取任务列表
     */
    private Response getIssues(IHTTPSession session) {
        logUtils.d(TAG, "getIssues: Listing all issues");
        
        // TODO: 实现任务列表查询
        // 需要支持过滤、分页、排序等参数
        
        String jsonResponse = "{\"issues\":[],\"total_count\":0,\"offset\":0,\"limit\":25}";
        return createCorsResponse(Response.Status.OK, "application/json", jsonResponse);
    }
    
    /**
     * POST /issues.json - 创建新任务
     */
    private Response createIssue(IHTTPSession session) {
        logUtils.d(TAG, "createIssue: Creating new issue");
        
        try {
            // 解析请求体
            Map<String, String> files = new java.util.HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");
            
            if (postData == null || postData.isEmpty()) {
                return createCorsResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\":\"No data provided\"}");
            }
            
            logUtils.d(TAG, "createIssue: Received data: " + postData);
            
            // TODO: 解析 JSON 并创建任务
            
            // 临时响应
            String jsonResponse = "{\"issue\":{\"id\":0,\"message\":\"TODO: Implement task creation\"}}";
            return createCorsResponse(Response.Status.CREATED, "application/json", jsonResponse);
            
        } catch (IOException e) {
            logUtils.e(TAG, "createIssue: Error reading request body", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\":\"Internal server error\"}");
        }
    }
    
    /**
     * GET /projects.json - 获取项目列表
     */
    private Response getProjects(IHTTPSession session) {
        logUtils.d(TAG, "getProjects: Listing all projects");
        
        // TODO: 实现项目列表查询
        
        String jsonResponse = "{\"projects\":[],\"total_count\":0}";
        return createCorsResponse(Response.Status.OK, "application/json", jsonResponse);
    }
    
    /**
     * POST /projects.json - 创建新项目
     */
    private Response createProject(IHTTPSession session) {
        logUtils.d(TAG, "createProject: Creating new project");
        
        try {
            // 解析请求体
            Map<String, String> files = new java.util.HashMap<>();
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