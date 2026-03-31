package com.stupidbeauty.joyman.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.util.Date;

/**
 * Project 项目实体类
 * 
 * 对应数据库表：projects
 * 用于存储项目（任务分组）信息
 * 
 * 项目用于将相关任务组织在一起，例如：
 * - "工作项目"：包含所有工作任务
 * - "学习计划"：包含所有学习任务
 * - "家庭事务"：包含所有家庭相关任务
 * 
 * 字段说明：
 * - id: 14 位数字 ID，主键，由 IdGenerator 生成
 * - name: 项目名称（必填，最大长度 100 字符）
 * - description: 项目描述（可选，最大长度 2000 字符）
 * - color: 项目颜色（可选，十六进制格式如 "#FF5722"）
 * - icon: 项目图标（可选，Emoji 或图标名称）
 * - createdAt: 创建时间（毫秒级时间戳）
 * - updatedAt: 最后更新时间（毫秒级时间戳）
 * - sortOrder: 排序顺序（数字越小越靠前）
 * - isArchived: 是否已归档（默认 false）
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-03-31
 */
@Entity(tableName = "projects")
public class Project {
    
    /**
     * 默认项目颜色
     */
    public static final String DEFAULT_COLOR = "#2196F3"; // Material Blue
    
    /**
     * 主键 ID（14 位数字）
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    private long id;
    
    /**
     * 项目名称（必填）
     */
    @ColumnInfo(name = "name", defaultValue = "")
    private String name;
    
    /**
     * 项目描述（可选）
     */
    @ColumnInfo(name = "description", defaultValue = "''")
    private String description;
    
    /**
     * 项目颜色（十六进制格式，可选）
     */
    @ColumnInfo(name = "color", defaultValue = "'#2196F3'")
    private String color;
    
    /**
     * 项目图标（可选，Emoji 或图标名称）
     */
    @ColumnInfo(name = "icon", defaultValue = "''")
    private String icon;
    
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
     * 排序顺序（数字越小越靠前，默认：0）
     */
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    private int sortOrder;
    
    /**
     * 是否已归档（默认：false）
     */
    @ColumnInfo(name = "is_archived", defaultValue = "0")
    private boolean isArchived;
    
    // ==================== 构造函数 ====================
    
    /**
     * 默认构造函数（Room 需要）
     */
    public Project() {
    }
    
    /**
     * 快速创建项目（仅名称）
     * 
     * @param id 项目 ID（由 IdGenerator 生成）
     * @param name 项目名称
     */
    public Project(long id, String name) {
        this.id = id;
        this.name = name;
        this.color = DEFAULT_COLOR;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.sortOrder = 0;
        this.isArchived = false;
    }
    
    /**
     * 完整构造函数
     */
    public Project(long id, String name, String description, String color, String icon,
                   long createdAt, long updatedAt, int sortOrder, boolean isArchived) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.icon = icon;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sortOrder = sortOrder;
        this.isArchived = isArchived;
    }
    
    // ==================== Getter 和 Setter ====================
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
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
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isArchived() {
        return isArchived;
    }
    
    public void setArchived(boolean archived) {
        isArchived = archived;
        this.updatedAt = System.currentTimeMillis();
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查项目是否有图标
     */
    public boolean hasIcon() {
        return icon != null && !icon.isEmpty();
    }
    
    /**
     * 检查项目是否已归档
     */
    public boolean isInactive() {
        return isArchived;
    }
    
    /**
     * 获取项目的 Emoji 显示（如果有图标）
     */
    public String getIconDisplay() {
        return hasIcon() ? icon : "📁"; // 默认文件夹图标
    }
    
    // ==================== Object 方法重写 ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Project project = (Project) obj;
        return id == project.id;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    
    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", icon='" + getIconDisplay() + '\'' +
                ", createdAt=" + new Date(createdAt) +
                '}';
    }
}