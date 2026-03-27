package com.stupidbeauty.joyman.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * 任务实体类 - 对应数据库 tasks 表
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 */
@Entity(tableName = "tasks")
public class Task {
    
    // 优先级常量
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_URGENT = 4;
    
    // 状态常量
    public static final int STATUS_NEW = 1;
    public static final int STATUS_IN_PROGRESS = 2;
    public static final int STATUS_DONE = 3;
    public static final int STATUS_CLOSED = 4;
    
    /**
     * 主键 ID - 随机生成的 14 位数字
     * 格式：时间戳 (10 位) + 随机数 (4 位)
     */
    @PrimaryKey
    @NonNull
    private long id;
    
    /**
     * 任务标题
     */
    private String title;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 优先级：1=低，2=普通，3=高，4=紧急
     */
    private int priority;
    
    /**
     * 状态：1=新建，2=进行中，3=已完成，4=已关闭
     */
    private int status;
    
    /**
     * 创建时间戳（毫秒）
     */
    private long createdAt;
    
    /**
     * 更新时间戳（毫秒）
     */
    private long updatedAt;
    
    /**
     * 原始 ID（用于从 Redmine 导入时保留原 ID）
     */
    private Long originalId;
    
    /**
     * 所属项目 ID
     */
    private Long projectId;
    
    /**
     * 父任务 ID（支持子任务）
     */
    private Long parentTaskId;
    
    /**
     * 构造函数
     */
    public Task(long id, String title, String description, int priority, int status, long createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
    
    // Getters and Setters
    
    @NonNull
    public long getId() {
        return id;
    }
    
    public void setId(@NonNull long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getOriginalId() {
        return originalId;
    }
    
    public void setOriginalId(Long originalId) {
        this.originalId = originalId;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public Long getParentTaskId() {
        return parentTaskId;
    }
    
    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }
    
    /**
     * 获取优先级文本描述
     */
    public String getPriorityText() {
        switch (priority) {
            case PRIORITY_LOW: return "低";
            case PRIORITY_NORMAL: return "普通";
            case PRIORITY_HIGH: return "高";
            case PRIORITY_URGENT: return "紧急";
            default: return "未知";
        }
    }
    
    /**
     * 获取状态文本描述
     */
    public String getStatusText() {
        switch (status) {
            case STATUS_NEW: return "新建";
            case STATUS_IN_PROGRESS: return "进行中";
            case STATUS_DONE: return "已完成";
            case STATUS_CLOSED: return "已关闭";
            default: return "未知";
        }
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", priority=" + getPriorityText() +
                ", status=" + getStatusText() +
                '}';
    }
}
