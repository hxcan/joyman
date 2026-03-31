package com.stupidbeauty.joyman.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.stupidbeauty.joyman.data.database.entity.Project;

import java.util.List;

/**
 * Project 数据访问对象（DAO）
 * 
 * 定义对 projects 表的所有数据库操作接口
 * 由 Room 在编译时自动生成实现类
 * 
 * 功能包括：
 * - 插入项目（支持批量插入）
 * - 更新项目
 * - 删除项目
 * - 查询项目（支持多种条件）
 * - 统计项目数量
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-03-31
 */
@Dao
public interface ProjectDao {
    
    // ==================== 插入操作 ====================
    
    /**
     * 插入单个项目
     * 
     * @param project 要插入的项目对象
     * @return 插入的行 ID（如果冲突则返回 -1）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Project project);
    
    /**
     * 批量插入项目
     * 
     * @param projects 要插入的项目列表
     * @return 插入的行 ID 列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Project> projects);
    
    /**
     * 插入或忽略（如果 ID 已存在则跳过）
     * 
     * @param project 要插入的项目对象
     * @return 插入的行 ID（如果忽略则返回 -1）
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertOrIgnore(Project project);
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新单个项目
     * 
     * @param project 要更新的项目对象（必须包含 id）
     * @return 更新的行数（通常为 1）
     */
    @Update
    int update(Project project);
    
    /**
     * 批量更新项目
     * 
     * @param projects 要更新的项目列表
     * @return 更新的行数
     */
    @Update
    int updateAll(List<Project> projects);
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除单个项目
     * 
     * @param project 要删除的项目对象
     * @return 删除的行数（通常为 1）
     */
    @Delete
    int delete(Project project);
    
    /**
     * 根据 ID 删除项目
     * 
     * @param projectId 项目 ID
     * @return 删除的行数（0 或 1）
     */
    @Query("DELETE FROM projects WHERE id = :projectId")
    int deleteById(long projectId);
    
    /**
     * 批量删除项目
     * 
     * @param projects 要删除的项目列表
     * @return 删除的行数
     */
    @Delete
    int deleteAll(List<Project> projects);
    
    /**
     * 删除所有项目（慎用！）
     * 
     * @return 删除的行数
     */
    @Query("DELETE FROM projects")
    int deleteAllProjects();
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有项目（按排序顺序升序）
     * 
     * @return 项目列表
     */
    @Query("SELECT * FROM projects ORDER BY sort_order ASC, created_at DESC")
    List<Project> getAllProjects();
    
    /**
     * 获取所有项目（LiveData 响应式）
     * 
     * @return 可观察的项目列表
     */
    @Query("SELECT * FROM projects ORDER BY sort_order ASC, created_at DESC")
    LiveData<List<Project>> getAllProjectsLive();
    
    /**
     * 获取未归档的项目（按排序顺序升序）
     * 
     * @return 项目列表
     */
    @Query("SELECT * FROM projects WHERE is_archived = 0 ORDER BY sort_order ASC, created_at DESC")
    List<Project> getActiveProjects();
    
    /**
     * 获取未归档的项目（LiveData 响应式）
     * 
     * @return 可观察的项目列表
     */
    @Query("SELECT * FROM projects WHERE is_archived = 0 ORDER BY sort_order ASC, created_at DESC")
    LiveData<List<Project>> getActiveProjectsLive();
    
    /**
     * 获取已归档的项目
     * 
     * @return 项目列表
     */
    @Query("SELECT * FROM projects WHERE is_archived = 1 ORDER BY updated_at DESC")
    List<Project> getArchivedProjects();
    
    /**
     * 根据 ID 获取项目
     * 
     * @param projectId 项目 ID
     * @return 项目对象，不存在则返回 null
     */
    @Query("SELECT * FROM projects WHERE id = :projectId")
    Project getProjectById(long projectId);
    
    /**
     * 根据 ID 获取项目（LiveData 响应式）
     * 
     * @param projectId 项目 ID
     * @return 可观察的项目对象
     */
    @Query("SELECT * FROM projects WHERE id = :projectId")
    LiveData<Project> getProjectByIdLive(long projectId);
    
    /**
     * 根据名称搜索项目
     * 
     * @param keyword 搜索关键词
     * @return 项目列表
     */
    @Query("SELECT * FROM projects WHERE name LIKE '%' || :keyword || '%' ORDER BY sort_order ASC")
    List<Project> searchProjects(String keyword);
    
    /**
     * 根据名称搜索项目（LiveData 响应式）
     * 
     * @param keyword 搜索关键词
     * @return 可观察的项目列表
     */
    @Query("SELECT * FROM projects WHERE name LIKE '%' || :keyword || '%' ORDER BY sort_order ASC")
    LiveData<List<Project>> searchProjectsLive(String keyword);
    
    /**
     * 获取指定颜色的项目
     * 
     * @param color 颜色值（十六进制格式）
     * @return 项目列表
     */
    @Query("SELECT * FROM projects WHERE color = :color ORDER BY sort_order ASC")
    List<Project> getProjectsByColor(String color);
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取项目总数
     * 
     * @return 项目数量
     */
    @Query("SELECT COUNT(*) FROM projects")
    int getProjectCount();
    
    /**
     * 获取未归档的项目数量
     * 
     * @return 未归档项目数量
     */
    @Query("SELECT COUNT(*) FROM projects WHERE is_archived = 0")
    int getActiveProjectCount();
    
    /**
     * 获取已归档的项目数量
     * 
     * @return 已归档项目数量
     */
    @Query("SELECT COUNT(*) FROM projects WHERE is_archived = 1")
    int getArchivedProjectCount();
}