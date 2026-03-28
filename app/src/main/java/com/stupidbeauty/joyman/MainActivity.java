package com.stupidbeauty.joyman;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.ui.adapter.TaskAdapter;
import com.stupidbeauty.builtinftp.BuiltinFtpServer;
import com.stupidbeauty.joyman.listener.BuiltinFtpServerErrorListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * JoyMan 主界面 - 任务列表展示
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    // 内置 FTP服务器固定端口（避免与 BlindBox.her 等应用冲突）
    private static final int FTP_SERVER_PORT = 2122;
    
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    
    // 内置 FTP服务器相关
    private BuiltinFtpServer builtinFtpServer = null;
    private BuiltinFtpServerErrorListener builtinFtpServerErrorListener = null;

    /**
     * 启动内置 FTP服务器
     */
    private void startBuiltinFtpServer() {
        builtinFtpServer = new BuiltinFtpServer(this);
        builtinFtpServerErrorListener = new BuiltinFtpServerErrorListener();
        
        builtinFtpServer.setPort(FTP_SERVER_PORT);
        builtinFtpServer.setAllowActiveMode(false);
        builtinFtpServer.setErrorListener(builtinFtpServerErrorListener);
        builtinFtpServer.start();
    }

    /**
     * 计划启动内置 FTP服务器（延时 2 秒）
     */
    private void scheduleStartBuiltinFtpServer() {
        Timer timerObj = new Timer();
        TimerTask timerTaskObj = new TimerTask() {
            public void run() {
                startBuiltinFtpServer();
            }
        };
        timerObj.schedule(timerTaskObj, 2000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setSupportActionBar(findViewById(R.id.toolbar));
        
        // 启动内置 FTP服务器（用于数据备份）
        scheduleStartBuiltinFtpServer();
        
        // 初始化 RecyclerView
        initRecyclerView();
        
        // 加载示例数据（后续替换为数据库查询）
        loadSampleTasks();
    }

    /**
     * 初始化 RecyclerView
     */
    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this::onTaskClick);
        recyclerView.setAdapter(taskAdapter);
    }

    /**
     * 加载示例任务数据
     * TODO: 替换为从 Room 数据库加载
     */
    private void loadSampleTasks() {
        taskList.clear();
        
        // 示例数据
        taskList.add(new Task(1L, "测试任务 1", "这是一个测试任务", Task.PRIORITY_NORMAL, Task.STATUS_NEW, System.currentTimeMillis()));
        taskList.add(new Task(2L, "测试任务 2", "另一个测试任务", Task.PRIORITY_HIGH, Task.STATUS_IN_PROGRESS, System.currentTimeMillis()));
        taskList.add(new Task(3L, "测试任务 3", "第三个测试任务", Task.PRIORITY_LOW, Task.STATUS_DONE, System.currentTimeMillis()));
        
        taskAdapter.notifyDataSetChanged();
    }

    /**
     * 任务点击事件处理
     */
    private void onTaskClick(Task task) {
        Toast.makeText(this, "点击了：" + task.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: 打开任务详情页
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            Toast.makeText(this, "打开设置", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_sync) {
            Toast.makeText(this, "开始同步", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_export) {
            Toast.makeText(this, "导出任务包", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_import) {
            Toast.makeText(this, "导入任务包", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}