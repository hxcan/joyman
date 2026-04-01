package com.stupidbeauty.joyman.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.util.Date;

/**
 * Task 任务实体类
 * 
 * 对应数据库表：tasks
 * 用于存储单个任务的完整信息
 * 
 * @author 太极美术工程狮狮长
 * @version 2.0.0
 * @since 2026-03-31
 */
@Entity(tableName = "tasks")
public class Task {
    
    /**
     * 任务状态常量
     */
    public static final int STATUS_TODO = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_DONE = 2;
    public static final int STATUS_CANCELLED = 3;
    
    /**
     * 优先级常量
     */
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_URGENT = 4;
    
    /**
     * 主键 ID（14 位数字）
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    private long id;
    
    /**
     * 任务标题（必填）
     */
    @ColumnInfo(name = "title", defaultValue = "")
    private String title;
    
    /**
     * 任务描述（可选）
     */
    @ColumnInfo(name = "description", defaultValue = "''")
    private String description;
    
    /**
     * 任务状态（默认：待办）
     */
    @ColumnInfo(name = "status", defaultValue = "0")
    private int status;
    
    /**
     * 优先级（默认：普通）
     */
    @ColumnInfo(name = "priority", defaultValue = "2")
    private int priority;
    
    /**
     * 所属项目 ID（可选，外键）
     */
    @ColumnInfo(name = "project_id", defaultValue = "NULL")
    private Long projectId;
    
    /**
     * 创建时间（毫秒级时间戳）
     */
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    /**
     * 最后更新时间（毫秒级时间戳）
     */
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    /**
     * 截止时间（可选，毫秒级时间戳）
     */
    @ColumnInfo(name = "due_date", defaultValue = "NULL")
    private Long dueDate;
    
    /**
     * 标签（可选，JSON 格式存储）
     */
    @ColumnInfo(name = "tags", defaultValue = "''")
    private String tags;
    
    // ==================== 构造函数 ====================
    
    public Task() {
    }
    
    public Task(long id, String title) {
        this.id = id;
        this.title = title;
        this.status = STATUS_TODO;
        this.priority = PRIORITY_NORMAL;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }
    
    public Task(long id, String title, String description, int status, int priority, 
                long createdAt, long updatedAt, Long dueDate, String tags) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.dueDate = dueDate;
        this.tags = tags;
    }
    
    // ==================== Getter 和 Setter ====================
    
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getStatus() { return status; }
    public void setStatus(int status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) {
        this.priority = priority;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getTags() { return tags; }
    public void setTags(String tags) {
        this.tags = tags;
        this.updatedAt = System.currentTimeMillis();
    }
    
    // ==================== 辅助方法 ====================
    
    public boolean isDone() { return status == STATUS_DONE; }
    public boolean isCancelled() { return status == STATUS_CANCELLED; }
    public boolean hasDueDate() { return dueDate != null; }
    public boolean isOverdue() {
        if (dueDate == null) return false;
        return !isDone() && !isCancelled() && System.currentTimeMillis() > dueDate;
    }
    
    public String getStatusText() {
        switch (status) {
            case STATUS_TODO: return "待办";
            case STATUS_IN_PROGRESS: return "进行中";
            case STATUS_DONE: return "已完成";
            case STATUS_CANCELLED: return "已取消";
            default: return "未知";
        }
    }
    
    public String getPriorityText() {
        switch (priority) {
            case PRIORITY_LOW: return "低";
            case PRIORITY_NORMAL: return "普通";
            case PRIORITY_HIGH: return "高";
            case PRIORITY_URGENT: return "紧急";
            default: return "未知";
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return id == task.id;
    }
    
    @Override
    public int hashCode() { return Long.hashCode(id); }
    
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + getStatusText() +
                ", priority=" + getPriorityText() +
                ", projectId=" + projectId +
                '}';
    }
}