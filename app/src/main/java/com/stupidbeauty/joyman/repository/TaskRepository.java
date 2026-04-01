package com.stupidbeauty.joyman.repository;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;

import com.stupidbeauty.joyman.data.database.AppDatabase;
import com.stupidbeauty.joyman.data.database.dao.TaskDao;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.util.IdGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Task 数据仓库
 * 
 * @author 太极美术工程狮狮长
 * @version 2.0.2
 * @since 2026-03-31
 */
public class TaskRepository {
    
    private static final String TAG = "TaskRepository";
    private static volatile TaskRepository INSTANCE;
    
    private final TaskDao taskDao;
    private final LiveData<List<Task>> allTasksLive;
    private final ExecutorService executorService;
    
    private TaskRepository(Application application) {
        Log.d(TAG, "Constructor: Creating repository for app: " + application.getPackageName());
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        allTasksLive = taskDao.getAllTasksLive();
        executorService = Executors.newFixedThreadPool(4);
        Log.d(TAG, "Constructor: Thread pool created with 4 threads: " + executorService.toString());
    }
    
    public static TaskRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (TaskRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TaskRepository(application);
                    Log.i(TAG, "getInstance: Created new instance");
                }
            }
        } else {
            Log.i(TAG, "getInstance: Returning existing instance");
        }
        return INSTANCE;
    }
    
    public void insert(Task task) {
        Log.d(TAG, "insert: Starting for task ID: " + task.getId() + ", title: " + task.getTitle());
        Log.d(TAG, "insert: Thread pool state before: " + executorService.toString());
        
        executorService.execute(() -> {
            try {
                if (task.getId() == 0) task.setId(IdGenerator.generateId());
                taskDao.insert(task);
                Log.d(TAG, "insert: Task inserted successfully");
            } catch (Exception e) {
                Log.e(TAG, "insert: Error inserting task", e);
            }
        });
    }
    
    public long createTask(String title) {
        Log.d(TAG, "createTask: Creating task with title: " + title);
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        insert(task);
        return id;
    }
    
    public long createTask(String title, String description) {
        Log.d(TAG, "createTask: Creating task with title: " + title + ", description: " + description);
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        task.setDescription(description);
        insert(task);
        return id;
    }
    
    public void update(Task task) {
        Log.d(TAG, "=================================================================");
        Log.d(TAG, "update: START - Updating task ID: " + task.getId() + ", title: " + task.getTitle());
        Log.d(TAG, "update: Thread pool state: " + executorService.toString());
        Log.d(TAG, "update: isTerminated: " + executorService.isTerminated());
        Log.d(TAG, "update: isShutdown: " + executorService.isShutdown());
        
        try {
            executorService.execute(() -> {
                try {
                    Log.d(TAG, "update: Executing in thread: " + Thread.currentThread().getName());
                    Log.d(TAG, "update: Inside runnable - Thread pool state: " + executorService.toString());
                    taskDao.update(task);
                    Log.i(TAG, "update: Task updated successfully in database");
                } catch (Exception e) {
                    Log.e(TAG, "update: Error inside runnable", e);
                }
            });
            Log.d(TAG, "update: execute() called successfully");
        } catch (Exception e) {
            Log.e(TAG, "=================================================================");
            Log.e(TAG, "update: EXCEPTION CAUGHT!");
            Log.e(TAG, "update: Exception type: " + e.getClass().getSimpleName());
            Log.e(TAG, "update: Exception message: " + e.getMessage());
            Log.e(TAG, "update: Thread pool is terminated: " + executorService.isTerminated());
            Log.e(TAG, "update: Thread pool is shutdown: " + executorService.isShutdown());
            Log.e(TAG, "update: Stack trace:", e);
            Log.e(TAG, "=================================================================");
            
            // 降级方案：在主线程直接执行
            Log.w(TAG, "update: FALLBACK - Executing update on main thread due to thread pool termination");
            try {
                taskDao.update(task);
                Log.i(TAG, "update: FALLBACK SUCCESS - Task updated on main thread");
            } catch (Exception ex) {
                Log.e(TAG, "update: FALLBACK FAILED - Error updating on main thread", ex);
            }
        }
        
        Log.d(TAG, "update: END");
        Log.d(TAG, "=================================================================");
    }
    
    public void markTaskAsDone(long taskId) {
        Log.d(TAG, "markTaskAsDone: Starting for task ID: " + taskId);
        executorService.execute(() -> {
            try {
                Task task = taskDao.getTaskById(taskId);
                if (task != null) {
                    task.setStatus(Task.STATUS_DONE);
                    taskDao.update(task);
                    Log.i(TAG, "markTaskAsDone: Task marked as done");
                } else {
                    Log.w(TAG, "markTaskAsDone: Task not found");
                }
            } catch (Exception e) {
                Log.e(TAG, "markTaskAsDone: Error", e);
            }
        });
    }
    
    public void markTaskAsTodo(long taskId) {
        Log.d(TAG, "markTaskAsTodo: Starting for task ID: " + taskId);
        executorService.execute(() -> {
            try {
                Task task = taskDao.getTaskById(taskId);
                if (task != null) {
                    task.setStatus(Task.STATUS_TODO);
                    taskDao.update(task);
                    Log.i(TAG, "markTaskAsTodo: Task marked as todo");
                } else {
                    Log.w(TAG, "markTaskAsTodo: Task not found");
                }
            } catch (Exception e) {
                Log.e(TAG, "markTaskAsTodo: Error", e);
            }
        });
    }
    
    public void setTaskPriority(long taskId, int priority) {
        Log.d(TAG, "setTaskPriority: Starting for task ID: " + taskId + ", priority: " + priority);
        executorService.execute(() -> {
            try {
                Task task = taskDao.getTaskById(taskId);
                if (task != null) {
                    task.setPriority(priority);
                    taskDao.update(task);
                    Log.i(TAG, "setTaskPriority: Priority updated");
                } else {
                    Log.w(TAG, "setTaskPriority: Task not found");
                }
            } catch (Exception e) {
                Log.e(TAG, "setTaskPriority: Error", e);
            }
        });
    }
    
    public void delete(Task task) {
        Log.d(TAG, "delete: Deleting task ID: " + task.getId());
        executorService.execute(() -> {
            try {
                taskDao.delete(task);
                Log.i(TAG, "delete: Task deleted");
            } catch (Exception e) {
                Log.e(TAG, "delete: Error", e);
            }
        });
    }
    
    public void deleteById(long taskId) {
        Log.d(TAG, "deleteById: Deleting task ID: " + taskId);
        executorService.execute(() -> {
            try {
                taskDao.deleteById(taskId);
                Log.i(TAG, "deleteById: Task deleted by ID");
            } catch (Exception e) {
                Log.e(TAG, "deleteById: Error", e);
            }
        });
    }
    
    public LiveData<List<Task>> getAllTasksLive() { 
        Log.d(TAG, "getAllTasksLive: Returning live data");
        return allTasksLive; 
    }
    
    public Task getTaskById(long taskId) { 
        Log.d(TAG, "getTaskById: Getting task ID: " + taskId);
        return taskDao.getTaskById(taskId); 
    }
    
    public LiveData<Task> getTaskByIdLive(long taskId) { 
        Log.d(TAG, "getTaskByIdLive: Getting live data for task ID: " + taskId);
        return taskDao.getTaskByIdLive(taskId); 
    }
    
    public LiveData<List<Task>> getTasksByStatusLive(int status) { 
        Log.d(TAG, "getTasksByStatusLive: Getting tasks with status: " + status);
        return taskDao.getTasksByStatusLive(status); 
    }
    
    public LiveData<List<Task>> getIncompleteTasksLive() { 
        Log.d(TAG, "getIncompleteTasksLive: Returning incomplete tasks");
        return taskDao.getIncompleteTasksLive(); 
    }
    
    public LiveData<List<Task>> searchTasksLive(String keyword) { 
        Log.d(TAG, "searchTasksLive: Searching with keyword: " + keyword);
        return taskDao.searchTasksLive(keyword); 
    }
    
    public LiveData<List<Task>> getTasksByProjectLive(long projectId) { 
        Log.d(TAG, "getTasksByProjectLive: Getting tasks for project ID: " + projectId);
        return taskDao.getTasksByProject(projectId); 
    }
    
    public int getTaskCount() { 
        Log.d(TAG, "getTaskCount: Getting total count");
        return taskDao.getTaskCount(); 
    }
    
    public void shutdown() { 
        Log.i(TAG, "=================================================================");
        Log.i(TAG, "shutdown: START - Shutting down repository");
        Log.i(TAG, "shutdown: Thread pool state before shutdown: " + executorService.toString());
        
        try {
            executorService.shutdown();
            Log.i(TAG, "shutdown: Shutdown initiated successfully");
            
            // 等待一段时间让任务完成
            boolean terminated = executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
            Log.i(TAG, "shutdown: Await termination result: " + terminated);
            Log.i(TAG, "shutdown: Thread pool state after shutdown: " + executorService.toString());
            Log.i(TAG, "shutdown: isTerminated: " + executorService.isTerminated());
            Log.i(TAG, "shutdown: isShutdown: " + executorService.isShutdown());
        } catch (InterruptedException e) {
            Log.e(TAG, "shutdown: Interrupted during shutdown", e);
            executorService.shutdownNow();
            Log.w(TAG, "shutdown: Forced shutdown completed");
        } catch (Exception e) {
            Log.e(TAG, "shutdown: Unexpected error during shutdown", e);
        }
        
        Log.i(TAG, "shutdown: END");
        Log.i(TAG, "=================================================================");
    }
}