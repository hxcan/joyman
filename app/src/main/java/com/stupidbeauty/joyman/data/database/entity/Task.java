package com.stupidbeauty.joyman.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import java.util.ArrayList;
import java.util.List;


/**
 * Task 任务实体类
 * 
 * 对应数据库表：tasks
 * 用于存储单个任务的完整信息
 * 
 * @author 太极美术工程狮狮长
 * @version 2.0.4
 * @since 2026-03-31
 */
@Entity(
    tableName = "tasks",
    indices = {
        @Index(value = {"project_id"}),
        @Index(value = {"parent_id"})
    }
)
public class Task {
    
    /**
     * JoyMan 默认任务状态集合（固定值，与 Redmine 典型默认状态一致）
     * 
     * 说明：
     * - ID 为固定连续值，不随实例变化
     * - 新创建的 JoyMan 项目将使用此默认状态集合
     * - 后续版本可支持自定义状态，但默认值保持不变
     * 
     * 参考：Redmine 初次安装时的默认状态
     */
    public static final int STATUS_NEW = 1;          // 新建
    public static final int STATUS_IN_PROGRESS = 2;  // 进行中
    public static final int STATUS_RESOLVED = 3;     // 已解决
    public static final int STATUS_FEEDBACK = 4;     // 反馈中
    public static final int STATUS_CLOSED = 5;       // 已关闭
    
    /**
     * 向后兼容的别名（旧代码可继续使用）
     */
    @Deprecated
    public static final int STATUS_TODO = STATUS_NEW;
    @Deprecated
    public static final int STATUS_DONE = STATUS_CLOSED;
    
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
     * 任务状态（默认：新建）
     * 取值范围：1-5（JoyMan 默认状态集合）
     */
    @ColumnInfo(name = "status", defaultValue = "1")
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
     * 父任务 ID（可选，用于子任务）
     */
    @ColumnInfo(name = "parent_id", defaultValue = "NULL")
    private Long parentId;
    
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
        this.status = STATUS_NEW;  // 默认状态：新建
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
    
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) {
        this.parentId = parentId;
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
    
    public boolean isDone() { return status == STATUS_CLOSED || status == STATUS_RESOLVED; }
    public boolean isCancelled() { return status == STATUS_CLOSED; }
    public boolean hasDueDate() { return dueDate != null; }
    public boolean isOverdue() {
        if (dueDate == null) return false;
        return !isDone() && !isCancelled() && System.currentTimeMillis() > dueDate;
    }
    
    /**
     * 判断是否为子任务
     */
    public boolean isSubtask() { return parentId != null; }
    
    /**
     * 获取状态的中文显示文本
     */
    public String getStatusText() {
        switch (status) {
            case STATUS_NEW: return "新建";
            case STATUS_IN_PROGRESS: return "进行中";
            case STATUS_RESOLVED: return "已解决";
            case STATUS_FEEDBACK: return "反馈中";
            case STATUS_CLOSED: return "已关闭";
            default: return "未知 (" + status + ")";
        }
    }
    
    /**
     * 根据状态 ID 获取状态名称（静态方法，便于 UI 使用）
     * @param statusId 状态 ID（JoyMan 默认：1-5）
     * @return 状态中文名称
     */
    public static String getStatusNameById(int statusId) {
        switch (statusId) {
            case STATUS_NEW: return "新建";
            case STATUS_IN_PROGRESS: return "进行中";
            case STATUS_RESOLVED: return "已解决";
            case STATUS_FEEDBACK: return "反馈中";
            case STATUS_CLOSED: return "已关闭";
            default: return "未知 (" + statusId + ")";
        }
    }
    
    /**
     * 获取状态的 CSS 类名（用于 UI 样式）
     */
    public String getStatusClass() {
        switch (status) {
            case STATUS_NEW: return "status-new";
            case STATUS_IN_PROGRESS: return "status-in-progress";
            case STATUS_RESOLVED: return "status-resolved";
            case STATUS_FEEDBACK: return "status-feedback";
            case STATUS_CLOSED: return "status-closed";
            default: return "status-unknown";
        }
    }
    
    /**
     * 获取 JoyMan 默认状态列表
     * @return 状态 ID 数组 [1, 2, 3, 4, 5]
     */
    public static int[] getDefaultStatusIds() {
        return new int[]{STATUS_NEW, STATUS_IN_PROGRESS, STATUS_RESOLVED, STATUS_FEEDBACK, STATUS_CLOSED};
    }
    
    /**
     * 获取状态 ID 对应的中文名称列表
     * @return 状态名称数组 ["新建", "进行中", "已解决", "反馈中", "已关闭"]
     */
    public static String[] getDefaultStatusNames() {
        return new String[]{"新建", "进行中", "已解决", "反馈中", "已关闭"};
    }
    
    /**
     * 验证状态 ID 是否为有效的默认状态
     * @param statusId 待验证的状态 ID
     * @return true 如果是有效的默认状态
     */
    public static boolean isValidDefaultStatus(int statusId) {
        return statusId >= STATUS_NEW && statusId <= STATUS_CLOSED;
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
                ", parentId=" + parentId +
                '}';
    }
}