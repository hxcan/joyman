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
 * 字段说明：
 * - id: 14 位数字 ID，主键，由 IdGenerator 生成
 * - title: 任务标题（必填，最大长度 200 字符）
 * - description: 任务描述（可选，最大长度 5000 字符）
 * - status: 任务状态（0=待办，1=进行中，2=已完成，3=已取消）
 * - priority: 优先级（1=低，2=普通，3=高，4=紧急）
 * - createdAt: 创建时间（毫秒级时间戳）
 * - updatedAt: 最后更新时间（毫秒级时间戳）
 * - dueDate: 截止时间（可选，毫秒级时间戳）
 * - tags: 标签（可选，JSON 格式存储）
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-03-31
 */
@Entity(tableName = "tasks")
public class Task {
    
    /**
     * 任务状态常量
     */
    public static final int STATUS_TODO = 0;      // 待办
    public static final int STATUS_IN_PROGRESS = 1; // 进行中
    public static final int STATUS_DONE = 2;        // 已完成
    public static final int STATUS_CANCELLED = 3;   // 已取消
    
    /**
     * 优先级常量
     */
    public static final int PRIORITY_LOW = 1;      // 低
    public static final int PRIORITY_NORMAL = 2;   // 普通
    public static final int PRIORITY_HIGH = 3;     // 高
    public static final int PRIORITY_URGENT = 4;   // 紧急
    
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
    
    /**
     * 默认构造函数（Room 需要）
     */
    public Task() {
    }
    
    /**
     * 快速创建任务（仅标题）
     * 
     * @param id 任务 ID（由 IdGenerator 生成）
     * @param title 任务标题
     */
    public Task(long id, String title) {
        this.id = id;
        this.title = title;
        this.status = STATUS_TODO;
        this.priority = PRIORITY_NORMAL;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }
    
    /**
     * 完整构造函数
     */
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
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
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
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
        this.updatedAt = System.currentTimeMillis();
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
    
    public Long getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(Long dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
        this.updatedAt = System.currentTimeMillis();
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查任务是否已完成
     */
    public boolean isDone() {
        return status == STATUS_DONE;
    }
    
    /**
     * 检查任务是否已取消
     */
    public boolean isCancelled() {
        return status == STATUS_CANCELLED;
    }
    
    /**
     * 检查任务是否有截止时间
     */
    public boolean hasDueDate() {
        return dueDate != null;
    }
    
    /**
     * 检查任务是否已过期
     */
    public boolean isOverdue() {
        if (dueDate == null) {
            return false;
        }
        return !isDone() && !isCancelled() && System.currentTimeMillis() > dueDate;
    }
    
    /**
     * 获取状态文本描述
     */
    public String getStatusText() {
        switch (status) {
            case STATUS_TODO: return "待办";
            case STATUS_IN_PROGRESS: return "进行中";
            case STATUS_DONE: return "已完成";
            case STATUS_CANCELLED: return "已取消";
            default: return "未知";
        }
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
    
    // ==================== Object 方法重写 ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return id == task.id;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + getStatusText() +
                ", priority=" + getPriorityText() +
                ", createdAt=" + new Date(createdAt) +
                '}';
    }
}