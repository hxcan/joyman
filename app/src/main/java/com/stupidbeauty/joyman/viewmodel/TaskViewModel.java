package com.stupidbeauty.joyman.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.repository.TaskRepository;

import java.util.List;

/**
 * Task ViewModel
 * 
 * @author 太极美术工程狮狮长
 * @version 2.0.1
 * @since 2026-03-31
 */
public class TaskViewModel extends AndroidViewModel {
    
    private final TaskRepository repository;
    private final LiveData<List<Task>> allTasks;
    private final LiveData<List<Task>> incompleteTasks;
    
    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = TaskRepository.getInstance(application);
        allTasks = repository.getAllTasksLive();
        incompleteTasks = repository.getIncompleteTasksLive();
    }
    
    public LiveData<List<Task>> getAllTasks() { return allTasks; }
    public LiveData<List<Task>> getIncompleteTasks() { return incompleteTasks; }
    public LiveData<Task> getTaskById(long taskId) { return repository.getTaskByIdLive(taskId); }
    public LiveData<List<Task>> getTasksByStatusLive(int status) { return repository.getTasksByStatusLive(status); }
    public LiveData<List<Task>> searchTasksLive(String keyword) { return repository.searchTasksLive(keyword); }
    public LiveData<List<Task>> getTasksByProject(long projectId) { return repository.getTasksByProjectLive(projectId); }
    
    public void insert(Task task) { repository.insert(task); }
    public void update(Task task) { repository.update(task); }
    public void delete(Task task) { repository.delete(task); }
    public void deleteById(long taskId) { repository.deleteById(taskId); }
    
    public long createTask(String title) { return repository.createTask(title); }
    public long createTask(String title, String description) { return repository.createTask(title, description); }
    
    public void markAsDone(long taskId) { repository.markTaskAsDone(taskId); }
    public void markAsTodo(long taskId) { repository.markTaskAsTodo(taskId); }
    public void setPriority(long taskId, int priority) { repository.setTaskPriority(taskId, priority); }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}