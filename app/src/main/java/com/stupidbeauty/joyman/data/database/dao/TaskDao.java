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
 * @author 太极美术工程狮狮长
 * @version 2.0.4
 * @since 2026-03-31
 */
@Dao
public interface TaskDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);
    
    @Update
    int update(Task task);
    
    @Delete
    int delete(Task task);
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    int deleteById(long taskId);
    
    /**
     * 获取所有任务（LiveData 响应式）
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    LiveData<List<Task>> getAllTasksLive();
    
    /**
     * 获取所有任务（同步，直接返回 List）
     * 用于 API 服务层，避免 LiveData 异步问题
     * @return 任务列表
     */
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    List<Task> getAllTasks();
    
    /**
     * 根据 ID 获取任务（同步）
     * @param taskId 任务 ID
     * @return 任务对象，不存在则返回 null
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskById(long taskId);
    
    /**
     * 根据 ID 获取任务（LiveData 响应式）
     * @param taskId 任务 ID
     * @return 可观察的任务对象
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<Task> getTaskByIdLive(long taskId);
    
    /**
     * 根据状态获取任务（LiveData 响应式）
     * @param status 状态 ID
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC, created_at DESC")
    LiveData<List<Task>> getTasksByStatusLive(int status);
    
    /**
     * 获取未完成任务（LiveData 响应式）
     * @return 可观察的未完成任务列表
     */
    @Query("SELECT * FROM tasks WHERE status IN (0, 1) ORDER BY priority DESC, due_date ASC, created_at DESC")
    LiveData<List<Task>> getIncompleteTasksLive();
    
    /**
     * 根据项目 ID 获取任务（LiveData 响应式）
     * @param projectId 项目 ID
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks WHERE project_id = :projectId ORDER BY priority DESC, created_at DESC")
    LiveData<List<Task>> getTasksByProject(long projectId);
    
    /**
     * 搜索任务（LiveData 响应式）
     * @param keyword 搜索关键词
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    LiveData<List<Task>> searchTasksLive(String keyword);
    
    /**
     * 多关键词搜索任务（LiveData 响应式）
     * 支持空格分隔的多个关键词，所有关键词都必须匹配（AND 逻辑）
     * 搜索范围：任务标题 + 任务描述
     * @param keywords 关键词数组
     * @return 可观察的任务列表
     */
    @Query("SELECT * FROM tasks WHERE (:keywords IS NULL OR :keywords = '' OR " +
           "EXISTS (SELECT 1 FROM (SELECT rtrim(rtrim(replace(' ' || :keywords || ' ', ' ', char(10)), char(10) || ' '), ' ') as word, " +
           "length(' ' || :keywords || ' ') - length(replace(' ' || :keywords || ' ', ' ', '')) + 1 as cnt) " +
           "WHERE word != '' AND LENGTH(word) >= 2 LIMIT 5) AND " +
           "(title LIKE '%' || :keywords || '%' OR (description IS NOT NULL AND description LIKE '%' || :keywords || '%'))) " +
           "ORDER BY created_at DESC")
    LiveData<List<Task>> searchTasksByKeywords(String keywords);
    
    /**
     * 获取任务总数
     * @return 任务数量
     */
    @Query("SELECT COUNT(*) FROM tasks")
    int getTaskCount();
    
    /**
     * 获取指定父任务的所有子任务（LiveData 响应式）
     * @param parentId 父任务 ID
     * @return 子任务列表
     */
    @Query("SELECT * FROM tasks WHERE parent_id = :parentId ORDER BY created_at ASC")
    LiveData<List<Task>> getSubtasksByParentIdLive(long parentId);
    
    /**
     * 获取指定父任务的所有子任务（同步版本）
     * @param parentId 父任务 ID
     * @return 子任务列表
     */
    @Query("SELECT * FROM tasks WHERE parent_id = :parentId ORDER BY created_at ASC")
    List<Task> getSubtasksByParentId(long parentId);
    
    /**
     * 判断任务是否有子任务
     * @param parentId 父任务 ID
     * @return true 如果有子任务
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE parent_id = :parentId LIMIT 1)")
    boolean hasSubtasks(long parentId);
    
    /**
     * 统计子任务数量
     * @param parentId 父任务 ID
     * @return 子任务数量
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE parent_id = :parentId")
    int countSubtasks(long parentId);
}