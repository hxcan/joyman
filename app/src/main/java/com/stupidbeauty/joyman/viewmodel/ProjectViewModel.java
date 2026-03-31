package com.stupidbeauty.joyman.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.repository.ProjectRepository;

import java.util.List;

/**
 * Project ViewModel
 * 
 * MVVM 架构中的 ViewModel 层
 * 负责管理 Project 相关的 UI 数据
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-03-31
 */
public class ProjectViewModel extends AndroidViewModel {
    
    private final ProjectRepository repository;
    private final LiveData<List<Project>> allProjects;
    private final LiveData<List<Project>> activeProjects;
    
    public ProjectViewModel(@NonNull Application application) {
        super(application);
        repository = ProjectRepository.getInstance(application);
        allProjects = repository.getAllProjectsLive();
        activeProjects = repository.getActiveProjectsLive();
    }
    
    public LiveData<List<Project>> getAllProjects() {
        return allProjects;
    }
    
    public LiveData<List<Project>> getActiveProjects() {
        return activeProjects;
    }
    
    public LiveData<Project> getProjectById(long projectId) {
        return repository.getProjectByIdLive(projectId);
    }
    
    public LiveData<List<Project>> searchProjects(String keyword) {
        return repository.searchProjectsLive(keyword);
    }
    
    public void insert(Project project) {
        repository.insert(project);
    }
    
    public void update(Project project) {
        repository.update(project);
    }
    
    public void delete(Project project) {
        repository.delete(project);
    }
    
    public void deleteById(long projectId) {
        repository.deleteById(projectId);
    }
    
    public long createProject(String name) {
        return repository.createProject(name);
    }
    
    public long createProject(String name, String description, String color) {
        return repository.createProject(name, description, color);
    }
    
    public void archiveProject(long projectId) {
        repository.archiveProject(projectId);
    }
    
    public void unarchiveProject(long projectId) {
        repository.unarchiveProject(projectId);
    }
    
    public void setProjectColor(long projectId, String color) {
        repository.setProjectColor(projectId, color);
    }
    
    public void setProjectIcon(long projectId, String icon) {
        repository.setProjectIcon(projectId, icon);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}