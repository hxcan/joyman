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

    /**
     * 动态获取当前支持的 API 端点列表（运行时刻列举，不写死）
     */
    private Map<String, List<String>> getAvailableEndpoints()
    {
        Map<String, List<String>> endpoints = new HashMap<>();
        endpoints.put("issues.json", Arrays.asList("GET", "POST"));
        endpoints.put("issues/{id}.json", Arrays.asList("GET", "PUT", "DELETE"));
        endpoints.put("search.json", Arrays.asList("GET"));
        endpoints.put("projects.json", Arrays.asList("GET", "POST"));
        return endpoints;
    }

    /**
     * 构建可用端点的 JSON 响应
     */
    private String buildAvailableEndpointsJson()
    {
        Map<String, List<String>> endpoints = getAvailableEndpoints();
        JsonObject root = new JsonObject();
        JsonArray endpointsArray = new JsonArray();
        for (Map.Entry<String, List<String>> entry : endpoints.entrySet())
        {
            JsonObject endpoint = new JsonObject();
            endpoint.addProperty("path", entry.getKey());
            JsonArray methodsArray = new JsonArray();
            for (String method : entry.getValue())
            {
                methodsArray.add(method);
            }
            endpoint.add("methods", methodsArray);
            endpointsArray.add(endpoint);
        }
        root.add("endpoints", endpointsArray);
        return root.toString();
    }

    /**
     * 清理 Chunked Transfer Encoding 的数据
     * 修复：移除错误的 JSON 起始位置查找逻辑，避免截断合法的 JSON 内容
     *
     * 问题原因：
     * - 原代码使用 indexOf('{') 查找 JSON 起始位置，但当描述字段包含 Markdown 格式的 JSON 代码块时（如 ```json），
     * 会错误地定位到描述内容中的 { 字符，导致截断了真正的 JSON 开头
     * - NanoHTTPD 的 parseBody 已经正确处理了 Chunked Encoding，不需要额外处理
     *
     * 解决方案：
     * - 直接信任 parseBody 返回的数据是完整的
     * - 只清理可能的 Chunked 结尾标记（\r\n0\r\n 等）
     * - 添加详细日志用于调试
     */
    private String cleanChunkedData(String data)
    {
        if (data == null || data.isEmpty())
        {
            return data;
        }
        logUtils.d(TAG, "cleanChunkedData: === START ===");
        logUtils.d(TAG, "cleanChunkedData: Original data length: " + data.length());
        logUtils.d(TAG, "cleanChunkedData: Original data preview (first 300 chars): " + (data.length() > 300 ? data.substring(0, 300) + "..." : data));

        // 不再查找并截取 JSON 起始位置，直接使用原始数据
        // 原因：描述字段中可能包含 Markdown 格式的 JSON 代码块，导致 indexOf('{') 定位错误
        // 仅清理可能的 Chunked Encoding 结尾标记
        String[] chunkedEndings = {
            "\r\n0\r\n", "\r\n0\n", "\n0\r\n", "\n0\n", "\r\n0", "\n0"
        };
        String originalData = data;
        for (String ending : chunkedEndings)
        {
            if (data.endsWith(ending))
            {
                logUtils.d(TAG, "cleanChunkedData: Removing chunked ending: " + escapeSpecialChars(ending));
                data = data.substring(0, data.length() - ending.length());
                break;
            }
        }

        // 移除首尾空白字符
        data = data.trim();

        logUtils.d(TAG, "cleanChunkedData: Cleaned data length: " + data.length());
        logUtils.d(TAG, "cleanChunkedData: Cleaned data preview (first 300 chars): " + (data.length() > 300 ? data.substring(0, 300) + "..." : data));

        if (!originalData.equals(data))
        {
            logUtils.d(TAG, "cleanChunkedData: Data was modified (length changed from " + originalData.length() + " to " + data.length() + ")");
        }
        else
        {
            logUtils.d(TAG, "cleanChunkedData: Data unchanged (no chunked markers found)");
        }

        logUtils.d(TAG, "cleanChunkedData: === END ===");
        return data;
    }

    /**
     * 转义特殊字符用于日志显示
     */
    private String escapeSpecialChars(String str)
    {
        return str.replace("\r", "\\r").replace("\n", "\\n");
    }

    /**
     * 检测字符串是否为文件路径
     */
    private boolean isFilePath(String path)
    {
        if (path == null || path.isEmpty())
        {
            return false;
        }
        return (path.startsWith("/") || path.startsWith("C:") || path.startsWith("D:")) &&
               (path.contains("/cache/") || path.contains("\\cache\\") ||
                path.contains("/tmp/") || path.contains("\\tmp\\") ||
                path.contains("NanoHTTPD"));
    }

    /**
     * 从文件读取内容（Android 兼容方式，支持 API 24+）
     */
    private String readFileContent(String filePath)
    {
        try
        {
            logUtils.d(TAG, "readFileContent: Reading file: " + filePath);
            File file = new File(filePath);
            if (!file.exists())
            {
                logUtils.e(TAG, "readFileContent: File does not exist: " + filePath);
                return null;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            FileInputStream inputStream = null;
            try
            {
                inputStream = new FileInputStream(file);
                byte[] data = new byte[8192];
                int nRead;
                while ((nRead = inputStream.read(data, 0, data.length)) != -1)
                {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                String content = buffer.toString(StandardCharsets.UTF_8.name());
                logUtils.d(TAG, "readFileContent: File size: " + content.length() + " chars");
                logUtils.d(TAG, "readFileContent: Content preview: " + (content.length() > 200 ? content.substring(0, 200) + "..." : content));
                return content;
            }
            finally
            {
                if (inputStream != null)
                {
                    try
                    {
                        inputStream.close();
                    }
                    catch (IOException e)
                    {
                        logUtils.w(TAG, "readFileContent: Error closing stream: " + e.getMessage());
                    }
                }
            }
        }
        catch (IOException e)
        {
            logUtils.e(TAG, "readFileContent: Error reading file: " + filePath, e);
            return null;
        }
    }

    /**
     * 从输入流读取请求体（带详细调试日志）
     */
    private String readRequestBodyFromStream(IHTTPSession session)
    {
        logUtils.d(TAG, "readRequestBodyFromStream: === START ===");
        try
        {
            Map<String, String> headers = session.getHeaders();
            logUtils.d(TAG, "readRequestBodyFromStream: Headers count: " + (headers != null ? headers.size() : 0));
            if (headers != null)
            {
                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    logUtils.d(TAG, "readRequestBodyFromStream: Header[" + entry.getKey() + "] = " + entry.getValue());
                }
            }

            String contentLength = headers != null ? headers.get("content-length") : null;
            logUtils.d(TAG, "readRequestBodyFromStream: Content-Length = " + contentLength);
            logUtils.d(TAG, "readRequestBodyFromStream: Method = " + session.getMethod());
            logUtils.d(TAG, "readRequestBodyFromStream: URI = " + session.getUri());
            logUtils.d(TAG, "readRequestBodyFromStream: Trying to get input stream...");

            if (contentLength != null && !contentLength.isEmpty())
            {
                int len = Integer.parseInt(contentLength);
                logUtils.d(TAG, "readRequestBodyFromStream: Content-Length parsed as " + len + " bytes");
                if (len > 0)
                {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[len];
                    int totalRead = 0;
                    try
                    {
                        int nRead;
                        while ((nRead = session.getInputStream().read(data, 0, len)) != -1)
                        {
                            buffer.write(data, 0, nRead);
                            totalRead += nRead;
                            logUtils.d(TAG, "readRequestBodyFromStream: Read chunk of " + nRead + " bytes (total: " + totalRead + ")");
                            if (totalRead >= len)
                            {
                                break;
                            }
                        }
                        String result = buffer.toString(StandardCharsets.UTF_8.name());
                        logUtils.d(TAG, "readRequestBodyFromStream: Successfully read " + totalRead + " bytes");
                        logUtils.d(TAG, "readRequestBodyFromStream: Data preview: " + (result.length() > 100 ? result.substring(0, 100) + "..." : result));
                        logUtils.d(TAG, "readRequestBodyFromStream: === END (success) ===");
                        return result;
                    }
                    catch (Exception e)
                    {
                        logUtils.e(TAG, "readRequestBodyFromStream: Error reading stream", e);
                        logUtils.d(TAG, "readRequestBodyFromStream: === END (error) ===");
                        return null;
                    }
                }
                else
                {
                    logUtils.w(TAG, "readRequestBodyFromStream: Content-Length is 0, no data to read");
                    logUtils.d(TAG, "readRequestBodyFromStream: === END (zero length) ===");
                    return null;
                }
            }
            else
            {
                logUtils.w(TAG, "readRequestBodyFromStream: Content-Length is null or empty, trying to read anyway...");
                try
                {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[1024];
                    int nRead;
                    int totalRead = 0;
                    while ((nRead = session.getInputStream().read(data, 0, 1024)) != -1)
                    {
                        buffer.write(data, 0, nRead);
                        totalRead += nRead;
                        logUtils.d(TAG, "readRequestBodyFromStream: Read chunk of " + nRead + " bytes (total: " + totalRead + ")");
                    }
                    if (totalRead > 0)
                    {
                        String result = buffer.toString(StandardCharsets.UTF_8.name());
                        logUtils.d(TAG, "readRequestBodyFromStream: Successfully read " + totalRead + " bytes (no Content-Length)");
                        logUtils.d(TAG, "readRequestBodyFromStream: Data preview: " + (result.length() > 100 ? result.substring(0, 100) + "..." : result));
                        logUtils.d(TAG, "readRequestBodyFromStream: === END (success, no CL) ===");
                        return result;
                    }
                    else
                    {
                        logUtils.w(TAG, "readRequestBodyFromStream: No data read from stream");
                        logUtils.d(TAG, "readRequestBodyFromStream: === END (no data) ===");
                        return null;
                    }
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "readRequestBodyFromStream: Error reading stream without Content-Length", e);
                    logUtils.d(TAG, "readRequestBodyFromStream: === END (error, no CL) ===");
                    return null;
                }
            }
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "readRequestBodyFromStream: Unexpected error", e);
            logUtils.d(TAG, "readRequestBodyFromStream: === END (unexpected error) ===");
            return null;
        }
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        String uri = normalizeUri(session.getUri());
        Method method = session.getMethod();
        logUtils.i(TAG, "Request: " + method + " " + uri + " from " + session.getRemoteIpAddress());

        logUtils.d(TAG, "serve: === 原始请求信息 START ===");
        logUtils.d(TAG, "serve: Raw URI: " + session.getUri());
        String fullUri = session.getUri();
        String queryString = "";
        int queryIndex = fullUri.indexOf('?');
        if (queryIndex >= 0)
        {
            queryString = fullUri.substring(queryIndex + 1);
        }
        logUtils.d(TAG, "serve: Query String: " + queryString);
        logUtils.d(TAG, "serve: Method: " + method);
        logUtils.d(TAG, "serve: Remote IP: " + session.getRemoteIpAddress());
        logUtils.d(TAG, "serve: === 原始请求信息 END ===");

        if (method.equals(Method.PUT) || method.equals(Method.POST))
        {
            Map<String, String> headers = session.getHeaders();
            if (headers != null)
            {
                logUtils.d(TAG, "serve: Request headers for " + method + " " + uri + ":");
                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    logUtils.d(TAG, "  " + entry.getKey() + ": " + entry.getValue());
                }
            }
        }

        if (Method.OPTIONS.equals(method))
        {
            logUtils.d(TAG, "serve: Handling OPTIONS preflight request");
            return createCorsResponse(Response.Status.OK, "text/plain", "");
        }

        if (!authenticate(session))
        {
            logUtils.w(TAG, "serve: Authentication failed for " + uri);
            return createCorsResponse(Response.Status.UNAUTHORIZED, "application/json", "{\"error\":\"Unauthorized\"}");
        }

        try
        {
            if (uri.equals("issues.json"))
            {
                return handleIssues(session, method);
            }
            else if (uri.equals("search.json"))
            {
                return handleSearch(session, method);
            }
            else if (uri.startsWith("issues/") && uri.endsWith(".json"))
            {
                return handleIssueDetail(session, method, uri);
            }
            else if (uri.equals("projects.json"))
            {
                return handleProjects(session, method);
            }
            else if (uri.startsWith("projects/") && uri.endsWith(".json"))
            {
                logUtils.w(TAG, "Unknown endpoint: " + uri);
                String responseBody = "{\"error\":\"Unknown endpoint: " + uri + "\",\"available_endpoints\":" + buildAvailableEndpointsJson() + "}";
                return createCorsResponse(Response.Status.NOT_FOUND, "application/json", responseBody);
            }
            else
            {
                logUtils.w(TAG, "Unknown endpoint: " + uri);
                String responseBody = "{\"error\":\"Unknown endpoint: " + uri + "\",\"available_endpoints\":" + buildAvailableEndpointsJson() + "}";
                return createCorsResponse(Response.Status.NOT_FOUND, "application/json", responseBody);
            }
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "serve: Error handling request", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 处理 /search.json 请求
     */
    private Response handleSearch(IHTTPSession session, Method method)
    {
        if (!Method.GET.equals(method))
        {
            logUtils.w(TAG, "handleSearch: Method not allowed: " + method);
            return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", "{\"error\":\"Method not allowed\"}");
        }

        Map<String, String> params = session.getParms();
        String query = params.get("q");
        String issuesFlag = params.get("issues");
        int offset = parseIntSafe(params.get("offset"), 0);
        int limit = parseIntSafe(params.get("limit"), 25);

        logUtils.d(TAG, "handleSearch: q=" + query + ", issues=" + issuesFlag + ", limit=" + limit + ", offset=" + offset);

        if (issuesFlag == null || !"1".equals(issuesFlag))
        {
            JsonObject emptyResponse = new JsonObject();
            emptyResponse.add("results", new JsonArray());
            emptyResponse.addProperty("total_count", 0);
            emptyResponse.addProperty("offset", offset);
            emptyResponse.addProperty("limit", limit);
            return createCorsResponse(Response.Status.OK, "application/json", emptyResponse.toString());
        }

        if (query == null || query.trim().isEmpty())
        {
            logUtils.w(TAG, "handleSearch: No query provided, returning all issues");
            return getIssues(session);
        }

        try
        {
            List<Task> results = searchTasks(query, limit, offset);
            JsonArray resultsArray = new JsonArray();
            for (Task task : results)
            {
                JsonObject result = new JsonObject();
                result.addProperty("id", task.getId());
                result.addProperty("title", "Issue #" + task.getId() + ": " + task.getTitle());
                result.addProperty("type", "issue");
                result.addProperty("url", "/issues/" + task.getId());
                result.addProperty("description", task.getDescription() != null ? task.getDescription() : "");
                result.addProperty("datetime", formatDateTime(task.getCreatedAt()));

                if (task.getProjectId() != null)
                {
                    Project project = projectRepository.getProjectById(task.getProjectId());
                    if (project != null)
                    {
                        JsonObject projectObj = new JsonObject();
                        projectObj.addProperty("id", project.getId());
                        projectObj.addProperty("name", project.getName());
                        result.add("project", projectObj);
                    }
                }

                resultsArray.add(result);
            }

            JsonObject response = new JsonObject();
            response.add("results", resultsArray);
            response.addProperty("total_count", results.size());
            response.addProperty("offset", offset);
            response.addProperty("limit", limit);

            logUtils.i(TAG, "handleSearch: Found " + results.size() + " results for query: " + query);
            return createCorsResponse(Response.Status.OK, "application/json", response.toString());
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "handleSearch: Error executing search", e);
            return createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Search failed: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 执行任务搜索（使用内存过滤模拟 SQL LIKE）
     */
    private List<Task> searchTasks(String query, int limit, int offset)
    {
        String[] tokens = query.split("\\s+");
        List<String> validTokens = new ArrayList<>();
        for (String token : tokens)
        {
            if (token.length() >= 2 && !token.isEmpty())
            {
                validTokens.add(token.toLowerCase());
            }
        }

        if (validTokens.isEmpty())
        {
            return new ArrayList<>();
        }

        logUtils.d(TAG, "searchTasks: Searching with tokens: " + validTokens);

        List<Task> allTasks = taskRepository.getAllTasks();
        if (allTasks == null || allTasks.isEmpty())
        {
            return new ArrayList<>();
        }

        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : allTasks)
        {
            boolean matches = true;
            for (String token : validTokens)
            {
                String lowerToken = token.toLowerCase();
                boolean titleMatch = task.getTitle().toLowerCase().contains(lowerToken);
                boolean descMatch = task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerToken);

                if (!titleMatch && !descMatch)
                {
                    matches = false;
                    break;
                }
            }
            if (matches)
            {
                filteredTasks.add(task);
            }
        }

        // 按创建时间倒序排序
        filteredTasks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        // 分页
        int totalCount = filteredTasks.size();
        int fromIndex = Math.min(offset, totalCount);
        int toIndex = Math.min(offset + limit, totalCount);
        List<Task> paginatedTasks = fromIndex < toIndex ? filteredTasks.subList(fromIndex, toIndex) : new ArrayList<>();

        logUtils.i(TAG, "searchTasks: Found " + paginatedTasks.size() + " of " + totalCount + " matching tasks");
        return paginatedTasks;
    }

    /**
     * 格式化日期时间为 ISO 8601 格式
     */
    private String formatDateTime(long timestamp)
    {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(timestamp));
    }

    private Response handleIssues(IHTTPSession session, Method method)
    {
        switch (method)
        {
            case GET:
                return getIssues(session);
            case POST:
                return createIssue(session);
            default:
                logUtils.w(TAG, "handleIssues: Method not allowed: " + method);
                return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", "{\"error\":\"Method not allowed\"}");
        }
    }

    private Response handleIssueDetail(IHTTPSession session, Method method, String uri)
    {
        Matcher matcher = ISSUE_ID_PATTERN.matcher(uri);
        if (!matcher.find())
        {
            logUtils.w(TAG, "handleIssueDetail: Invalid issue ID pattern: " + uri);
            return createCorsResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Invalid issue ID\"}");
        }
        long issueId = Long.parseLong(matcher.group(1));

        switch (method)
        {
            case GET:
                return getIssue(session, issueId);
            case PUT:
                return updateIssue(session, issueId);
            case DELETE:
                return deleteIssue(session, issueId);
            default:
                logUtils.w(TAG, "handleIssueDetail: Method not allowed: " + method);
                return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", "{\"error\":\"Method not allowed\"}");
        }
    }

    private Response handleProjects(IHTTPSession session, Method method)
    {
        switch (method)
        {
            case GET:
                return getProjects(session);
            case POST:
                return createProject(session);
            default:
                logUtils.w(TAG, "handleProjects: Method not allowed: " + method);
                return createCorsResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", "{\"error\":\"Method not allowed\"}");
        }
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
            {
                comments = new ArrayList<>();
            }

            // 🔍 [DEBUG] 第 2 行日志
            logUtils.i(TAG, "🔍 [DEBUG] comments count=" + comments.size());
            if (comments == null)

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


        // 🔍 [DEBUG] 第 3 行日志
        logUtils.i(TAG, "🔍 [DEBUG] has journals=" + responseJson.has("journals"));
                journalsArray.add(journal);
            }

            responseJson.add("journals", journalsArray);
            logUtils.i(TAG, "getIssue: Included " + comments.size() + " journals/comments");
        }

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