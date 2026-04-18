package com.stupidbeauty.joyman.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.stupidbeauty.joyman.data.database.entity.Comment;

import java.util.List;

/**
 * Comment 数据访问对象（DAO）
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.1
 * @since 2026-04-18
 */
@Dao
public interface CommentDao {
    
    /**
     * 插入单条评论
     * @param comment 评论对象
     * @return 插入的行 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Comment comment);
    
    /**
     * 批量插入评论
     * @param comments 评论列表
     * @return 插入的行 ID 列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Comment> comments);
    
    /**
     * 更新评论
     * @param comment 评论对象
     * @return 更新的行数
     */
    @Update
    int update(Comment comment);
    
    /**
     * 删除评论
     * @param comment 评论对象
     * @return 删除的行数
     */
    @Delete
    int delete(Comment comment);
    
    /**
     * 根据 ID 删除评论
     * @param commentId 评论 ID
     * @return 删除的行数
     */
    @Query("DELETE FROM comments WHERE id = :commentId")
    int deleteById(long commentId);
    
    /**
     * 根据任务 ID 获取所有评论（正序排列，时间早的在上，最新的在下）
     * @param issueId 任务 ID
     * @return 评论列表
     */
    @Query("SELECT * FROM comments WHERE issue_id = :issueId ORDER BY created_on ASC")
    List<Comment> getCommentsByIssueId(long issueId);
    
    /**
     * 根据任务 ID 获取所有评论（LiveData 响应式，正序排列）
     * @param issueId 任务 ID
     * @return 可观察的评论列表
     */
    @Query("SELECT * FROM comments WHERE issue_id = :issueId ORDER BY created_on ASC")
    LiveData<List<Comment>> getCommentsByIssueIdLive(long issueId);
    
    /**
     * 根据 ID 获取评论
     * @param commentId 评论 ID
     * @return 评论对象
     */
    @Query("SELECT * FROM comments WHERE id = :commentId")
    Comment getCommentById(long commentId);
    
    /**
     * 统计任务的评论数量
     * @param issueId 任务 ID
     * @return 评论数量
     */
    @Query("SELECT COUNT(*) FROM comments WHERE issue_id = :issueId")
    int countCommentsByIssueId(long issueId);
    
    /**
     * 删除任务的所有评论（级联删除时使用）
     * @param issueId 任务 ID
     * @return 删除的行数
     */
    @Query("DELETE FROM comments WHERE issue_id = :issueId")
    int deleteAllByIssueId(long issueId);
}