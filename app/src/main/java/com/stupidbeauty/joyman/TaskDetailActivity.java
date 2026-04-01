package com.stupidbeauty.joyman;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.util.LogUtils;
import com.stupidbeauty.joyman.viewmodel.ProjectViewModel;
import com.stupidbeauty.joyman.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * 任务详情界面
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.5
 * @since 2026-04-01
 */
public class TaskDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_TASK_ID = "task_id";
    private static final String TAG = "TaskDetailActivity";
    
    private long taskId;
    private Task task;
    private TaskViewModel taskViewModel;
    private ProjectViewModel projectViewModel;
    
    private TextView textTitle;
    private TextView textDescription;
    private TextView textStatus;
    private TextView textPriority;
    private TextView textProject;
    private TextView textCreatedAt;
    private Spinner spinnerProject;
    
    private List<Project> projectList;
    private Long pendingProjectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.getInstance().d(TAG, "=================================================================");
        LogUtils.getInstance().d(TAG, "onCreate: START - Activity created");
        LogUtils.getInstance().d(TAG, "onCreate: Task ID from intent: " + getIntent().getLongExtra(EXTRA_TASK_ID, 0));
        LogUtils.getInstance().d(TAG, "onCreate: Activity hash code: " + this.hashCode());
        
        setContentView(R.layout.activity_task_detail);
        
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("任务详情");
        }
        
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0);
        if (taskId == 0) {
            LogUtils.getInstance().e(TAG, "onCreate: Invalid task ID!");
            Toast.makeText(this, "无效的任务 ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        LogUtils.getInstance().d(TAG, "onCreate: Getting ViewModels");
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        LogUtils.getInstance().d(TAG, "onCreate: TaskViewModel hash code: " + taskViewModel.hashCode());
        LogUtils.getInstance().d(TAG, "onCreate: ProjectViewModel hash code: " + projectViewModel.hashCode());
        
        initViews();
        loadTask();
        loadProjects();
        
        LogUtils.getInstance().d(TAG, "onCreate: END");
        LogUtils.getInstance().d(TAG, "=================================================================");
    }
    
    private void initViews() {
        LogUtils.getInstance().d(TAG, "initViews: Initializing views");
        textTitle = findViewById(R.id.text_detail_title);
        textDescription = findViewById(R.id.text_detail_description);
        textStatus = findViewById(R.id.text_detail_status);
        textPriority = findViewById(R.id.text_detail_priority);
        textProject = findViewById(R.id.text_detail_project);
        textCreatedAt = findViewById(R.id.text_detail_created_at);
        spinnerProject = findViewById(R.id.spinner_detail_project);
        
        findViewById(R.id.btn_toggle_status).setOnClickListener(v -> toggleStatus());
        findViewById(R.id.btn_move_project).setOnClickListener(v -> showMoveProjectDialog());
        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteConfirm());
        findViewById(R.id.btn_save_project).setOnClickListener(v -> saveProjectSelection());
        LogUtils.getInstance().d(TAG, "initViews: Views initialized and listeners set");
    }
    
    private void loadTask() {
        LogUtils.getInstance().d(TAG, "loadTask: Loading task ID: " + taskId);
        taskViewModel.getTaskById(taskId).observe(this, task -> {
            LogUtils.getInstance().d(TAG, "loadTask: Observer triggered, task is null: " + (task == null));
            if (task == null) {
                LogUtils.getInstance().e(TAG, "loadTask: Task not found!");
                Toast.makeText(this, "任务不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            this.task = task;
            LogUtils.getInstance().d(TAG, "loadTask: Task loaded - ID: " + task.getId() + ", title: " + task.getTitle() + ", projectId: " + task.getProjectId());
            updateUI();
        });
    }
    
    private void loadProjects() {
        LogUtils.getInstance().d(TAG, "loadProjects: START - Loading projects for spinner");
        LogUtils.getInstance().d(TAG, "loadProjects: Activity is destroyed: " + isDestroyed());
        LogUtils.getInstance().d(TAG, "loadProjects: Activity is finishing: " + isFinishing());
        
        projectViewModel.getAllProjects().observe(this, projects -> {
            LogUtils.getInstance().d(TAG, "loadProjects: Observer triggered, projects is null: " + (projects == null));
            LogUtils.getInstance().d(TAG, "loadProjects: Activity is destroyed: " + isDestroyed());
            
            if (isDestroyed()) {
                LogUtils.getInstance().w(TAG, "loadProjects: Activity already destroyed, skipping update");
                return;
            }
            
            projectList = new ArrayList<>();
            List<String> projectNames = new ArrayList<>();
            
            projectList.add(null);
            projectNames.add("无项目");
            LogUtils.getInstance().d(TAG, "loadProjects: Added 'no project' option");
            
            if (projects != null) {
                for (Project project : projects) {
                    projectList.add(project);
                    projectNames.add(project.getIconDisplay() + " " + project.getName());
                }
                LogUtils.getInstance().d(TAG, "loadProjects: Added " + projects.size() + " projects");
            } else {
                LogUtils.getInstance().w(TAG, "loadProjects: Projects list is null");
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                projectNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProject.setAdapter(adapter);
            LogUtils.getInstance().d(TAG, "loadProjects: Adapter created and set");
            
            if (task != null && task.getProjectId() != null) {
                LogUtils.getInstance().d(TAG, "loadProjects: Setting selection for task with projectId: " + task.getProjectId());
                for (int i = 0; i < projectList.size(); i++) {
                    Project p = projectList.get(i);
                    if (p != null && p.getId() == task.getProjectId()) {
                        spinnerProject.setSelection(i);
                        LogUtils.getInstance().d(TAG, "loadProjects: Selected index: " + i);
                        break;
                    }
                }
            } else {
                LogUtils.getInstance().d(TAG, "loadProjects: Task has no project, selecting 'no project'");
                spinnerProject.setSelection(0);
            }
            
            spinnerProject.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    LogUtils.getInstance().d(TAG, "=================================================================");
                    LogUtils.getInstance().d(TAG, "onItemSelected: START - Position: " + position);
                    LogUtils.getInstance().d(TAG, "onItemSelected: Activity is destroyed: " + isDestroyed());
                    LogUtils.getInstance().d(TAG, "onItemSelected: Activity is finishing: " + isFinishing());
                    
                    if (isDestroyed()) {
                        LogUtils.getInstance().w(TAG, "onItemSelected: Activity already destroyed, ignoring selection");
                        return;
                    }
                    
                    if (position >= projectList.size()) {
                        LogUtils.getInstance().e(TAG, "onItemSelected: Invalid position: " + position + ", projectList size: " + projectList.size());
                        return;
                    }
                    
                    Project selectedProject = projectList.get(position);
                    LogUtils.getInstance().d(TAG, "onItemSelected: Selected project: " + (selectedProject == null ? "null" : selectedProject.getName()));
                    LogUtils.getInstance().d(TAG, "onItemSelected: Current task projectId: " + (task == null ? "null" : task.getProjectId()));
                    
                    if (selectedProject == null) {
                        pendingProjectId = null;
                        LogUtils.getInstance().d(TAG, "onItemSelected: Pending project set to null");
                    } else {
                        pendingProjectId = selectedProject.getId();
                        LogUtils.getInstance().d(TAG, "onItemSelected: Pending project set to: " + selectedProject.getId());
                    }
                    
                    LogUtils.getInstance().d(TAG, "onItemSelected: END (waiting for manual save)");
                    LogUtils.getInstance().d(TAG, "=================================================================");
                }
                
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    LogUtils.getInstance().d(TAG, "onNothingSelected: Called");
                }
            });
            
            LogUtils.getInstance().d(TAG, "loadProjects: Listener set up");
            LogUtils.getInstance().d(TAG, "loadProjects: END");
        });
    }
    
    private void saveProjectSelection() {
        LogUtils.getInstance().d(TAG, "=================================================================");
        LogUtils.getInstance().d(TAG, "saveProjectSelection: START");
        
        if (task == null) {
            LogUtils.getInstance().w(TAG, "saveProjectSelection: Task is null");
            Toast.makeText(this, "任务数据未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Long currentProjectId = task.getProjectId();
        LogUtils.getInstance().d(TAG, "saveProjectSelection: currentProjectId = " + (currentProjectId == null ? "null" : currentProjectId));
        LogUtils.getInstance().d(TAG, "saveProjectSelection: pendingProjectId = " + (pendingProjectId == null ? "null" : pendingProjectId));
        
        boolean isNullBoth = (currentProjectId == null && pendingProjectId == null);
        boolean isSameValue = (currentProjectId != null && pendingProjectId != null && currentProjectId.equals(pendingProjectId));
        
        LogUtils.getInstance().d(TAG, "saveProjectSelection: isNullBoth = " + isNullBoth);
        LogUtils.getInstance().d(TAG, "saveProjectSelection: isSameValue = " + isSameValue);
        
        if (isNullBoth || isSameValue) {
            LogUtils.getInstance().d(TAG, "saveProjectSelection: No change detected");
            Toast.makeText(this, "项目未变更", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (pendingProjectId == null) {
            LogUtils.getInstance().i(TAG, "saveProjectSelection: Removing project association");
            task.setProjectId(null);
            taskViewModel.update(task);
            Toast.makeText(this, "已移除项目关联", Toast.LENGTH_SHORT).show();
        } else {
            LogUtils.getInstance().i(TAG, "saveProjectSelection: Saving project: " + pendingProjectId);
            task.setProjectId(pendingProjectId);
            taskViewModel.update(task);
            
            String projectName = "未知";
            if (projectList != null) {
                for (Project p : projectList) {
                    if (p != null && p.getId() == pendingProjectId) {
                        projectName = p.getName();
                        break;
                    }
                }
            }
            Toast.makeText(this, "已保存到：📁 " + projectName, Toast.LENGTH_SHORT).show();
        }
        
        updateUI();
        pendingProjectId = null;
        LogUtils.getInstance().d(TAG, "saveProjectSelection: END");
        LogUtils.getInstance().d(TAG, "=================================================================");
    }
    
    private void updateUI() {
        LogUtils.getInstance().d(TAG, "updateUI: START - Updating UI");
        if (task == null) {
            LogUtils.getInstance().w(TAG, "updateUI: Task is null, skipping update");
            return;
        }
        
        textTitle.setText(task.getTitle());
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            textDescription.setText(task.getDescription());
            textDescription.setVisibility(android.view.View.VISIBLE);
        } else {
            textDescription.setVisibility(android.view.View.GONE);
        }
        
        textStatus.setText("状态：" + task.getStatusText());
        textPriority.setText("优先级：" + task.getPriorityText());
        
        Long projectId = task.getProjectId();
        LogUtils.getInstance().i(TAG, "updateUI: Task project ID: " + (projectId == null ? "null" : projectId));
        
        if (projectId != null) {
            LogUtils.getInstance().i(TAG, "updateUI: Observing projects to find project ID: " + projectId);
            projectViewModel.getAllProjects().observe(this, projects -> {
                LogUtils.getInstance().i(TAG, "updateUI: Projects observer triggered, projects count: " + (projects == null ? "null" : projects.size()));
                
                boolean found = false;
                if (projects != null) {
                    for (Project p : projects) {
                        LogUtils.getInstance().d(TAG, "updateUI: Checking project: " + p.getId() + " - " + p.getName());
                        if (p.getId() == projectId) {
                            String projectDisplay = "所属项目：" + p.getIconDisplay() + " " + p.getName();
                            textProject.setText(projectDisplay);
                            LogUtils.getInstance().i(TAG, "updateUI: Project found! Display: " + projectDisplay);
                            found = true;
                            break;
                        }
                    }
                }
                
                if (!found) {
                    LogUtils.getInstance().w(TAG, "updateUI: Project not found! ID: " + projectId);
                    textProject.setText("所属项目：未知项目 (ID: " + projectId + ")");
                }
            });
        } else {
            LogUtils.getInstance().i(TAG, "updateUI: Task has no project");
            textProject.setText("所属项目：无");
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textCreatedAt.setText("创建时间：" + sdf.format(new Date(task.getCreatedAt())));
        
        LogUtils.getInstance().d(TAG, "updateUI: END - UI updated");
    }
    
    private void toggleStatus() {
        LogUtils.getInstance().d(TAG, "toggleStatus: Toggling status for task ID: " + taskId);
        if (task == null) {
            LogUtils.getInstance().w(TAG, "toggleStatus: Task is null");
            return;
        }
        
        if (task.isDone()) {
            taskViewModel.markAsTodo(taskId);
            Toast.makeText(this, "已标记为待办", Toast.LENGTH_SHORT).show();
        } else {
            taskViewModel.markAsDone(taskId);
            Toast.makeText(this, "已标记为完成", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showMoveProjectDialog() {
        LogUtils.getInstance().d(TAG, "showMoveProjectDialog: Showing dialog");
        if (projectList == null) {
            LogUtils.getInstance().w(TAG, "showMoveProjectDialog: Project list not loaded yet");
            Toast.makeText(this, "项目列表加载中...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] projectNames = new String[projectList.size()];
        for (int i = 0; i < projectList.size(); i++) {
            Project p = projectList.get(i);
            projectNames[i] = (p == null) ? "无项目" : (p.getIconDisplay() + " " + p.getName());
        }
        
        int currentIndex = 0;
        if (task != null && task.getProjectId() != null) {
            for (int i = 0; i < projectList.size(); i++) {
                Project p = projectList.get(i);
                if (p != null && p.getId() == task.getProjectId()) {
                    currentIndex = i;
                    break;
                }
            }
        }
        
        new AlertDialog.Builder(this)
            .setTitle("移动到项目")
            .setSingleChoiceItems(projectNames, currentIndex, (dialog, which) -> {
                LogUtils.getInstance().d(TAG, "showMoveProjectDialog: Dialog item clicked, position: " + which);
                Project selectedProject = projectList.get(which);
                if (selectedProject == null) {
                    task.setProjectId(null);
                } else {
                    task.setProjectId(selectedProject.getId());
                }
                taskViewModel.update(task);
                
                String projectName = (selectedProject == null) ? "无项目" : selectedProject.getName();
                Toast.makeText(this, "已移动到：" + projectName, Toast.LENGTH_SHORT).show();
                
                dialog.dismiss();
                updateUI();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showDeleteConfirm() {
        LogUtils.getInstance().d(TAG, "showDeleteConfirm: Showing delete confirmation");
        if (task == null) {
            LogUtils.getInstance().w(TAG, "showDeleteConfirm: Task is null");
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("删除任务")
            .setMessage("确定要删除任务 \"" + task.getTitle() + "\" 吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                LogUtils.getInstance().i(TAG, "showDeleteConfirm: User confirmed deletion");
                taskViewModel.deleteById(taskId);
                Toast.makeText(this, "任务已删除", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            LogUtils.getInstance().d(TAG, "onOptionsItemSelected: Home button clicked, finishing activity");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        LogUtils.getInstance().d(TAG, "=================================================================");
        LogUtils.getInstance().d(TAG, "onDestroy: START - Activity being destroyed");
        LogUtils.getInstance().d(TAG, "onDestroy: Activity hash code: " + this.hashCode());
        LogUtils.getInstance().d(TAG, "onDestroy: Task ID: " + taskId);
        super.onDestroy();
        LogUtils.getInstance().d(TAG, "onDestroy: END");
        LogUtils.getInstance().d(TAG, "=================================================================");
    }
}