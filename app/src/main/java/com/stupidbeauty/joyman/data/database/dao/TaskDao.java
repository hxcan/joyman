package com.stupidbeauty.joyman.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.stupidbeauty.joyman.data.database.entity.Task;

import java.util.List;

/**
 * Task 数据访问对象（DAO）
 * 
 * 定义对 tasks 表的所有数据库操作接口
 * 由 Room 在编译时自动生成实现类
 * 
 * 功能包括：
 * - 插入任务（支持批量插入）
 * - 更新任务
 * - 删除任务
 * - 查询任务（支持多种条件）
 * - 统计任务数量
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-03-31
 */
@Dao
public interface TaskDao {
    
    // ==================== 插入操作 ====================
    
    /**
     * 插入单个任务
     * 
     * @param task 要插入的任务对象
     * @return 插入的行 ID（如果冲突则返回 -1）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);
    
    /**
     * 批量插入任务
     * 
     * @param tasks 要插入的任务列表
     * @return 插入的行 ID 列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Task> tasks);
    
    /**
     * 插入或忽略（如果 ID 已存在则跳过）
     * 
     * @param task 要插入的任务对象
     * @return 插入的行 ID（如果忽略则返回 -1）
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertOrIgnore(Task task);
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新单个任务
     * 
     * @param task 要更新的任务对象（必须包含 id）
     * @return 更新的行数（通常为 1）
     */
    @Update
    int update(Task task);
    
    /**
     * 批量更新任务
     * 
     * @param tasks 要更新的任务列表
     * @return 更新的行数
     */
    @Update
    int updateAll(List<Task> tasks);
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除单个任务
     * 
     * @param task 要删除的任务对象
     * @return 删除的行数（通常为 1）
     */
    @Delete
    int delete(Task task);
    
    /**
     * 根据 ID 删除任务
     * 
     * @param taskId 任务 ID
     * @return 删除的行数（0 或 1）
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    int deleteById(long taskId);
    
    /**
     * 批量删除任务
     * 
     * @param tasks 要删除的任务列表
     * @return 删除的行数
     */
    @Delete
    int deleteAll(List<Task> tasks);
    
    /**
     * 删除所有任务（慎用！）
     * 
     * @return 删除的行数
     */
    @Query("DELETE FROM tasks")
    int deleteAllTasks();
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有任务（按创建时间倒序）
     * 
     * @return 任务列表
     */
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    List<Task> getAllTasks();
    
    /**
     * 获取所有任务（LiveData 响应式）
     * 
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    LiveData<List<Task>> getAllTasksLive();
    
    /**
     * 根据 ID 获取任务
     * 
     * @param taskId 任务 ID
     * @return 任务对象，不存在则返回 null
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskById(long taskId);
    
    /**
     * 根据 ID 获取任务（LiveData 响应式）
     * 
     * @param taskId 任务 ID
     * @return 可观察的任务对象
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<Task> getTaskByIdLive(long taskId);
    
    /**
     * 根据状态获取任务
     * 
     * @param status 任务状态（STATUS_TODO, STATUS_IN_PROGRESS, etc.）
     * @return 任务列表
     */
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC, created_at DESC")
    List<Task> getTasksByStatus(int status);
    
    /**
     * 根据状态获取任务（LiveData 响应式）
     * 
     * @param status 任务状态
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC, created_at DESC")
    LiveData<List<Task>> getTasksByStatusLive(int status);
    
    /**
     * 根据优先级获取任务
     * 
     * @param priority 优先级（PRIORITY_LOW, PRIORITY_NORMAL, etc.）
     * @return 任务列表
     */
    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY created_at DESC")
    List<Task> getTasksByPriority(int priority);
    
    /**
     * 获取未完成的任务（待办 + 进行中）
     * 
     * @return 任务列表
     */
    @Query("SELECT * FROM tasks WHERE status IN (0, 1) ORDER BY priority DESC, due_date ASC, created_at DESC")
    List<Task> getIncompleteTasks();
    
    /**
     * 获取未完成的任务（LiveData 响应式）
     * 
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks WHERE status IN (0, 1) ORDER BY priority DESC, due_date ASC, created_at DESC")
    LiveData<List<Task>> getIncompleteTasksLive();
    
    /**
     * 获取已过期的任务
     * 
     * @param currentTime 当前时间戳（毫秒）
     * @return 任务列表
     */
    @Query("SELECT * FROM tasks WHERE due_date IS NOT NULL AND due_date < :currentTime AND status NOT IN (2, 3) ORDER BY due_date ASC")
    List<Task> getOverdueTasks(long currentTime);
    
    /**
     * 获取今日到期的任务
     * 
     * @param startOfDay 今日开始时间戳（毫秒）
     * @param endOfDay 今日结束时间戳（毫秒）
     * @return 任务列表
     */
    @Query("SELECT * FROM tasks WHERE due_date IS NOT NULL AND due_date >= :startOfDay AND due_date <= :endOfDay ORDER BY priority DESC")
    List<Task> getTasksDueToday(long startOfDay, long endOfDay);
    
    /**
     * 搜索任务（标题或描述包含关键词）
     * 
     * @param keyword 搜索关键词
     * @return 任务列表
     */
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    List<Task> searchTasks(String keyword);
    
    /**
     * 搜索任务（LiveData 响应式）
     * 
     * @param keyword 搜索关键词
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    LiveData<List<Task>> searchTasksLive(String keyword);
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取任务总数
     * 
     * @return 任务数量
     */
    @Query("SELECT COUNT(*) FROM tasks")
    int getTaskCount();
    
    /**
     * 根据状态统计任务数量
     * 
     * @param status 任务状态
     * @return 任务数量
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE status = :status")
    int getTaskCountByStatus(int status);
    
    /**
     * 获取未完成任务数量
     * 
     * @return 未完成任务数量
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE status IN (0, 1)")
    int getIncompleteTaskCount();
    
    /**
     * 获取过期任务数量
     * 
     * @param currentTime 当前时间戳（毫秒）
     * @return 过期任务数量
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE due_date IS NOT NULL AND due_date < :currentTime AND status NOT IN (2, 3)")
    int getOverdueTaskCount(long currentTime);
}