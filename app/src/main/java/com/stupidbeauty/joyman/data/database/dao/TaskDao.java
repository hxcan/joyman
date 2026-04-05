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
 * @version 2.0.2
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
    
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    LiveData<List<Task>> getAllTasksLive();
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskById(long taskId);
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<Task> getTaskByIdLive(long taskId);
    
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority DESC, created_at DESC")
    LiveData<List<Task>> getTasksByStatusLive(int status);
    
    @Query("SELECT * FROM tasks WHERE status IN (0, 1) ORDER BY priority DESC, due_date ASC, created_at DESC")
    LiveData<List<Task>> getIncompleteTasksLive();
    
    @Query("SELECT * FROM tasks WHERE project_id = :projectId ORDER BY priority DESC, created_at DESC")
    LiveData<List<Task>> getTasksByProject(long projectId);
    
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    LiveData<List<Task>> searchTasksLive(String keyword);
    
    @Query("SELECT COUNT(*) FROM tasks")
    int getTaskCount();
    
    /**
     * 获取指定父任务的所有子任务
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