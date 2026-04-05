package com.stupidbeauty.joyman.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.util.LogUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * API 数据转换器
 * 
 * 负责 Task/Project 与 Redmine 兼容的 JSON 格式之间的转换
 * 支持 ISO 8601 时间格式
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-04-05
 */
public class ApiJsonConverter {
    
    private static final String TAG = "ApiJsonConverter";
    private static final LogUtils logUtils = LogUtils.getInstance();
    private static final Gson gson = new Gson();
    
    // ISO 8601 时间格式：2026-04-05T10:00:00Z
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    
    static {
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * 将 Task 转换为 Redmine Issue JSON 格式
     */
    public static JsonObject taskToIssueJson(Task task, Project project) {
        JsonObject issueJson = new JsonObject();
        
        try {
            issueJson.addProperty("id", task.getId());
            issueJson.addProperty("subject", task.getTitle());
            issueJson.addProperty("description", task.getDescription() != null ? task.getDescription() : "");
            issueJson.addProperty("priority", task.getPriority());
            
            // 状态对象
            JsonObject statusJson = new JsonObject();
            statusJson.addProperty("id", task.getStatus());
            statusJson.addProperty("name", task.getStatusText());
            issueJson.add("status", statusJson);
            
            // 项目对象（如果有）
            if (project != null) {
                JsonObject projectJson = new JsonObject();
                projectJson.addProperty("id", project.getId());
                projectJson.addProperty("name", project.getName());
                issueJson.add("project", projectJson);
            }
            
            // 父任务对象（如果是子任务）
            if (task.getParentId() != null) {
                JsonObject parentJson = new JsonObject();
                parentJson.addProperty("id", task.getParentId());
                issueJson.add("parent", parentJson);
            }
            
            // 时间戳
            issueJson.addProperty("created_on", formatTimestamp(task.getCreatedAt()));
            issueJson.addProperty("updated_on", formatTimestamp(task.getUpdatedAt()));
            
            if (task.getDueDate() != null) {
                issueJson.addProperty("due_date", formatTimestamp(task.getDueDate()));
            }
            
            logUtils.d(TAG, "taskToIssueJson: Converted task " + task.getId() + " to JSON");
            
        } catch (Exception e) {
            logUtils.e(TAG, "taskToIssueJson: Error converting task to JSON", e);
        }
        
        return issueJson;
    }
    
    /**
     * 将 Task 列表转换为 Redmine Issues JSON 格式
     */
    public static JsonObject tasksToIssuesJson(List<Task> tasks, int totalCount, int offset, int limit) {
        JsonObject responseJson = new JsonObject();
        
        try {
            com.google.gson.JsonArray issuesArray = new com.google.gson.JsonArray();
            
            for (Task task : tasks) {
                // 这里简化处理，实际应该传入对应的 Project
                JsonObject issueJson = taskToIssueJson(task, null);
                issuesArray.add(issueJson);
            }
            
            responseJson.add("issues", issuesArray);
            responseJson.addProperty("total_count", totalCount);
            responseJson.addProperty("offset", offset);
            responseJson.addProperty("limit", limit);
            
            logUtils.d(TAG, "tasksToIssuesJson: Converted " + tasks.size() + " tasks to JSON");
            
        } catch (Exception e) {
            logUtils.e(TAG, "tasksToIssuesJson: Error converting tasks to JSON", e);
        }
        
        return responseJson;
    }
    
    /**
     * 将 Project 转换为 Redmine Project JSON 格式
     */
    public static JsonObject projectToJson(Project project) {
        JsonObject projectJson = new JsonObject();
        
        try {
            projectJson.addProperty("id", project.getId());
            projectJson.addProperty("name", project.getName());
            projectJson.addProperty("identifier", "project-" + project.getId()); // 简化处理
            projectJson.addProperty("description", project.getDescription() != null ? project.getDescription() : "");
            
            // 时间戳
            projectJson.addProperty("created_on", formatTimestamp(project.getCreatedAt()));
            projectJson.addProperty("updated_on", formatTimestamp(project.getUpdatedAt()));
            
            logUtils.d(TAG, "projectToJson: Converted project " + project.getId() + " to JSON");
            
        } catch (Exception e) {
            logUtils.e(TAG, "projectToJson: Error converting project to JSON", e);
        }
        
        return projectJson;
    }
    
    /**
     * 将 Project 列表转换为 Redmine Projects JSON 格式
     */
    public static JsonObject projectsToJson(List<Project> projects) {
        JsonObject responseJson = new JsonObject();
        
        try {
            com.google.gson.JsonArray projectsArray = new com.google.gson.JsonArray();
            
            for (Project project : projects) {
                JsonObject projectJson = projectToJson(project);
                projectsArray.add(projectJson);
            }
            
            responseJson.add("projects", projectsArray);
            
            logUtils.d(TAG, "projectsToJson: Converted " + projects.size() + " projects to JSON");
            
        } catch (Exception e) {
            logUtils.e(TAG, "projectsToJson: Error converting projects to JSON", e);
        }
        
        return responseJson;
    }
    
    /**
     * 从 JSON 创建 Task 对象
     * 用于 POST /issues.json 请求
     */
    public static Task jsonToTask(JsonObject issueJson) {
        Task task = new Task();
        
        try {
            // 标题（必填）
            if (issueJson.has("subject")) {
                task.setTitle(issueJson.get("subject").getAsString());
            }
            
            // 描述
            if (issueJson.has("description")) {
                task.setDescription(issueJson.get("description").getAsString());
            }
            
            // 状态 ID
            if (issueJson.has("status_id")) {
                task.setStatus(issueJson.get("status_id").getAsInt());
            } else {
                task.setStatus(Task.STATUS_NEW); // 默认新建
            }
            
            // 优先级
            if (issueJson.has("priority")) {
                task.setPriority(issueJson.get("priority").getAsInt());
            } else {
                task.setPriority(Task.PRIORITY_NORMAL); // 默认普通
            }
            
            // 项目 ID
            if (issueJson.has("project_id")) {
                task.setProjectId(issueJson.get("project_id").getAsLong());
            }
            
            // 父任务 ID（创建子任务）
            if (issueJson.has("parent_issue_id")) {
                task.setParentId(issueJson.get("parent_issue_id").getAsLong());
            }
            
            logUtils.d(TAG, "jsonToTask: Created task from JSON: " + task.getTitle());
            
        } catch (Exception e) {
            logUtils.e(TAG, "jsonToTask: Error parsing JSON to task", e);
        }
        
        return task;
    }
    
    /**
     * 从 JSON 字符串创建 Task 对象
     */
    public static Task parseIssueJson(String jsonString) {
        try {
            JsonObject rootJson = JsonParser.parseString(jsonString).getAsJsonObject();
            
            // Redmine 格式：{ "issue": { ... } }
            if (rootJson.has("issue")) {
                JsonObject issueJson = rootJson.getAsJsonObject("issue");
                return jsonToTask(issueJson);
            } else {
                // 直接是 issue 对象
                return jsonToTask(rootJson);
            }
            
        } catch (Exception e) {
            logUtils.e(TAG, "parseIssueJson: Error parsing JSON string", e);
            return null;
        }
    }
    
    /**
     * 格式化时间戳为 ISO 8601 格式
     */
    public static String formatTimestamp(long timestamp) {
        return ISO_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * 解析 ISO 8601 时间字符串
     */
    public static long parseTimestamp(String isoString) {
        try {
            Date date = ISO_FORMAT.parse(isoString);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            logUtils.e(TAG, "parseTimestamp: Error parsing timestamp", e);
            return System.currentTimeMillis();
        }
    }
    
    /**
     * 创建错误响应 JSON
     */
    public static JsonObject createErrorJson(String message) {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("error", message);
        return errorJson;
    }
    
    /**
     * 创建成功响应 JSON
     */
    public static JsonObject createSuccessJson(String message) {
        JsonObject successJson = new JsonObject();
        successJson.addProperty("message", message);
        return successJson;
    }
}