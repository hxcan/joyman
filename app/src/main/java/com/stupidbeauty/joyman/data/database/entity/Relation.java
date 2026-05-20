package com.stupidbeauty.joyman.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import androidx.room.ForeignKey;



/**
 * Relation 任务关系实体类
 * 
 * 对应数据库表：relations
 * 用于存储任务之间的阻塞关系（blocking/blocked_by）
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-05-19
 */
@Entity(
    tableName = "relations",
    indices = {
        @Index(value = {"issue_id"}),
        @Index(value = {"related_issue_id"})
    },
    foreignKeys = {
        @ForeignKey(
            entity = Task.class,
            parentColumns = {"id"},
            childColumns = {"issue_id"},
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        @ForeignKey(
            entity = Task.class,
            parentColumns = {"id"},
            childColumns = {"related_issue_id"},
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    }
)
public class Relation {
    
    /**
     * 关系类型：阻塞其他任务
     */
    public static final String TYPE_BLOCKS = "blocks";
    
    /**
     * 关系类型：被其他任务阻塞
     */
    public static final String TYPE_BLOCKED_BY = "blocked_by";
    
    /**
     * 主键 ID（自增长）
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    /**
     * 主任务 ID
     */
    @ColumnInfo(name = "issue_id")
    private long issueId;
    
    /**
     * 关联任务 ID
     */
    @ColumnInfo(name = "related_issue_id")
    private long relatedIssueId;
    
    /**
     * 关系类型
     * - "blocks": 主任务阻塞关联任务
     * - "blocked_by": 主任务被关联任务阻塞
     */
    @ColumnInfo(name = "type")
    private String type;
    
    /**
     * 创建时间（毫秒级时间戳）
     */
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    // ==================== 构造函数 ====================
    
    public Relation() {
    }
    
    public Relation(long issueId, long relatedIssueId, String type) {
        this.issueId = issueId;
        this.relatedIssueId = relatedIssueId;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }
    
    public Relation(long id, long issueId, long relatedIssueId, String type, long createdAt) {
        this.id = id;
        this.issueId = issueId;
        this.relatedIssueId = relatedIssueId;
        this.type = type;
        this.createdAt = createdAt;
    }
    
    // ==================== Getter 和 Setter ====================
    
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getIssueId() { return issueId; }
    public void setIssueId(long issueId) { this.issueId = issueId; }
    
    public long getRelatedIssueId() { return relatedIssueId; }
    public void setRelatedIssueId(long relatedIssueId) { this.relatedIssueId = relatedIssueId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 判断是否为阻塞关系（blocks）
     */
    public boolean isBlocks() {
        return TYPE_BLOCKS.equals(type);
    }
    
    /**
     * 判断是否被阻塞关系（blocked_by）
     */
    public boolean isBlockedBy() {
        return TYPE_BLOCKED_BY.equals(type);
    }
    
    /**
     * 获取关系的反向类型
     * @return 如果当前是 blocks，返回 blocked_by；反之亦然
     */
    public String getReverseType() {
        if (TYPE_BLOCKS.equals(type)) {
            return TYPE_BLOCKED_BY;
        } else if (TYPE_BLOCKED_BY.equals(type)) {
            return TYPE_BLOCKS;
        }
        return type;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Relation relation = (Relation) obj;
        return id == relation.id;
    }
    
    @Override
    public int hashCode() { return Long.hashCode(id); }
    
    @Override
    public String toString() {
        return "Relation{" +
                "id=" + id +
                ", issueId=" + issueId +
                ", relatedIssueId=" + relatedIssueId +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}