package com.stupidbeauty.joyman.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.stupidbeauty.joyman.data.database.AppDatabase;
import com.stupidbeauty.joyman.data.database.dao.TaskDao;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.util.IdGenerator;
import com.stupidbeauty.joyman.util.LogUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Task 数据仓库
 * 
 * @author 太极美术工程狮狮长
 * @version 2.0.3
 * @since 2026-03-31
 */
public class TaskRepository {
    
    private static final String TAG = "TaskRepository";
    private static volatile TaskRepository INSTANCE;
    
    private final TaskDao taskDao;
    private final LiveData<List<Task>> allTasksLive;
    private final ExecutorService executorService;
    private final AtomicBoolean isShutdown;
    private final LogUtils logUtils;
    
    private TaskRepository(Application application) {
        logUtils = LogUtils.getInstance();
        logUtils.d(TAG, "Constructor: Creating repository for app: " + application.getPackageName());
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        allTasksLive = taskDao.getAllTasksLive();
        executorService = Executors.newFixedThreadPool(4);
        isShutdown = new AtomicBoolean(false);
        logUtils.d(TAG, "Constructor: Thread pool created with 4 threads");
    }
    
    public static TaskRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (TaskRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TaskRepository(application);
                    logUtils.i(TAG, "getInstance: Created new instance");
                }
            }
        } else {
            logUtils.i(TAG, "getInstance: Returning existing instance");
        }
        return INSTANCE;
    }
    
    public void insert(Task task) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ insert() called after shutdown, ignoring task: " + 
                      (task != null ? task.getTitle() : "null"));
            return;
        }
        
        logUtils.d(TAG, "📝 Submitting insert task for: " + (task != null ? task.getTitle() : "null"));
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    logUtils.w(TAG, "⚠️ Task rejected: executor shutdown during insert");
                    return;
                }
                
                try {
                    if (task.getId() == 0) task.setId(IdGenerator.generateId());
                    taskDao.insert(task);
                    logUtils.d(TAG, "✅ Task inserted successfully: " + task.getTitle());
                } catch (Exception e) {
                    logUtils.e(TAG, "❌ Error inserting task", e);
                }
            });
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to submit insert task - executor may be shutdown", e);
        }
    }
    
    public long createTask(String title) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ createTask() called after shutdown, ignoring: " + title);
            return 0;
        }
        
        logUtils.d(TAG, "🆕 Creating task with title: " + title);
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        insert(task);
        logUtils.i(TAG, "🆕 Created task: " + title + " (ID: " + id + ")");
        return id;
    }
    
    public long createTask(String title, String description) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ createTask() with description called after shutdown, ignoring: " + title);
            return 0;
        }
        
        logUtils.d(TAG, "🆕 Creating task with title: " + title + ", description: " + description);
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        task.setDescription(description);
        insert(task);
        logUtils.i(TAG, "🆕 Created task with details: " + title + " (ID: " + id + ")");
        return id;
    }
    
    public void update(Task task) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ update() called after shutdown, ignoring task: " + 
                      (task != null ? task.getTitle() : "null"));
            return;
        }
        
        logUtils.d(TAG, "📝 Submitting update task for: " + (task != null ? task.getTitle() : "null"));
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    logUtils.w(TAG, "⚠️ Task rejected: executor shutdown during update");
                    return;
                }
                
                try {
                    taskDao.update(task);
                    logUtils.i(TAG, "✅ Task updated successfully: " + (task != null ? task.getTitle() : "null"));
                } catch (Exception e) {
                    logUtils.e(TAG, "❌ Error updating task", e);
                }
            });
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to submit update task - executor may be shutdown", e);
            
            // Fallback: execute on main thread
            logUtils.w(TAG, "🔄 FALLBACK - Executing update on main thread");
            try {
                taskDao.update(task);
                logUtils.i(TAG, "✅ FALLBACK SUCCESS - Task updated on main thread");
            } catch (Exception ex) {
                logUtils.e(TAG, "❌ FALLBACK FAILED - Error updating on main thread", ex);
            }
        }
    }
    
    public void markTaskAsDone(long taskId) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ markTaskAsDone() called after shutdown for ID: " + taskId);
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    logUtils.w(TAG, "⚠️ Task rejected: executor shutdown during markTaskAsDone");
                    return;
                }
                
                try {
                    Task task = taskDao.getTaskById(taskId);
                    if (task != null) {
                        task.setStatus(Task.STATUS_DONE);
                        taskDao.update(task);
                        logUtils.i(TAG, "✅ Task marked as done: " + taskId);
                    } else {
                        logUtils.w(TAG, "⚠️ Task not found: " + taskId);
                    }
                } catch (Exception e) {
                    logUtils.e(TAG, "❌ Error in markTaskAsDone", e);
                }
            });
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to submit markTaskAsDone task", e);
        }
    }
    
    public void markTaskAsTodo(long taskId) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ markTaskAsTodo() called after shutdown for ID: " + taskId);
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    logUtils.w(TAG, "⚠️ Task rejected: executor shutdown during markTaskAsTodo");
                    return;
                }
                
                try {
                    Task task = taskDao.getTaskById(taskId);
                    if (task != null) {
                        task.setStatus(Task.STATUS_TODO);
                        taskDao.update(task);
                        logUtils.i(TAG, "✅ Task marked as todo: " + taskId);
                    } else {
                        logUtils.w(TAG, "⚠️ Task not found: " + taskId);
                    }
                } catch (Exception e) {
                    logUtils.e(TAG, "❌ Error in markTaskAsTodo", e);
                }
            });
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to submit markTaskAsTodo task", e);
        }
    }
    
    public void setTaskPriority(long taskId, int priority) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ setTaskPriority() called after shutdown for ID: " + taskId);
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    logUtils.w(TAG, "⚠️ Task rejected: executor shutdown during setTaskPriority");
                    return;
                }
                
                try {
                    Task task = taskDao.getTaskById(taskId);
                    if (task != null) {
                        task.setPriority(priority);
                        taskDao.update(task);
                        logUtils.i(TAG, "✅ Priority updated for task: " + taskId);
                    } else {
                        logUtils.w(TAG, "⚠️ Task not found: " + taskId);
                    }
                } catch (Exception e) {
                    logUtils.e(TAG, "❌ Error in setTaskPriority", e);
                }
            });
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to submit setTaskPriority task", e);
        }
    }
    
    public void delete(Task task) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ delete() called after shutdown for task: " + 
                      (task != null ? task.getTitle() : "null"));
            return;
        }
        
        logUtils.d(TAG, "🗑️ Submitting delete task for: " + (task != null ? task.getTitle() : "null"));
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    logUtils.w(TAG, "⚠️ Task rejected: executor shutdown during delete");
                    return;
                }
                
                try {
                    taskDao.delete(task);
                    logUtils.i(TAG, "✅ Task deleted: " + (task != null ? task.getTitle() : "null"));
                } catch (Exception e) {
                    logUtils.e(TAG, "❌ Error deleting task", e);
                }
            });
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to submit delete task", e);
        }
    }
    
    public void deleteById(long taskId) {
        if (isShutdown.get()) {
            logUtils.w(TAG, "⚠️ deleteById() called after shutdown for ID: " + taskId);
            return;
        }
        
        logUtils.d(TAG, "🗑️ Submitting deleteById task for ID: " + taskId);
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    logUtils.w(TAG, "⚠️ Task rejected: executor shutdown during deleteById");
                    return;
                }
                
                try {
                    taskDao.deleteById(taskId);
                    logUtils.i(TAG, "✅ Task deleted by ID: " + taskId);
                } catch (Exception e) {
                    logUtils.e(TAG, "❌ Error deleting task by ID", e);
                }
            });
        } catch (Exception e) {
            logUtils.e(TAG, "❌ Failed to submit deleteById task", e);
        }
    }
    
    public LiveData<List<Task>> getAllTasksLive() { 
        logUtils.d(TAG, "📋 Returning all tasks live data");
        return allTasksLive; 
    }
    
    public Task getTaskById(long taskId) { 
        logUtils.d(TAG, "🔍 Getting task by ID: " + taskId);
        return taskDao.getTaskById(taskId); 
    }
    
    public LiveData<Task> getTaskByIdLive(long taskId) { 
        logUtils.d(TAG, "🔍 Getting live data for task ID: " + taskId);
        return taskDao.getTaskByIdLive(taskId); 
    }
    
    public LiveData<List<Task>> getTasksByStatusLive(int status) { 
        logUtils.d(TAG, "📋 Getting tasks by status: " + status);
        return taskDao.getTasksByStatusLive(status); 
    }
    
    public LiveData<List<Task>> getIncompleteTasksLive() { 
        logUtils.d(TAG, "📋 Returning incomplete tasks live data");
        return taskDao.getIncompleteTasksLive(); 
    }
    
    public LiveData<List<Task>> searchTasksLive(String keyword) { 
        logUtils.d(TAG, "🔍 Searching tasks with keyword: " + keyword);
        return taskDao.searchTasksLive(keyword); 
    }
    
    public LiveData<List<Task>> getTasksByProjectLive(long projectId) { 
        logUtils.d(TAG, "📋 Getting tasks for project ID: " + projectId);
        return taskDao.getTasksByProject(projectId); 
    }
    
    public int getTaskCount() { 
        logUtils.d(TAG, "📊 Getting total task count");
        return taskDao.getTaskCount(); 
    }
    
    public void shutdown() { 
        logUtils.i(TAG, "🛑 START - Shutting down repository");
        logUtils.i(TAG, "Thread pool state before shutdown: terminated=" + executorService.isTerminated() + 
                  ", shutdown=" + executorService.isShutdown());
        
        isShutdown.set(true);
        
        try {
            executorService.shutdown();
            logUtils.i(TAG, "Shutdown initiated successfully");
            
            boolean terminated = executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
            logUtils.i(TAG, "Await termination result: " + terminated);
            logUtils.i(TAG, "Final state: terminated=" + executorService.isTerminated() + 
                      ", shutdown=" + executorService.isShutdown());
        } catch (InterruptedException e) {
            logUtils.e(TAG, "Interrupted during shutdown", e);
            executorService.shutdownNow();
            logUtils.w(TAG, "Forced shutdown completed");
        } catch (Exception e) {
            logUtils.e(TAG, "Unexpected error during shutdown", e);
        }
        
        logUtils.i(TAG, "✅ END - Repository shutdown complete");
    }
}