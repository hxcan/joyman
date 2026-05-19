package com.stupidbeauty.joyman.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.stupidbeauty.joyman.data.database.entity.Relation;

import java.util.List;


/**
 * Relation 数据访问对象（DAO）
 * 
 * 用于管理任务之间的阻塞关系
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-05-19
 */
@Dao
public interface RelationDao {
    
    /**
     * 插入新关系
     * @param relation 关系对象
     * @return 插入的关系 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Relation relation);
    
    /**
     * 根据 ID 删除关系
     * @param relationId 关系 ID
     * @return 删除的行数（1 表示成功，0 表示未找到）
     */
    @Query("DELETE FROM relations WHERE id = :relationId")
    int deleteById(long relationId);
    
    /**
     * 获取指定任务的所有关系（同步版本）
     * 用于 JoyManApiService 的 GET /issues/{id}/relations.json 端点
     * 
     * @param issueId 任务 ID
     * @return 关系列表
     */
    @Query("SELECT * FROM relations WHERE issue_id = :issueId ORDER BY created_at ASC")
    List<Relation> getRelationsByIssueId(long issueId);
    
    /**
     * 判断任务是否有关系
     * @param issueId 任务 ID
     * @return true 如果存在关系
     */
    @Query("SELECT EXISTS(SELECT 1 FROM relations WHERE issue_id = :issueId LIMIT 1)")
    boolean hasRelations(long issueId);
    
    /**
     * 统计任务的关系数量
     * @param issueId 任务 ID
     * @return 关系数量
     */
    @Query("SELECT COUNT(*) FROM relations WHERE issue_id = :issueId")
    int countRelations(long issueId);
    
    /**
     * 删除任务的所有关系
     * @param issueId 任务 ID
     * @return 删除的行数
     */
    @Query("DELETE FROM relations WHERE issue_id = :issueId")
    int deleteAllByIssueId(long issueId);
}