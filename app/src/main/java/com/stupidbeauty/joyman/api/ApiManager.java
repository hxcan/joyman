package com.stupidbeauty.joyman.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.stupidbeauty.joyman.util.LogUtils;

/**
 * API 服务管理器
 * 
 * 管理 JoyMan REST API 服务的启动、停止和配置
 * 简化版本：仅保留端口配置和服务管理
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.1
 * @since 2026-04-05
 */
public class ApiManager {
    
    private static final String TAG = "ApiManager";
    private static final String PREF_NAME = "joyman_api_settings";
    private static final String KEY_API_ENABLED = "api_enabled";
    private static final String KEY_API_PORT = "api_port";
    
    private static final int DEFAULT_PORT = 8080;
    
    private Context context;
    private LogUtils logUtils;
    private SharedPreferences prefs;
    private JoyManApiService apiService;
    
    private static ApiManager instance;
    
    /**
     * 私有构造函数（单例模式）
     */
    private ApiManager(Context context) {
        this.context = context.getApplicationContext();
        this.logUtils = LogUtils.getInstance();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        logUtils.d(TAG, "Constructor: ApiManager initialized");
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized ApiManager getInstance(Context context) {
        if (instance == null) {
            instance = new ApiManager(context);
        }
        return instance;
    }
    
    /**
     * 启动 API 服务（如果已启用）
     */
    public void startIfNeeded() {
        if (isApiEnabled()) {
            startApiService();
        } else {
            logUtils.i(TAG, "startIfNeeded: API is disabled, not starting service");
        }
    }
    
    /**
     * 启动 API 服务
     */
    public void startApiService() {
        if (apiService != null) {
            logUtils.w(TAG, "startApiService: Service already running");
            return;
        }
        
        int port = getApiPort();
        apiService = new JoyManApiService(context, port);
        apiService.startService();
        
        logUtils.i(TAG, "startApiService: API server started on port " + port);
        logUtils.i(TAG, "Authentication: HTTP Basic Auth (username/password)");
        logUtils.i(TAG, "Default credentials: admin / admin");
        logUtils.w(TAG, "⚠️ WARNING: Change default credentials in production!");
    }
    
    /**
     * 停止 API 服务
     */
    public void stopApiService() {
        if (apiService != null) {
            apiService.stopService();
            apiService = null;
            logUtils.i(TAG, "stopApiService: API server stopped");
        }
    }
    
    /**
     * 重启 API 服务
     */
    public void restartApiService() {
        stopApiService();
        
        if (isApiEnabled()) {
            try {
                Thread.sleep(500); // 等待服务完全停止
            } catch (InterruptedException e) {
                logUtils.e(TAG, "restartApiService: Interrupted", e);
            }
            startApiService();
        }
    }
    
    /**
     * 检查 API 是否启用
     */
    public boolean isApiEnabled() {
        return prefs.getBoolean(KEY_API_ENABLED, false);
    }
    
    /**
     * 设置 API 启用状态
     */
    public void setApiEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_API_ENABLED, enabled).apply();
        logUtils.i(TAG, "setApiEnabled: API " + (enabled ? "enabled" : "disabled"));
        
        if (enabled) {
            startApiService();
        } else {
            stopApiService();
        }
    }
    
    /**
     * 获取 API 端口
     */
    public int getApiPort() {
        return prefs.getInt(KEY_API_PORT, DEFAULT_PORT);
    }
    
    /**
     * 设置 API 端口
     */
    public void setApiPort(int port) {
        if (port < 1024 || port > 65535) {
            logUtils.w(TAG, "setApiPort: Invalid port " + port + ", using default");
            port = DEFAULT_PORT;
        }
        
        prefs.edit().putInt(KEY_API_PORT, port).apply();
        logUtils.i(TAG, "setApiPort: Port set to " + port);
        
        // 如果服务正在运行，重启以应用新端口
        if (apiService != null) {
            restartApiService();
        }
    }
    
    /**
     * 获取 API 服务状态
     */
    public boolean isServiceRunning() {
        return apiService != null;
    }
    
    /**
     * 获取 API 访问 URL
     */
    public String getApiUrl() {
        int port = getApiPort();
        return "http://localhost:" + port;
    }
    
    /**
     * 获取 API 访问说明
     */
    public String getApiDocumentation() {
        StringBuilder doc = new StringBuilder();
        doc.append("JoyMan REST API\n");
        doc.append("================\n\n");
        doc.append("Base URL: ").append(getApiUrl()).append("\n");
        doc.append("Authentication: HTTP Basic Auth (username:password)\n");
        doc.append("Default credentials: admin / admin\n\n");
        doc.append("Endpoints:\n");
        doc.append("  GET    /issues.json      - List issues\n");
        doc.append("  POST   /issues.json      - Create issue\n");
        doc.append("  GET    /projects.json    - List projects\n");
        doc.append("  POST   /projects.json    - Create project\n\n");
        doc.append("Authentication Methods:\n");
        doc.append("  1. HTTP Header:\n");
        doc.append("     Authorization: Basic base64(username:password)\n\n");
        doc.append("  2. URL Parameters (for testing only):\n");
        doc.append("     ?username=admin&password=admin\n\n");
        doc.append("Example (curl):\n");
        doc.append("  # Get issues list\n");
        doc.append("  curl -u admin:admin \\\n");
        doc.append("       http://localhost:").append(getApiPort()).append("/issues.json\n\n");
        doc.append("  # Create issue\n");
        doc.append("  curl -u admin:admin -X POST \\\n");
        doc.append("       -H \"Content-Type: application/json\" \\\n");
        doc.append("       -d '{\"issue\":{\"subject\":\"New Task\",\"description\":\"Description\"}}' \\\n");
        doc.append("       http://localhost:").append(getApiPort()).append("/issues.json\n");
        
        return doc.toString();
    }
}