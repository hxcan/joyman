package com.stupidbeauty.joyman.repository;

import android.app.Application;
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
 * @version 2.0.0
 * @since 2026-03-31
 */
public class TaskRepository {
    
    private static volatile TaskRepository INSTANCE;
    
    private final TaskDao taskDao;
    private final LiveData<List<Task>> allTasksLive;
    private final LiveData<List<Task>> incompleteTasksLive;
    private final ExecutorService executorService;
    
    private TaskRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        allTasksLive = taskDao.getAllTasksLive();
        incompleteTasksLive = taskDao.getIncompleteTasksLive();
        executorService = Executors.newFixedThreadPool(4);
    }
    
    public static TaskRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (TaskRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TaskRepository(application);
                }
            }
        }
        return INSTANCE;
    }
    
    public void insert(Task task) {
        executorService.execute(() -> {
            if (task.getId() == 0) task.setId(IdGenerator.generateId());
            taskDao.insert(task);
        });
    }
    
    public void insertAll(List<Task> tasks) {
        executorService.execute(() -> {
            for (Task task : tasks) {
                if (task.getId() == 0) task.setId(IdGenerator.generateId());
            }
            taskDao.insertAll(tasks);
        });
    }
    
    public long createTask(String title) {
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        insert(task);
        return id;
    }
    
    public long createTask(String title, String description) {
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        task.setDescription(description);
        insert(task);
        return id;
    }
    
    public void update(Task task) { executorService.execute(() -> taskDao.update(task)); }
    public void updateAll(List<Task> tasks) { executorService.execute(() -> taskDao.updateAll(tasks)); }
    
    public void markTaskAsDone(long taskId) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null) {
                task.setStatus(Task.STATUS_DONE);
                taskDao.update(task);
            }
        });
    }
    
    public void markTaskAsTodo(long taskId) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null) {
                task.setStatus(Task.STATUS_TODO);
                taskDao.update(task);
            }
        });
    }
    
    public void setTaskPriority(long taskId, int priority) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null) {
                task.setPriority(priority);
                taskDao.update(task);
            }
        });
    }
    
    public void delete(Task task) { executorService.execute(() -> taskDao.delete(task)); }
    public void deleteById(long taskId) { executorService.execute(() -> taskDao.deleteById(taskId)); }
    public void deleteAll(List<Task> tasks) { executorService.execute(() -> taskDao.deleteAll(tasks)); }
    
    public LiveData<List<Task>> getAllTasksLive() { return allTasksLive; }
    public List<Task> getAllTasks() { return taskDao.getAllTasks(); }
    public Task getTaskById(long taskId) { return taskDao.getTaskById(taskId); }
    public LiveData<Task> getTaskByIdLive(long taskId) { return taskDao.getTaskByIdLive(taskId); }
    public LiveData<List<Task>> getTasksByStatusLive(int status) { return taskDao.getTasksByStatusLive(status); }
    public LiveData<List<Task>> getIncompleteTasksLive() { return incompleteTasksLive; }
    public List<Task> getOverdueTasks() { return taskDao.getOverdueTasks(System.currentTimeMillis()); }
    public LiveData<List<Task>> searchTasksLive(String keyword) { return taskDao.searchTasksLive(keyword); }
    
    /**
     * 按项目查询任务
     */
    public LiveData<List<Task>> getTasksByProjectLive(long projectId) {
        return taskDao.getTasksByProject(projectId);
    }
    
    public int getTaskCount() { return taskDao.getTaskCount(); }
    public int getIncompleteTaskCount() { return taskDao.getIncompleteTaskCount(); }
    public int getOverdueTaskCount() { return taskDao.getOverdueTaskCount(System.currentTimeMillis()); }
    
    public void shutdown() { executorService.shutdown(); }
}