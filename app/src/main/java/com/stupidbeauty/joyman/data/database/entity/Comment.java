package com.stupidbeauty.joyman.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;

/**
 * 评论实体类
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.1
 * @since 2026-04-18
 */
@Entity(
    tableName = "comments",
    foreignKeys = {
        @ForeignKey(
            entity = Task.class,
            parentColumns = "id",
            childColumns = "issue_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {@Index("issue_id")}
)
public class Comment {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "issue_id")
    private long issueId;           // 关联的任务 ID
    
    @ColumnInfo(name = "content")
    private String content;         // 评论内容
    
    @ColumnInfo(name = "author")
    private String author;          // 评论作者
    
    @ColumnInfo(name = "created_on")
    private long createdOn;         // 创建时间（时间戳）
    
    public Comment() {}
    
    public Comment(long issueId, String content, String author) {
        this.issueId = issueId;
        this.content = content;
        this.author = author;
        this.createdOn = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getIssueId() {
        return issueId;
    }
    
    public void setIssueId(long issueId) {
        this.issueId = issueId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public long getCreatedOn() {
        return createdOn;
    }
    
    public void setCreatedOn(long createdOn) {
        this.createdOn = createdOn;
    }
}