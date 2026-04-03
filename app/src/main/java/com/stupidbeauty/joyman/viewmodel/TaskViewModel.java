package com.stupidbeauty.joyman.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.repository.TaskRepository;

import java.util.List;


/**
 * Task ViewModel
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.3
 * @since 2026-03-31
 */
public class TaskViewModel extends AndroidViewModel {
    
    private final TaskRepository repository;
    private final LiveData<List<Task>> allTasksLive;
    private final LiveData<List<Task>> incompleteTasksLive;
    
    public TaskViewModel(Application application) {
        super(application);
        repository = TaskRepository.getInstance(application);
        allTasksLive = repository.getAllTasksLive();
        incompleteTasksLive = repository.getIncompleteTasksLive();
    }
    
    public LiveData<List<Task>> getAllTasks() {
        return allTasksLive;
    }
    
    public LiveData<List<Task>> getAllTasksLive() {
        return allTasksLive;
    }
    
    public LiveData<List<Task>> getIncompleteTasksLive() {
        return incompleteTasksLive;
    }
    
    public LiveData<Task> getTaskById(long taskId) {
        return repository.getTaskByIdLive(taskId);
    }
    
    public LiveData<List<Task>> getTasksByStatus(int status) {
        return repository.getTasksByStatusLive(status);
    }
    
    public LiveData<List<Task>> searchTasks(String keyword) {
        return repository.searchTasksLive(keyword);
    }
    
    public LiveData<List<Task>> getTasksByProject(long projectId) {
        return repository.getTasksByProjectLive(projectId);
    }
    
    public void markAsDone(long taskId) {
        repository.markTaskAsDone(taskId);
    }
    
    public void markAsTodo(long taskId) {
        repository.markTaskAsTodo(taskId);
    }
    
    public void update(Task task) {
        repository.update(task);
    }
    
    public void delete(Task task) {
        repository.delete(task);
    }
    
    public void deleteById(long taskId) {
        repository.deleteById(taskId);
    }
    
    public long createTask(String title) {
        return repository.createTask(title);
    }
    
    public long createTask(String title, String description) {
        return repository.createTask(title, description);
    }
    
    public int getTaskCount() {
        return repository.getTaskCount();
    }
}