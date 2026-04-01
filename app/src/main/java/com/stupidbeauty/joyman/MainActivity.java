package com.stupidbeauty.joyman;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stupidbeauty.builtinftp.BuiltinFtpServer;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.listener.BuiltinFtpServerErrorListener;
import com.stupidbeauty.joyman.ui.adapter.TaskAdapter;
import com.stupidbeauty.joyman.viewmodel.TaskViewModel;

import java.util.Timer;
import java.util.TimerTask;

/**
 * JoyMan 主界面 - 任务列表展示
 * 
 * MVVM 架构：
 * - View: MainActivity
 * - ViewModel: TaskViewModel
 * - Model: TaskRepository -> TaskDao -> Room Database
 * 
 * @author 太极美术工程狮狮长
 * @version 2.0.0
 * @since 2026-03-31
 */
public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {
    
    private static final String TAG = "MainActivity";
    
    // 内置 FTP 服务器固定端口
    private static final int FTP_SERVER_PORT = 2122;
    
    // UI 组件
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private FloatingActionButton fabAddTask;
    
    // ViewModel
    private TaskViewModel taskViewModel;
    
    // 内置 FTP 服务器
    private BuiltinFtpServer builtinFtpServer;
    private BuiltinFtpServerErrorListener errorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 设置 Toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        
        // 初始化 ViewModel
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        
        // 初始化视图
        initViews();
        
        // 设置 RecyclerView
        setupRecyclerView();
        
        // 观察数据变化
        observeData();
        
        // 设置按钮点击事件
        setupClickListeners();
        
        // 启动内置 FTP 服务器（用于数据备份）
        scheduleStartFtpServer();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_tasks);
        fabAddTask = findViewById(R.id.fab_add_task);
    }

    /**
     * 设置 RecyclerView
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, this);
        recyclerView.setAdapter(taskAdapter);
    }

    /**
     * 观察数据变化（LiveData）
     */
    private void observeData() {
        // 观察所有任务
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                taskAdapter.setTasks(tasks);
                
                // 显示空状态提示
                if (tasks.isEmpty()) {
                    Toast.makeText(this, R.string.no_tasks, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 设置按钮点击事件
     */
    private void setupClickListeners() {
        // 添加任务按钮
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    /**
     * 显示添加任务对话框
     */
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
                    taskViewModel.createTask(title);
                    Toast.makeText(this, R.string.task_created, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.task_empty_title, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    /**
     * 启动内置 FTP 服务器（延时 2 秒）
     */
    private void scheduleStartFtpServer() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                startFtpServer();
            }
        };
        timer.schedule(task, 2000);
    }

    /**
     * 启动内置 FTP 服务器
     */
    private void startFtpServer() {
        builtinFtpServer = new BuiltinFtpServer(this);
        errorListener = new BuiltinFtpServerErrorListener();
        
        builtinFtpServer.setPort(FTP_SERVER_PORT);
        builtinFtpServer.setAllowActiveMode(false);
        builtinFtpServer.setErrorListener(errorListener);
        builtinFtpServer.start();
        
        android.util.Log.d(TAG, "Built-in FTP server started on port " + FTP_SERVER_PORT);
    }

    // ==================== TaskAdapter.OnTaskClickListener ====================

    @Override
    public void onTaskClick(Task task) {
        Toast.makeText(this, "点击了：" + task.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: 打开任务详情页
    }

    @Override
    public void onTaskLongClick(Task task) {
        // 长按显示删除确认
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

    // ==================== Menu ====================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
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
}