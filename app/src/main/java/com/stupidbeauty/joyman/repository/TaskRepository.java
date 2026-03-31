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
 * 统一管理 Task 数据的访问和操作
 * 作为 ViewModel 和 DAO 之间的中间层
 * 
 * 职责包括：
 * - 封装数据库操作（通过 TaskDao）
 * - 提供高级业务逻辑方法
 * - 处理异步操作（使用 ExecutorService）
 * - 为未来添加网络同步、数据缓存预留接口
 * 
 * 设计模式：
 * - 单例模式：确保全局唯一实例
 * - 仓库模式：抽象数据源，统一访问接口
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-03-31
 */
public class TaskRepository {
    
    private static volatile TaskRepository INSTANCE;
    
    private final TaskDao taskDao;
    private final LiveData<List<Task>> allTasksLive;
    private final ExecutorService executorService;
    
    /**
     * 私有构造函数（单例模式）
     * 
     * @param application 应用上下文
     */
    private TaskRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        
        // 获取所有任务的 LiveData（按创建时间倒序）
        allTasksLive = taskDao.getAllTasksLive();
        
        // 创建线程池用于异步操作（固定 4 个线程）
        executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 获取 TaskRepository 单例实例
     * 
     * @param application 应用上下文
     * @return TaskRepository 实例
     */
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
    
    // ==================== 插入操作 ====================
    
    /**
     * 插入任务（异步）
     * 
     * @param task 要插入的任务对象
     */
    public void insert(Task task) {
        executorService.execute(() -> {
            // 如果 ID 为空，自动生成
            if (task.getId() == 0) {
                task.setId(IdGenerator.generateId());
            }
            taskDao.insert(task);
        });
    }
    
    /**
     * 批量插入任务（异步）
     * 
     * @param tasks 要插入的任务列表
     */
    public void insertAll(List<Task> tasks) {
        executorService.execute(() -> {
            for (Task task : tasks) {
                if (task.getId() == 0) {
                    task.setId(IdGenerator.generateId());
                }
            }
            taskDao.insertAll(tasks);
        });
    }
    
    /**
     * 快速创建任务（便捷方法）
     * 
     * @param title 任务标题
     * @return 生成的任务 ID
     */
    public long createTask(String title) {
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        insert(task);
        return id;
    }
    
    /**
     * 快速创建任务（带描述）
     * 
     * @param title 任务标题
     * @param description 任务描述
     * @return 生成的任务 ID
     */
    public long createTask(String title, String description) {
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        task.setDescription(description);
        insert(task);
        return id;
    }
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新任务（异步）
     * 
     * @param task 要更新的任务对象
     */
    public void update(Task task) {
        executorService.execute(() -> taskDao.update(task));
    }
    
    /**
     * 批量更新任务（异步）
     * 
     * @param tasks 要更新的任务列表
     */
    public void updateAll(List<Task> tasks) {
        executorService.execute(() -> taskDao.updateAll(tasks));
    }
    
    /**
     * 标记任务为已完成（异步）
     * 
     * @param taskId 任务 ID
     */
    public void markTaskAsDone(long taskId) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null) {
                task.setStatus(Task.STATUS_DONE);
                taskDao.update(task);
            }
        });
    }
    
    /**
     * 标记任务为待办（异步）
     * 
     * @param taskId 任务 ID
     */
    public void markTaskAsTodo(long taskId) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null) {
                task.setStatus(Task.STATUS_TODO);
                taskDao.update(task);
            }
        });
    }
    
    /**
     * 设置任务优先级（异步）
     * 
     * @param taskId 任务 ID
     * @param priority 优先级
     */
    public void setTaskPriority(long taskId, int priority) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null) {
                task.setPriority(priority);
                taskDao.update(task);
            }
        });
    }
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除任务（异步）
     * 
     * @param task 要删除的任务对象
     */
    public void delete(Task task) {
        executorService.execute(() -> taskDao.delete(task));
    }
    
    /**
     * 根据 ID 删除任务（异步）
     * 
     * @param taskId 任务 ID
     */
    public void deleteById(long taskId) {
        executorService.execute(() -> taskDao.deleteById(taskId));
    }
    
    /**
     * 批量删除任务（异步）
     * 
     * @param tasks 要删除的任务列表
     */
    public void deleteAll(List<Task> tasks) {
        executorService.execute(() -> taskDao.deleteAll(tasks));
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有任务（LiveData 响应式）
     * 
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> getAllTasksLive() {
        return allTasksLive;
    }
    
    /**
     * 获取所有任务（同步，慎用）
     * 
     * @return 任务列表
     */
    public List<Task> getAllTasks() {
        return taskDao.getAllTasks();
    }
    
    /**
     * 根据 ID 获取任务（同步）
     * 
     * @param taskId 任务 ID
     * @return 任务对象，不存在则返回 null
     */
    public Task getTaskById(long taskId) {
        return taskDao.getTaskById(taskId);
    }
    
    /**
     * 根据 ID 获取任务（LiveData 响应式）
     * 
     * @param taskId 任务 ID
     * @return 可观察的任务对象
     */
    public LiveData<Task> getTaskByIdLive(long taskId) {
        return taskDao.getTaskByIdLive(taskId);
    }
    
    /**
     * 根据状态获取任务（LiveData 响应式）
     * 
     * @param status 任务状态
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> getTasksByStatusLive(int status) {
        return taskDao.getTasksByStatusLive(status);
    }
    
    /**
     * 获取未完成的任务（LiveData 响应式）
     * 
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> getIncompleteTasksLive() {
        return taskDao.getIncompleteTasksLive();
    }
    
    /**
     * 获取已过期的任务（同步）
     * 
     * @return 任务列表
     */
    public List<Task> getOverdueTasks() {
        return taskDao.getOverdueTasks(System.currentTimeMillis());
    }
    
    /**
     * 搜索任务（LiveData 响应式）
     * 
     * @param keyword 搜索关键词
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> searchTasksLive(String keyword) {
        return taskDao.searchTasksLive(keyword);
    }
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取任务总数
     * 
     * @return 任务数量
     */
    public int getTaskCount() {
        return taskDao.getTaskCount();
    }
    
    /**
     * 获取未完成任务数量
     * 
     * @return 未完成任务数量
     */
    public int getIncompleteTaskCount() {
        return taskDao.getIncompleteTaskCount();
    }
    
    /**
     * 获取过期任务数量
     * 
     * @return 过期任务数量
     */
    public int getOverdueTaskCount() {
        return taskDao.getOverdueTaskCount(System.currentTimeMillis());
    }
    
    /**
     * 关闭资源（应用退出时调用）
     */
    public void shutdown() {
        executorService.shutdown();
    }
}