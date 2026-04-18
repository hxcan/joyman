package com.stupidbeauty.joyman.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.stupidbeauty.joyman.data.database.AppDatabase;
import com.stupidbeauty.joyman.data.database.dao.TaskDao;
import com.stupidbeauty.joyman.data.database.dao.CommentDao;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.data.database.entity.Comment;
import com.stupidbeauty.joyman.util.IdGenerator;
import com.stupidbeauty.joyman.util.LogUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Task 数据仓库
 *
 * @author 太极美术工程狮狮长
 * @version 2.0.11
 * @since 2026-03-31
 */
public class TaskRepository
{
    private static final String TAG = "TaskRepository";
    private static volatile TaskRepository INSTANCE;
    private final TaskDao taskDao;
    private final CommentDao commentDao;
    private final LiveData<List<Task>> allTasksLive;
    private final ExecutorService executorService;
    private final LogUtils logUtils;

    private TaskRepository(Application application)
    {
        logUtils = LogUtils.getInstance();
        logUtils.d(TAG, "Constructor: Creating repository for app: " + application.getPackageName());
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        commentDao = database.commentDao();
        allTasksLive = taskDao.getAllTasksLive();
        executorService = Executors.newFixedThreadPool(4);
        logUtils.d(TAG, "Constructor: Thread pool created with 4 threads - Repository will stay active for app lifetime");
    }

    public static TaskRepository getInstance(Application application)
    {
        if (INSTANCE == null)
        {
            synchronized (TaskRepository.class)
            {
                if (INSTANCE == null)
                {
                    LogUtils.getInstance().i(TAG, "getInstance: Created new instance");
                    INSTANCE = new TaskRepository(application);
                }
            }
        }
        else
        {
            LogUtils.getInstance().i(TAG, "getInstance: Returning existing instance");
        }
        return INSTANCE;
    }

    /**
     * 获取 TaskDao 实例
     * @return TaskDao 实例
     */
    public TaskDao getTaskDao()
    {
        return taskDao;
    }

    /**
     * 添加评论到任务
     * @param issueId 任务 ID
     * @param notes 评论内容
     */
    public void addNote(long issueId, String notes)
    {
        logUtils.d(TAG, "📝 Adding note to task " + issueId + ": " + notes);
        try
        {
            executorService.execute(() ->
            {
                try
                {
                    Comment comment = new Comment(issueId, notes, "admin");
                    commentDao.insert(comment);
                    logUtils.i(TAG, "✅ Note added successfully to task " + issueId);
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "❌ Error adding note", e);
                }
            });
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ Failed to submit addNote task", e);
        }
    }

    public void insert(Task task)
    {
        logUtils.d(TAG, "📝 Submitting insert task for: " + (task != null ? task.getTitle() : "null"));
        try
        {
            executorService.execute(() ->
            {
                try
                {
                    if (task.getId() == 0)
                        task.setId(IdGenerator.generateId());
                    taskDao.insert(task);
                    logUtils.d(TAG, "✅ Task inserted successfully: " + task.getTitle());
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "❌ Error inserting task", e);
                }
            });
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ Failed to submit insert task", e);
        }
    }

    public long createTask(String title)
    {
        logUtils.d(TAG, "🆕 Creating task with title: " + title);
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        insert(task);
        logUtils.i(TAG, "🆕 Created task: " + title + " (ID: " + id + ")");
        return id;
    }

    public long createTask(String title, String description)
    {
        logUtils.d(TAG, "🆕 Creating task with title: " + title + ", description: " + description);
        long id = IdGenerator.generateId();
        Task task = new Task(id, title);
        task.setDescription(description);
        insert(task);
        logUtils.i(TAG, "🆕 Created task with details: " + title + " (ID: " + id + ")");
        return id;
    }

    public void update(Task task)
    {
        logUtils.d(TAG, "📝 Submitting update task for: " + (task != null ? task.getTitle() : "null"));
        try
        {
            executorService.execute(() ->
            {
                try
                {
                    taskDao.update(task);
                    logUtils.i(TAG, "✅ Task updated successfully: " + (task != null ? task.getTitle() : "null"));
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "❌ Error updating task", e);
                }
            });
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ Failed to submit update task", e);
            // Fallback: execute on main thread
            logUtils.w(TAG, "🔄 FALLBACK - Executing update on main thread");
            try
            {
                taskDao.update(task);
                logUtils.i(TAG, "✅ FALLBACK SUCCESS - Task updated on main thread");
            }
            catch (Exception ex)
            {
                logUtils.e(TAG, "❌ FALLBACK FAILED - Error updating on main thread", ex);
            }
        }
    }

    public void markTaskAsDone(long taskId)
    {
        try
        {
            executorService.execute(() ->
            {
                try
                {
                    Task task = taskDao.getTaskById(taskId);
                    if (task != null)
                    {
                        task.setStatus(Task.STATUS_DONE);
                        taskDao.update(task);
                        logUtils.i(TAG, "✅ Task marked as done: " + taskId);
                    }
                    else
                    {
                        logUtils.w(TAG, "⚠️ Task not found: " + taskId);
                    }
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "❌ Error in markTaskAsDone", e);
                }
            });
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ Failed to submit markTaskAsDone task", e);
        }
    }

    public void markTaskAsTodo(long taskId)
    {
        try
        {
            executorService.execute(() ->
            {
                try
                {
                    Task task = taskDao.getTaskById(taskId);
                    if (task != null)
                    {
                        task.setStatus(Task.STATUS_TODO);
                        taskDao.update(task);
                        logUtils.i(TAG, "✅ Task marked as todo: " + taskId);
                    }
                    else
                    {
                        logUtils.w(TAG, "⚠️ Task not found: " + taskId);
                    }
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "❌ Error in markTaskAsTodo", e);
                }
            });
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ Failed to submit markTaskAsTodo task", e);
        }
    }

    public void setTaskPriority(long taskId, int priority)
    {
        try
        {
            executorService.execute(() ->
            {
                try
                {
                    Task task = taskDao.getTaskById(taskId);
                    if (task != null)
                    {
                        task.setPriority(priority);
                        taskDao.update(task);
                        logUtils.i(TAG, "✅ Priority updated for task: " + taskId);
                    }
                    else
                    {
                        logUtils.w(TAG, "⚠️ Task not found: " + taskId);
                    }
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "❌ Error in setTaskPriority", e);
                }
            });
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ Failed to submit setTaskPriority task", e);
        }
    }

    public void delete(Task task)
    {
        logUtils.d(TAG, "🗑️ Submitting delete task for: " + (task != null ? task.getTitle() : "null"));
        try
        {
            executorService.execute(() ->
            {
                try
                {
                    taskDao.delete(task);
                    logUtils.i(TAG, "✅ Task deleted: " + (task != null ? task.getTitle() : "null"));
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "❌ Error deleting task", e);
                }
            });
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ Failed to submit delete task", e);
        }
    }

    public void deleteById(long taskId)
    {
        logUtils.d(TAG, "🗑️ Submitting deleteById task for ID: " + taskId);
        try
        {
            executorService.execute(() ->
            {
                try
                {
                    taskDao.deleteById(taskId);
                    logUtils.i(TAG, "✅ Task deleted by ID: " + taskId);
                }
                catch (Exception e)
                {
                    logUtils.e(TAG, "❌ Error deleting task by ID", e);
                }
            });
        }
        catch (Exception e)
        {
            logUtils.e(TAG, "❌ Failed to submit deleteById task", e);
        }
    }

    /**
     * 获取所有任务（LiveData 响应式）
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> getAllTasksLive()
    {
        logUtils.d(TAG, "📋 Returning all tasks live data");
        return allTasksLive;
    }

    /**
     * 获取所有任务（同步，直接查询数据库）
     * 用于 API 服务层，避免 LiveData 异步问题
     * @return 任务列表
     */
    public List<Task> getAllTasks()
    {
        logUtils.d(TAG, "📋 Getting all tasks (sync query)");
        return taskDao.getAllTasks();
    }

    /**
     * 根据 ID 获取任务（同步）
     * @param taskId 任务 ID
     * @return 任务对象，不存在则返回 null
     */
    public Task getTaskById(long taskId)
    {
        logUtils.d(TAG, "🔍 Getting task by ID: " + taskId);
        return taskDao.getTaskById(taskId);
    }

    /**
     * 根据 ID 获取任务（LiveData 响应式）
     * @param taskId 任务 ID
     * @return 可观察的任务对象
     */
    public LiveData<Task> getTaskByIdLive(long taskId)
    {
        logUtils.d(TAG, "🔍 Getting live data for task ID: " + taskId);
        return taskDao.getTaskByIdLive(taskId);
    }

    /**
     * 根据状态获取任务（LiveData 响应式）
     * @param status 状态 ID
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> getTasksByStatusLive(int status)
    {
        logUtils.d(TAG, "📋 Getting tasks by status: " + status);
        return taskDao.getTasksByStatusLive(status);
    }

    /**
     * 获取未完成任务（LiveData 响应式）
     * @return 可观察的未完成任务列表
     */
    public LiveData<List<Task>> getIncompleteTasksLive()
    {
        logUtils.d(TAG, "📋 Returning incomplete tasks live data");
        return taskDao.getIncompleteTasksLive();
    }

    /**
     * 搜索任务（LiveData 响应式）
     * @param keyword 搜索关键词
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> searchTasksLive(String keyword)
    {
        logUtils.d(TAG, "🔍 Searching tasks with keyword: " + keyword);
        return taskDao.searchTasksLive(keyword);
    }

    /**
     * 多关键词搜索任务（LiveData 响应式）
     * 支持空格分隔的多个关键词，所有关键词都必须匹配（AND 逻辑）
     * 搜索范围：任务标题 + 任务描述
     * @param keywords 关键词字符串（空格分隔）
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> searchTasksByKeywords(String keywords)
    {
        logUtils.d(TAG, "🔍 Searching tasks by keywords: " + keywords);
        return taskDao.searchTasksByKeywords(keywords);
    }

    /**
     * 根据项目 ID 获取任务（LiveData 响应式）
     * @param projectId 项目 ID
     * @return 可观察的任务列表
     */
    public LiveData<List<Task>> getTasksByProjectLive(long projectId)
    {
        logUtils.d(TAG, "📋 Getting tasks for project ID: " + projectId);
        return taskDao.getTasksByProject(projectId);
    }

    /**
     * 获取任务总数
     * @return 任务数量
     */
    public int getTaskCount()
    {
        logUtils.d(TAG, "📊 Getting total task count");
        return taskDao.getTaskCount();
    }
}
