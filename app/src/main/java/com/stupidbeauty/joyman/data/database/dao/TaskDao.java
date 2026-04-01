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

@Dao
public interface TaskDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Task> tasks);
    
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
}