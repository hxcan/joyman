package com.stupidbeauty.joyman;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * JoyMan 主界面 - 任务列表展示
 * 
 * 新增功能：
 * - 项目筛选
 * - 项目管理
 * - 任务关联项目
 * 
 * @author 太极美术工程狮狮长
 * @version 3.0.0
 * @since 2026-04-01
 */
public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener, ProjectAdapter.OnProjectClickListener {
    
    private static final String TAG = "MainActivity";
    private static final int FTP_SERVER_PORT = 2122;
    
    // UI 组件
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private FloatingActionButton fabAddTask;
    private Spinner spinnerProjects;
    
    // ViewModel
    private TaskViewModel taskViewModel;
    private ProjectViewModel projectViewModel;
    
    // 数据
    private List<Project> projectList;
    private Project selectedProject;
    private ArrayAdapter<String> projectSpinnerAdapter;
    
    // FTP
    private BuiltinFtpServer builtinFtpServer;
    private BuiltinFtpServerErrorListener errorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setSupportActionBar(findViewById(R.id.toolbar));
        
        // 初始化 ViewModel
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        
        initViews();
        setupRecyclerView();
        setupProjectSpinner();
        observeData();
        setupClickListeners();
        scheduleStartFtpServer();
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
                    selectedProject = projectList.get(position - 1);
                    loadTasksByProject(selectedProject.getId());
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeData() {
        // 观察项目列表
        projectViewModel.getAllProjects().observe(this, projects -> {
            projectList.clear();
            projectList.add(null); // 第一个为"全部项目"
            if (projects != null) {
                projectList.addAll(projects);
            }
            
            // 更新 Spinner
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
        
        // 观察所有任务
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null && selectedProject == null) {
                taskAdapter.setTasks(tasks);
                if (tasks.isEmpty()) {
                    Toast.makeText(this, R.string.no_tasks, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void showAddTaskDialog() {
        EditText editText = new EditText(this);
        editText.setHint(R.string.new_task_hint);
        editText.setPadding(50, 50, 50, 50);
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.add_task)
            .setView(editText)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                String title = editText.getText().toString().trim();
                if (!title.isEmpty()) {
                    long taskId = taskViewModel.createTask(title);
                    // TODO: 如果选择了项目，关联任务到项目
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
        
        // 观察项目数据
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
                taskAdapter.setTasks(tasks);
            }
        });
    }

    private void loadTasksByProject(long projectId) {
        taskViewModel.getTasksByProject(projectId).observe(this, tasks -> {
            if (tasks != null) {
                taskAdapter.setTasks(tasks);
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

    // ==================== TaskAdapter.OnTaskClickListener ====================
    @Override
    public void onTaskClick(Task task) {
        Toast.makeText(this, "点击了：" + task.getTitle(), Toast.LENGTH_SHORT).show();
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

    // ==================== ProjectAdapter.OnProjectClickListener ====================
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