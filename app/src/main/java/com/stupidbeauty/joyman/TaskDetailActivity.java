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
 * @version 1.0.0
 * @since 2026-04-01
 */
public class TaskDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_TASK_ID = "task_id";
    
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("任务详情");
        }
        
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0);
        if (taskId == 0) {
            Toast.makeText(this, "无效的任务 ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        
        initViews();
        loadTask();
        loadProjects();
    }
    
    private void initViews() {
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
    }
    
    private void loadTask() {
        taskViewModel.getTaskById(taskId).observe(this, task -> {
            if (task == null) {
                Toast.makeText(this, "任务不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            this.task = task;
            updateUI();
        });
    }
    
    private void loadProjects() {
        projectViewModel.getAllProjects().observe(this, projects -> {
            projectList = new ArrayList<>();
            List<String> projectNames = new ArrayList<>();
            
            // 添加"无项目"选项
            projectList.add(null);
            projectNames.add("无项目");
            
            // 添加所有项目
            if (projects != null) {
                for (Project project : projects) {
                    projectList.add(project);
                    projectNames.add(project.getIconDisplay() + " " + project.getName());
                }
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                projectNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProject.setAdapter(adapter);
            
            // 设置当前选中的项目
            if (task != null && task.getProjectId() != null) {
                for (int i = 0; i < projectList.size(); i++) {
                    Project p = projectList.get(i);
                    if (p != null && p.getId() == task.getProjectId()) {
                        spinnerProject.setSelection(i);
                        break;
                    }
                }
            } else {
                spinnerProject.setSelection(0);
            }
            
            // 监听项目选择变化
            spinnerProject.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    Project selectedProject = projectList.get(position);
                    if (selectedProject == null) {
                        // 选择了"无项目"
                        if (task.getProjectId() != null) {
                            task.setProjectId(null);
                            taskViewModel.update(task);
                            Toast.makeText(TaskDetailActivity.this, "已移除项目关联", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // 选择了具体项目
                        if (task.getProjectId() == null || task.getProjectId() != selectedProject.getId()) {
                            task.setProjectId(selectedProject.getId());
                            taskViewModel.update(task);
                            Toast.makeText(TaskDetailActivity.this, "已移动到项目：" + selectedProject.getName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    updateUI();
                }
                
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        });
    }
    
    private void updateUI() {
        if (task == null) return;
        
        textTitle.setText(task.getTitle());
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            textDescription.setText(task.getDescription());
            textDescription.setVisibility(android.view.View.VISIBLE);
        } else {
            textDescription.setVisibility(android.view.View.GONE);
        }
        
        textStatus.setText("状态：" + task.getStatusText());
        textPriority.setText("优先级：" + task.getPriorityText());
        
        if (task.getProjectId() != null) {
            projectViewModel.getAllProjects().observe(this, projects -> {
                if (projects != null) {
                    for (Project p : projects) {
                        if (p.getId() == task.getProjectId()) {
                            textProject.setText("所属项目：" + p.getIconDisplay() + " " + p.getName());
                            break;
                        }
                    }
                }
            });
        } else {
            textProject.setText("所属项目：无");
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textCreatedAt.setText("创建时间：" + sdf.format(new Date(task.getCreatedAt())));
    }
    
    private void toggleStatus() {
        if (task == null) return;
        
        if (task.isDone()) {
            taskViewModel.markAsTodo(taskId);
            Toast.makeText(this, "已标记为待办", Toast.LENGTH_SHORT).show();
        } else {
            taskViewModel.markAsDone(taskId);
            Toast.makeText(this, "已标记为完成", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showMoveProjectDialog() {
        if (projectList == null) {
            Toast.makeText(this, "项目列表加载中...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] projectNames = new String[projectList.size()];
        for (int i = 0; i < projectList.size(); i++) {
            Project p = projectList.get(i);
            projectNames[i] = (p == null) ? "无项目" : (p.getIconDisplay() + " " + p.getName());
        }
        
        int currentIndex = 0;
        if (task.getProjectId() != null) {
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
        new AlertDialog.Builder(this)
            .setTitle("删除任务")
            .setMessage("确定要删除任务 \"" + task.getTitle() + "\" 吗？")
            .setPositiveButton("删除", (dialog, which) -> {
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
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}