package com.stupidbeauty.joyman;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stupidbeauty.builtinftp.BuiltinFtpServer;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.listener.BuiltinFtpServerErrorListener;
import com.stupidbeauty.joyman.ui.adapter.ProjectAdapter;
import com.stupidbeauty.joyman.ui.adapter.TaskAdapter;
import com.stupidbeauty.joyman.viewmodel.ProjectViewModel;
import com.stupidbeauty.joyman.viewmodel.TaskViewModel;
import com.stupidbeauty.joyman.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * JoyMan 主界面 - 任务列表展示
 * 
 * @author 太极美术工程狮狮长
 * @version 3.0.5
 * @since 2026-04-01
 */
public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener, ProjectAdapter.OnProjectClickListener {
    
    private static final String TAG = "MainActivity";
    private static final int FTP_SERVER_PORT = 2122;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    
    private static MainActivity instance;
    
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private FloatingActionButton fabAddTask;
    private Spinner spinnerProjects;
    private TaskViewModel taskViewModel;
    private ProjectViewModel projectViewModel;
    private List<Project> projectList;
    private Project selectedProject;
    private ArrayAdapter<String> projectSpinnerAdapter;
    private BuiltinFtpServer builtinFtpServer;
    private BuiltinFtpServerErrorListener errorListener;
    private LogUtils logUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        logUtils = LogUtils.getInstance();
        
        logUtils.i(TAG, "onCreate: MainActivity created");
        
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        
        initViews();
        setupRecyclerView();
        setupProjectSpinner();
        observeData();
        setupClickListeners();
        scheduleStartFtpServer();
        
        // 检查通知权限（Android 13+）
        checkNotificationPermission();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }
    
    /**
     * 获取 MainActivity 实例（用于 Service 请求权限）
     */
    public static MainActivity getInstance() {
        return instance;
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_tasks);
        fabAddTask = findViewById(R.id.fab_add_task);
        spinnerProjects = findViewById(R.id.spinner_projects);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, this);
        recyclerView.setAdapter(taskAdapter);
    }

    private void setupProjectSpinner() {
        projectList = new ArrayList<>();
        projectSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        projectSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProjects.setAdapter(projectSpinnerAdapter);
        
        spinnerProjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedProject = null;
                    loadAllTasks();
                } else {
                    selectedProject = projectList.get(position);
                    if (selectedProject != null) {
                        loadTasksByProject(selectedProject.getId());
                    }
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeData() {
        projectViewModel.getAllProjects().observe(this, projects -> {
            projectList.clear();
            projectList.add(null);
            if (projects != null) {
                projectList.addAll(projects);
            }
            
            List<String> projectNames = new ArrayList<>();
            projectNames.add(getString(R.string.all_projects));
            for (int i = 1; i < projectList.size(); i++) {
                Project p = projectList.get(i);
                projectNames.add(p.getIconDisplay() + " " + p.getName());
            }
            
            projectSpinnerAdapter.clear();
            projectSpinnerAdapter.addAll(projectNames);
            projectSpinnerAdapter.notifyDataSetChanged();
        });
        
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null && selectedProject == null) {
                // 过滤掉已关闭的任务
                List<Task> filteredTasks = tasks.stream()
                    .filter(task -> task.getStatus() != Task.STATUS_CLOSED)
                    .collect(java.util.stream.Collectors.toList());
                
                taskAdapter.setTasks(filteredTasks);
                if (filteredTasks.isEmpty()) {
                    Toast.makeText(this, R.string.no_tasks, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void showAddTaskDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // 任务标题输入框
        EditText editTextTitle = new EditText(this);
        editTextTitle.setHint(R.string.new_task_hint);
        editTextTitle.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(editTextTitle);
        
        // 任务描述输入框（新增）
        EditText editTextDescription = new EditText(this);
        editTextDescription.setHint("任务描述（可选）");
        editTextDescription.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        editTextDescription.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editTextDescription.setMinLines(3);
        editTextDescription.setMaxLines(5);
        editTextDescription.setGravity(android.view.Gravity.TOP);
        layout.addView(editTextDescription);
        
        // 项目选择器
        Spinner spinnerProject = new Spinner(this);
        spinnerProject.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        List<String> projectNames = new ArrayList<>();
        List<Long> projectIds = new ArrayList<>();
        
        projectNames.add(getString(R.string.no_project));
        projectIds.add(null);
        
        if (projectList != null) {
            for (int i = 1; i < projectList.size(); i++) {
                Project project = projectList.get(i);
                if (project != null) {
                    projectNames.add(project.getIconDisplay() + " " + project.getName());
                    projectIds.add(project.getId());
                }
            }
        }
        
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            projectNames
        );
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(projectAdapter);
        
        layout.addView(spinnerProject);
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.add_task)
            .setView(layout)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                String title = editTextTitle.getText().toString().trim();
                String description = editTextDescription.getText().toString().trim();
                
                if (!title.isEmpty()) {
                    int selectedPosition = spinnerProject.getSelectedItemPosition();
                    Long projectId = null;
                    if (selectedPosition > 0 && selectedPosition < projectIds.size()) {
                        projectId = projectIds.get(selectedPosition);
                    }
                    
                    // 创建任务（支持描述）
                    long taskId = taskViewModel.createTask(title, description);
                    
                    if (projectId != null) {
                        Task task = new Task(taskId, title);
                        task.setDescription(description);
                        task.setProjectId(projectId);
                        taskViewModel.update(task);
                    }
                    
                    Toast.makeText(this, R.string.task_created, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.task_empty_title, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showManageProjectsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_projects, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_view_projects);
        
        ProjectAdapter adapter = new ProjectAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        projectViewModel.getAllProjects().observe(this, projects -> {
            if (projects != null) {
                adapter.setProjects(projects);
            }
        });
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.manage_projects)
            .setView(dialogView)
            .setPositiveButton(R.string.confirm, null)
            .setNeutralButton(R.string.create_project, (d, which) -> showCreateProjectDialog())
            .create();
        
        dialog.show();
    }

    private void showCreateProjectDialog() {
        EditText editText = new EditText(this);
        editText.setHint(R.string.project_name_hint);
        editText.setPadding(50, 50, 50, 50);
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.create_project)
            .setView(editText)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                String name = editText.getText().toString().trim();
                if (!name.isEmpty()) {
                    projectViewModel.createProject(name);
                    Toast.makeText(this, R.string.project_created, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.project_empty_name, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void loadAllTasks() {
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                // 过滤掉已关闭的任务
                List<Task> filteredTasks = tasks.stream()
                    .filter(task -> task.getStatus() != Task.STATUS_CLOSED)
                    .collect(java.util.stream.Collectors.toList());
                
                taskAdapter.setTasks(filteredTasks);
            }
        });
    }

    private void loadTasksByProject(long projectId) {
        taskViewModel.getTasksByProject(projectId).observe(this, tasks -> {
            if (tasks != null) {
                // 过滤掉已关闭的任务
                List<Task> filteredTasks = tasks.stream()
                    .filter(task -> task.getStatus() != Task.STATUS_CLOSED)
                    .collect(java.util.stream.Collectors.toList());
                
                taskAdapter.setTasks(filteredTasks);
            }
        });
    }

    private void scheduleStartFtpServer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                startFtpServer();
            }
        }, 2000);
    }

    private void startFtpServer() {
        builtinFtpServer = new BuiltinFtpServer(this);
        errorListener = new BuiltinFtpServerErrorListener();
        builtinFtpServer.setPort(FTP_SERVER_PORT);
        builtinFtpServer.setAllowActiveMode(false);
        builtinFtpServer.setErrorListener(errorListener);
        builtinFtpServer.start();
        android.util.Log.d(TAG, "FTP server started on port " + FTP_SERVER_PORT);
    }
    
    /**
     * 检查通知权限（Android 13+）
     */
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                logUtils.w(TAG, "checkNotificationPermission: Permission not granted, requesting...");
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            } else {
                logUtils.d(TAG, "checkNotificationPermission: Permission already granted");
            }
        }
    }
    
    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                logUtils.i(TAG, "onRequestPermissionsResult: Notification permission granted");
                Toast.makeText(this, "通知权限已授予，API 服务将显示通知", Toast.LENGTH_SHORT).show();
            } else {
                logUtils.w(TAG, "onRequestPermissionsResult: Notification permission denied");
                Toast.makeText(this, "通知权限被拒绝，API 服务可能无法正常显示通知", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_manage_projects) {
            showManageProjectsDialog();
            return true;
        } else if (id == R.id.action_settings) {
            Toast.makeText(this, R.string.settings, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_sync) {
            Toast.makeText(this, R.string.sync, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_export) {
            Toast.makeText(this, R.string.export, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_import) {
            Toast.makeText(this, R.string.import_text, Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    @Override
    public void onTaskLongClick(Task task) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("确定要删除任务 \"" + task.getTitle() + "\" 吗？")
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                taskViewModel.deleteById(task.getId());
                Toast.makeText(this, "任务已删除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public void onTaskComplete(Task task) {
        if (task.isDone()) {
            taskViewModel.markAsTodo(task.getId());
        } else {
            taskViewModel.markAsDone(task.getId());
        }
    }

    @Override
    public void onProjectClick(Project project) {
        Toast.makeText(this, "项目：" + project.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProjectLongClick(Project project) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("确定要删除项目 \"" + project.getName() + "\" 吗？")
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                projectViewModel.deleteById(project.getId());
                Toast.makeText(this, "项目已删除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
}