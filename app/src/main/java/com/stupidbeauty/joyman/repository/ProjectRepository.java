package com.stupidbeauty.joyman.repository;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;

import com.stupidbeauty.joyman.data.database.AppDatabase;
import com.stupidbeauty.joyman.data.database.dao.ProjectDao;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.util.IdGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Project 数据仓库
 * 
 * 统一管理 Project 数据的访问和操作
 * 作为 ViewModel 和 DAO 之间的中间层
 * 
 * 职责包括：
 * - 封装数据库操作（通过 ProjectDao）
 * - 提供高级业务逻辑方法
 * - 处理异步操作（使用 ExecutorService）
 * - 为未来添加网络同步、数据缓存预留接口
 * 
 * 设计模式：
 * - 单例模式：确保全局唯一实例
 * - 仓库模式：抽象数据源，统一访问接口
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.1
 * @since 2026-03-31
 */
public class ProjectRepository {
    
    private static final String TAG = "ProjectRepository";
    private static volatile ProjectRepository INSTANCE;
    
    private final ProjectDao projectDao;
    private final LiveData<List<Project>> allProjectsLive;
    private final LiveData<List<Project>> activeProjectsLive;
    private final ExecutorService executorService;
    private final AtomicBoolean isShutdown;
    
    /**
     * 私有构造函数（单例模式）
     * 
     * @param application 应用上下文
     */
    private ProjectRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        projectDao = database.projectDao();
        
        // 获取所有项目的 LiveData（按排序顺序升序）
        allProjectsLive = projectDao.getAllProjectsLive();
        
        // 获取未归档项目的 LiveData
        activeProjectsLive = projectDao.getActiveProjectsLive();
        
        // 创建线程池用于异步操作（固定 4 个线程）
        executorService = Executors.newFixedThreadPool(4);
        isShutdown = new AtomicBoolean(false);
        
        Log.i(TAG, "✅ Repository initialized with thread pool");
    }
    
    /**
     * 获取 ProjectRepository 单例实例
     * 
     * @param application 应用上下文
     * @return ProjectRepository 实例
     */
    public static ProjectRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (ProjectRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ProjectRepository(application);
                }
            }
        }
        return INSTANCE;
    }
    
    // ==================== 插入操作 ====================
    
    /**
     * 插入项目（异步）
     * 
     * @param project 要插入的项目对象
     */
    public void insert(Project project) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ insert() called after shutdown, ignoring task for project: " + 
                  (project != null ? project.getName() : "null"));
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    Log.w(TAG, "⚠️ Task rejected: executor shutdown during insert");
                    return;
                }
                
                try {
                    // 如果 ID 为空，自动生成
                    if (project.getId() == 0) {
                        project.setId(IdGenerator.generateId());
                    }
                    projectDao.insert(project);
                    Log.d(TAG, "✅ Inserted project: " + project.getName() + " (ID: " + project.getId() + ")");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error inserting project", e);
                }
            });
            Log.d(TAG, "📝 Submitting insert task for project: " + (project != null ? project.getName() : "null"));
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to submit insert task - executor may be shutdown", e);
        }
    }
    
    /**
     * 批量插入项目（异步）
     * 
     * @param projects 要插入的项目列表
     */
    public void insertAll(List<Project> projects) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ insertAll() called after shutdown, ignoring " + 
                  (projects != null ? projects.size() : 0) + " projects");
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    Log.w(TAG, "⚠️ Task rejected: executor shutdown during insertAll");
                    return;
                }
                
                try {
                    for (Project project : projects) {
                        if (project.getId() == 0) {
                            project.setId(IdGenerator.generateId());
                        }
                    }
                    projectDao.insertAll(projects);
                    Log.d(TAG, "✅ Inserted " + projects.size() + " projects");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error inserting projects", e);
                }
            });
            Log.d(TAG, "📝 Submitting insertAll task for " + (projects != null ? projects.size() : 0) + " projects");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to submit insertAll task - executor may be shutdown", e);
        }
    }
    
    /**
     * 快速创建项目（便捷方法）
     * 
     * @param name 项目名称
     * @return 生成的项目 ID
     */
    public long createProject(String name) {
        long id = IdGenerator.generateId();
        Project project = new Project(id, name);
        insert(project);
        Log.i(TAG, "🆕 Created project: " + name + " (ID: " + id + ")");
        return id;
    }
    
    /**
     * 快速创建项目（带描述和颜色）
     * 
     * @param name 项目名称
     * @param description 项目描述
     * @param color 项目颜色（十六进制格式）
     * @return 生成的项目 ID
     */
    public long createProject(String name, String description, String color) {
        long id = IdGenerator.generateId();
        Project project = new Project(id, name);
        project.setDescription(description);
        project.setColor(color);
        insert(project);
        Log.i(TAG, "🆕 Created project with details: " + name + " (ID: " + id + ")");
        return id;
    }
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新项目（异步）
     * 
     * @param project 要更新的项目对象
     */
    public void update(Project project) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ update() called after shutdown, ignoring task for project: " + 
                  (project != null ? project.getName() : "null"));
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    Log.w(TAG, "⚠️ Task rejected: executor shutdown during update");
                    return;
                }
                
                try {
                    projectDao.update(project);
                    Log.d(TAG, "✅ Updated project: " + (project != null ? project.getName() : "null"));
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error updating project", e);
                }
            });
            Log.d(TAG, "📝 Submitting update task for project: " + (project != null ? project.getName() : "null"));
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to submit update task - executor may be shutdown", e);
        }
    }
    
    /**
     * 批量更新项目（异步）
     * 
     * @param projects 要更新的项目列表
     */
    public void updateAll(List<Project> projects) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ updateAll() called after shutdown, ignoring " + 
                  (projects != null ? projects.size() : 0) + " projects");
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    Log.w(TAG, "⚠️ Task rejected: executor shutdown during updateAll");
                    return;
                }
                
                try {
                    projectDao.updateAll(projects);
                    Log.d(TAG, "✅ Updated " + projects.size() + " projects");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error updating projects", e);
                }
            });
            Log.d(TAG, "📝 Submitting updateAll task for " + (projects != null ? projects.size() : 0) + " projects");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to submit updateAll task - executor may be shutdown", e);
        }
    }
    
    /**
     * 归档项目（异步）
     * 
     * @param projectId 项目 ID
     */
    public void archiveProject(long projectId) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ archiveProject() called after shutdown for ID: " + projectId);
            return;
        }
        
        executorService.execute(() -> {
            if (isShutdown.get()) {
                Log.w(TAG, "⚠️ Task rejected: executor shutdown during archiveProject");
                return;
            }
            
            Project project = projectDao.getProjectById(projectId);
            if (project != null) {
                project.setArchived(true);
                projectDao.update(project);
                Log.d(TAG, "✅ Archived project ID: " + projectId);
            } else {
                Log.w(TAG, "⚠️ Project not found for archiving: " + projectId);
            }
        });
    }
    
    /**
     * 取消归档项目（异步）
     * 
     * @param projectId 项目 ID
     */
    public void unarchiveProject(long projectId) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ unarchiveProject() called after shutdown for ID: " + projectId);
            return;
        }
        
        executorService.execute(() -> {
            if (isShutdown.get()) {
                Log.w(TAG, "⚠️ Task rejected: executor shutdown during unarchiveProject");
                return;
            }
            
            Project project = projectDao.getProjectById(projectId);
            if (project != null) {
                project.setArchived(false);
                projectDao.update(project);
                Log.d(TAG, "✅ Unarchived project ID: " + projectId);
            } else {
                Log.w(TAG, "⚠️ Project not found for unarchiving: " + projectId);
            }
        });
    }
    
    /**
     * 设置项目颜色（异步）
     * 
     * @param projectId 项目 ID
     * @param color 颜色值（十六进制格式）
     */
    public void setProjectColor(long projectId, String color) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ setProjectColor() called after shutdown for ID: " + projectId);
            return;
        }
        
        executorService.execute(() -> {
            if (isShutdown.get()) {
                Log.w(TAG, "⚠️ Task rejected: executor shutdown during setProjectColor");
                return;
            }
            
            Project project = projectDao.getProjectById(projectId);
            if (project != null) {
                project.setColor(color);
                projectDao.update(project);
                Log.d(TAG, "✅ Set color for project ID: " + projectId);
            }
        });
    }
    
    /**
     * 设置项目图标（异步）
     * 
     * @param projectId 项目 ID
     * @param icon 图标（Emoji 或图标名称）
     */
    public void setProjectIcon(long projectId, String icon) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ setProjectIcon() called after shutdown for ID: " + projectId);
            return;
        }
        
        executorService.execute(() -> {
            if (isShutdown.get()) {
                Log.w(TAG, "⚠️ Task rejected: executor shutdown during setProjectIcon");
                return;
            }
            
            Project project = projectDao.getProjectById(projectId);
            if (project != null) {
                project.setIcon(icon);
                projectDao.update(project);
                Log.d(TAG, "✅ Set icon for project ID: " + projectId);
            }
        });
    }
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除项目（异步）
     * 
     * @param project 要删除的项目对象
     */
    public void delete(Project project) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ delete() called after shutdown for project: " + 
                  (project != null ? project.getName() : "null"));
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    Log.w(TAG, "⚠️ Task rejected: executor shutdown during delete");
                    return;
                }
                
                try {
                    projectDao.delete(project);
                    Log.d(TAG, "✅ Deleted project: " + (project != null ? project.getName() : "null"));
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error deleting project", e);
                }
            });
            Log.d(TAG, "📝 Submitting delete task for project: " + (project != null ? project.getName() : "null"));
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to submit delete task - executor may be shutdown", e);
        }
    }
    
    /**
     * 根据 ID 删除项目（异步）
     * 
     * @param projectId 项目 ID
     */
    public void deleteById(long projectId) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ deleteById() called after shutdown for ID: " + projectId);
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    Log.w(TAG, "⚠️ Task rejected: executor shutdown during deleteById");
                    return;
                }
                
                try {
                    projectDao.deleteById(projectId);
                    Log.d(TAG, "✅ Deleted project by ID: " + projectId);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error deleting project by ID", e);
                }
            });
            Log.d(TAG, "📝 Submitting deleteById task for ID: " + projectId);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to submit deleteById task - executor may be shutdown", e);
        }
    }
    
    /**
     * 批量删除项目（异步）
     * 
     * @param projects 要删除的项目列表
     */
    public void deleteAll(List<Project> projects) {
        if (isShutdown.get()) {
            Log.w(TAG, "⚠️ deleteAll() called after shutdown for " + 
                  (projects != null ? projects.size() : 0) + " projects");
            return;
        }
        
        try {
            executorService.execute(() -> {
                if (isShutdown.get()) {
                    Log.w(TAG, "⚠️ Task rejected: executor shutdown during deleteAll");
                    return;
                }
                
                try {
                    projectDao.deleteAll(projects);
                    Log.d(TAG, "✅ Deleted " + projects.size() + " projects");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error deleting projects", e);
                }
            });
            Log.d(TAG, "📝 Submitting deleteAll task for " + (projects != null ? projects.size() : 0) + " projects");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to submit deleteAll task - executor may be shutdown", e);
        }
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有项目（LiveData 响应式）
     * 
     * @return 可观察的项目列表
     */
    public LiveData<List<Project>> getAllProjectsLive() {
        return allProjectsLive;
    }
    
    /**
     * 获取所有项目（同步，慎用）
     * 
     * @return 项目列表
     */
    public List<Project> getAllProjects() {
        return projectDao.getAllProjects();
    }
    
    /**
     * 获取未归档的项目（LiveData 响应式）
     * 
     * @return 可观察的项目列表
     */
    public LiveData<List<Project>> getActiveProjectsLive() {
        return activeProjectsLive;
    }
    
    /**
     * 获取未归档的项目（同步）
     * 
     * @return 项目列表
     */
    public List<Project> getActiveProjects() {
        return projectDao.getActiveProjects();
    }
    
    /**
     * 根据 ID 获取项目（同步）
     * 
     * @param projectId 项目 ID
     * @return 项目对象，不存在则返回 null
     */
    public Project getProjectById(long projectId) {
        return projectDao.getProjectById(projectId);
    }
    
    /**
     * 根据 ID 获取项目（LiveData 响应式）
     * 
     * @param projectId 项目 ID
     * @return 可观察的项目对象
     */
    public LiveData<Project> getProjectByIdLive(long projectId) {
        return projectDao.getProjectByIdLive(projectId);
    }
    
    /**
     * 搜索项目（LiveData 响应式）
     * 
     * @param keyword 搜索关键词
     * @return 可观察的项目列表
     */
    public LiveData<List<Project>> searchProjectsLive(String keyword) {
        return projectDao.searchProjectsLive(keyword);
    }
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取项目总数
     * 
     * @return 项目数量
     */
    public int getProjectCount() {
        return projectDao.getProjectCount();
    }
    
    /**
     * 获取未归档的项目数量
     * 
     * @return 未归档项目数量
     */
    public int getActiveProjectCount() {
        return projectDao.getActiveProjectCount();
    }
    
    /**
     * 获取已归档的项目数量
     * 
     * @return 已归档项目数量
     */
    public int getArchivedProjectCount() {
        return projectDao.getArchivedProjectCount();
    }
    
    /**
     * 关闭资源（应用退出时调用）
     */
    public void shutdown() {
        Log.i(TAG, "🛑 Shutting down repository executor...");
        isShutdown.set(true);
        executorService.shutdown();
        Log.i(TAG, "✅ Repository executor shutdown complete");
    }
}