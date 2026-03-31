package com.stupidbeauty.joyman.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.stupidbeauty.joyman.data.database.AppDatabase;
import com.stupidbeauty.joyman.data.database.dao.ProjectDao;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.util.IdGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * @version 1.0.0
 * @since 2026-03-31
 */
public class ProjectRepository {
    
    private static volatile ProjectRepository INSTANCE;
    
    private final ProjectDao projectDao;
    private final LiveData<List<Project>> allProjectsLive;
    private final LiveData<List<Project>> activeProjectsLive;
    private final ExecutorService executorService;
    
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
        executorService.execute(() -> {
            // 如果 ID 为空，自动生成
            if (project.getId() == 0) {
                project.setId(IdGenerator.generateId());
            }
            projectDao.insert(project);
        });
    }
    
    /**
     * 批量插入项目（异步）
     * 
     * @param projects 要插入的项目列表
     */
    public void insertAll(List<Project> projects) {
        executorService.execute(() -> {
            for (Project project : projects) {
                if (project.getId() == 0) {
                    project.setId(IdGenerator.generateId());
                }
            }
            projectDao.insertAll(projects);
        });
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
        return id;
    }
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新项目（异步）
     * 
     * @param project 要更新的项目对象
     */
    public void update(Project project) {
        executorService.execute(() -> projectDao.update(project));
    }
    
    /**
     * 批量更新项目（异步）
     * 
     * @param projects 要更新的项目列表
     */
    public void updateAll(List<Project> projects) {
        executorService.execute(() -> projectDao.updateAll(projects));
    }
    
    /**
     * 归档项目（异步）
     * 
     * @param projectId 项目 ID
     */
    public void archiveProject(long projectId) {
        executorService.execute(() -> {
            Project project = projectDao.getProjectById(projectId);
            if (project != null) {
                project.setArchived(true);
                projectDao.update(project);
            }
        });
    }
    
    /**
     * 取消归档项目（异步）
     * 
     * @param projectId 项目 ID
     */
    public void unarchiveProject(long projectId) {
        executorService.execute(() -> {
            Project project = projectDao.getProjectById(projectId);
            if (project != null) {
                project.setArchived(false);
                projectDao.update(project);
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
        executorService.execute(() -> {
            Project project = projectDao.getProjectById(projectId);
            if (project != null) {
                project.setColor(color);
                projectDao.update(project);
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
        executorService.execute(() -> {
            Project project = projectDao.getProjectById(projectId);
            if (project != null) {
                project.setIcon(icon);
                projectDao.update(project);
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
        executorService.execute(() -> projectDao.delete(project));
    }
    
    /**
     * 根据 ID 删除项目（异步）
     * 
     * @param projectId 项目 ID
     */
    public void deleteById(long projectId) {
        executorService.execute(() -> projectDao.deleteById(projectId));
    }
    
    /**
     * 批量删除项目（异步）
     * 
     * @param projects 要删除的项目列表
     */
    public void deleteAll(List<Project> projects) {
        executorService.execute(() -> projectDao.deleteAll(projects));
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
        executorService.shutdown();
    }
}