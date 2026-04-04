package com.stupidbeauty.joyman;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
 * @version 1.0.10
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
    private ImageButton btnCopyTitle;
    private Spinner spinnerProject;
    private Spinner spinnerStatus;
    private View btnSaveChanges;
    
    private List<Project> projectList;
    private Long pendingProjectId;   // 暂存待保存的项目 ID
    private Integer pendingStatusId; // 暂存待保存的状态 ID
    
    // 状态列表数据
    private int[] statusIds;
    private String[] statusNames;
    private int[] statusColors;

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
        
        // 初始化状态数据
        initStatusData();
        
        initViews();
        loadTask();
        loadProjects();
        
        LogUtils.getInstance().d(TAG, "onCreate: END");
        LogUtils.getInstance().d(TAG, "=================================================================");
    }
    
    /**
     * 初始化状态数据（ID、名称、颜色）
     */
    private void initStatusData() {
        LogUtils.getInstance().d(TAG, "initStatusData: Initializing status data");
        
        statusIds = Task.getDefaultStatusIds();
        statusNames = Task.getDefaultStatusNames();
        statusColors = new int[]{
            ContextCompat.getColor(this, R.color.status_new),
            ContextCompat.getColor(this, R.color.status_in_progress),
            ContextCompat.getColor(this, R.color.status_resolved),
            ContextCompat.getColor(this, R.color.status_feedback),
            ContextCompat.getColor(this, R.color.status_closed)
        };
        
        LogUtils.getInstance().d(TAG, "initStatusData: Status data initialized, count: " + statusIds.length);
    }
    
    private void initViews() {
        LogUtils.getInstance().d(TAG, "initViews: Initializing views");
        textTitle = findViewById(R.id.text_detail_title);
        textDescription = findViewById(R.id.text_detail_description);
        textStatus = findViewById(R.id.text_detail_status);
        textPriority = findViewById(R.id.text_detail_priority);
        textProject = findViewById(R.id.text_detail_project);
        textCreatedAt = findViewById(R.id.text_detail_created_at);
        btnCopyTitle = findViewById(R.id.btn_copy_title);
        spinnerProject = findViewById(R.id.spinner_detail_project);
        spinnerStatus = findViewById(R.id.spinner_status);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        
        // 设置复制按钮点击事件
        btnCopyTitle.setOnClickListener(v -> copyTitleToClipboard());
        
        // 初始化状态下拉框
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            statusNames
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        
        // 状态变更监听 - 仅暂存，不保存
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (task != null) {
                    int selectedStatusId = statusIds[position];
                    if (selectedStatusId != task.getStatus()) {
                        pendingStatusId = selectedStatusId;
                        LogUtils.getInstance().d(TAG, "onItemSelected: Status changed to " + selectedStatusId + " (pending save)");
                    } else {
                        pendingStatusId = null;
                    }
                    updateSaveButtonState();
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        // 项目变更监听 - 仅暂存，不保存
        spinnerProject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (projectList != null && position < projectList.size()) {
                    Project selectedProject = projectList.get(position);
                    Long selectedProjectId = (selectedProject == null) ? null : selectedProject.getId();
                    
                    if (task != null) {
                        Long currentProjectId = task.getProjectId();
                        boolean isNullBoth = (currentProjectId == null && selectedProjectId == null);
                        boolean isSameValue = (currentProjectId != null && selectedProjectId != null && currentProjectId.equals(selectedProjectId));
                        
                        if (isNullBoth || isSameValue) {
                            pendingProjectId = null;
                        } else {
                            pendingProjectId = selectedProjectId;
                            LogUtils.getInstance().d(TAG, "onItemSelected: Project changed to " + selectedProjectId + " (pending save)");
                        }
                        updateSaveButtonState();
                    }
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        // 统一保存按钮
        btnSaveChanges.setOnClickListener(v -> saveAllChanges());
        
        findViewById(R.id.btn_move_project).setOnClickListener(v -> showMoveProjectDialog());
        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteConfirm());
        
        LogUtils.getInstance().d(TAG, "initViews: Views initialized and listeners set");
    }
    
    /**
     * 更新保存按钮的可用状态
     */
    private void updateSaveButtonState() {
        boolean hasChanges = (pendingStatusId != null || pendingProjectId != null);
        btnSaveChanges.setEnabled(hasChanges);
        btnSaveChanges.setAlpha(hasChanges ? 1.0f : 0.5f);
        
        if (hasChanges) {
            StringBuilder hint = new StringBuilder("保存更改：");
            List<String> changes = new ArrayList<>();
            if (pendingStatusId != null) {
                changes.add("状态");
            }
            if (pendingProjectId != null) {
                changes.add("项目");
            }
            hint.append(String.join("、", changes));
            ((TextView) btnSaveChanges).setText(hint.toString());
        } else {
            ((TextView) btnSaveChanges).setText("保存更改");
        }
        
        LogUtils.getInstance().d(TAG, "updateSaveButtonState: enabled=" + hasChanges);
    }
    
    /**
     * 复制任务标题到剪贴板
     */
    private void copyTitleToClipboard() {
        if (task == null) {
            LogUtils.getInstance().w(TAG, "copyTitleToClipboard: Task is null");
            Toast.makeText(this, "任务数据未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String title = task.getTitle();
        if (title == null || title.isEmpty()) {
            LogUtils.getInstance().w(TAG, "copyTitleToClipboard: Task title is empty");
            Toast.makeText(this, "任务标题为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            LogUtils.getInstance().e(TAG, "copyTitleToClipboard: ClipboardManager is null");
            Toast.makeText(this, "无法访问剪贴板", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipData clip = ClipData.newPlainText("JoyMan 任务标题", title);
        clipboard.setPrimaryClip(clip);
        
        LogUtils.getInstance().i(TAG, "copyTitleToClipboard: Title copied successfully: " + title);
        
        String toastMessage = "已复制：" + title;
        if (toastMessage.length() > 50) {
            toastMessage = toastMessage.substring(0, 47) + "...";
        }
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
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
            pendingStatusId = null;
            pendingProjectId = null;
            LogUtils.getInstance().d(TAG, "loadTask: Task loaded - ID: " + task.getId() + ", title: " + task.getTitle() + ", projectId: " + task.getProjectId() + ", status: " + task.getStatus());
            updateUI();
            updateSaveButtonState();
        });
    }
    
    private void loadProjects() {
        LogUtils.getInstance().d(TAG, "loadProjects: START - Loading projects for spinner");
        
        projectViewModel.getAllProjects().observe(this, projects -> {
            LogUtils.getInstance().d(TAG, "loadProjects: Observer triggered, projects is null: " + (projects == null));
            
            if (isDestroyed()) {
                LogUtils.getInstance().w(TAG, "loadProjects: Activity already destroyed, skipping update");
                return;
            }
            
            projectList = new ArrayList<>();
            List<String> projectNames = new ArrayList<>();
            
            projectList.add(null);
            projectNames.add("无项目");
            
            if (projects != null) {
                for (Project project : projects) {
                    projectList.add(project);
                    projectNames.add(project.getIconDisplay() + " " + project.getName());
                }
                LogUtils.getInstance().d(TAG, "loadProjects: Added " + projects.size() + " projects");
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                projectNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProject.setAdapter(adapter);
            
            LogUtils.getInstance().d(TAG, "loadProjects: END");
        });
    }
    
    /**
     * 保存所有待处理的变更（状态 + 项目）
     */
    private void saveAllChanges() {
        LogUtils.getInstance().d(TAG, "=================================================================");
        LogUtils.getInstance().d(TAG, "saveAllChanges: START");
        
        if (task == null) {
            LogUtils.getInstance().w(TAG, "saveAllChanges: Task is null");
            Toast.makeText(this, "任务数据未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean hasChanges = false;
        List<String> savedItems = new ArrayList<>();
        
        // 保存状态变更
        if (pendingStatusId != null) {
            LogUtils.getInstance().i(TAG, "saveAllChanges: Saving status: " + pendingStatusId);
            task.setStatus(pendingStatusId);
            String statusName = Task.getStatusNameById(pendingStatusId);
            savedItems.add("状态：" + statusName);
            pendingStatusId = null;
            hasChanges = true;
        }
        
        // 保存项目变更
        if (pendingProjectId != null) {
            LogUtils.getInstance().i(TAG, "saveAllChanges: Saving project: " + pendingProjectId);
            task.setProjectId(pendingProjectId);
            
            String projectName = "未知";
            if (projectList != null) {
                for (Project p : projectList) {
                    if (p != null && p.getId() == pendingProjectId) {
                        projectName = p.getName();
                        break;
                    }
                }
            }
            savedItems.add("项目：" + projectName);
            pendingProjectId = null;
            hasChanges = true;
        }
        
        if (!hasChanges) {
            LogUtils.getInstance().d(TAG, "saveAllChanges: No changes detected");
            Toast.makeText(this, "没有需要保存的更改", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 执行保存
        taskViewModel.update(task);
        
        String message = "已保存：" + String.join("、", savedItems);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
        updateUI();
        updateSaveButtonState();
        
        LogUtils.getInstance().d(TAG, "saveAllChanges: END - Saved: " + String.join(", ", savedItems));
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
            textDescription.setVisibility(View.VISIBLE);
        } else {
            textDescription.setVisibility(View.GONE);
        }
        
        updateStatusUI();
        textPriority.setText("优先级：" + task.getPriorityText());
        
        Long projectId = task.getProjectId();
        if (projectId != null) {
            projectViewModel.getAllProjects().observe(this, projects -> {
                boolean found = false;
                if (projects != null) {
                    for (Project p : projects) {
                        if (p.getId() == projectId) {
                            textProject.setText("所属项目：" + p.getIconDisplay() + " " + p.getName());
                            found = true;
                            
                            if (projectList != null) {
                                for (int i = 0; i < projectList.size(); i++) {
                                    Project sp = projectList.get(i);
                                    if (sp != null && sp.getId() == projectId) {
                                        spinnerProject.setSelection(i);
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                
                if (!found) {
                    textProject.setText("所属项目：未知项目 (ID: " + projectId + ")");
                }
            });
        } else {
            textProject.setText("所属项目：无");
            if (projectList != null) {
                spinnerProject.setSelection(0);
            }
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textCreatedAt.setText("创建时间：" + sdf.format(new Date(task.getCreatedAt())));
        
        updateStatusSpinnerSelection();
        
        LogUtils.getInstance().d(TAG, "updateUI: END - UI updated");
    }
    
    private void updateStatusUI() {
        if (task == null) return;
        
        String statusText = task.getStatusText();
        textStatus.setText(statusText);
        
        int colorIndex = task.getStatus() - Task.STATUS_NEW;
        if (colorIndex >= 0 && colorIndex < statusColors.length) {
            textStatus.setTextColor(statusColors[colorIndex]);
        } else {
            textStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }
    
    private void updateStatusSpinnerSelection() {
        if (task == null) return;
        
        int currentStatusId = task.getStatus();
        for (int i = 0; i < statusIds.length; i++) {
            if (statusIds[i] == currentStatusId) {
                spinnerStatus.setSelection(i);
                break;
            }
        }
    }
    
    private void showMoveProjectDialog() {
        LogUtils.getInstance().d(TAG, "showMoveProjectDialog: Showing dialog");
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
                Project selectedProject = projectList.get(which);
                Long selectedProjectId = (selectedProject == null) ? null : selectedProject.getId();
                
                if (task != null) {
                    Long currentProjectId = task.getProjectId();
                    boolean isNullBoth = (currentProjectId == null && selectedProjectId == null);
                    boolean isSameValue = (currentProjectId != null && selectedProjectId != null && currentProjectId.equals(selectedProjectId));
                    
                    if (isNullBoth || isSameValue) {
                        pendingProjectId = null;
                    } else {
                        pendingProjectId = selectedProjectId;
                    }
                    updateSaveButtonState();
                }
                
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showDeleteConfirm() {
        if (task == null) return;
        
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
    
    @Override
    protected void onDestroy() {
        LogUtils.getInstance().d(TAG, "onDestroy: START");
        super.onDestroy();
        LogUtils.getInstance().d(TAG, "onDestroy: END");
    }
}