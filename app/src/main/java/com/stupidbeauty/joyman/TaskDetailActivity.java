package com.stupidbeauty.joyman;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stupidbeauty.joyman.data.database.entity.Comment;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.ui.adapter.CommentAdapter;
import com.stupidbeauty.joyman.ui.adapter.ParentTaskAdapter;
import com.stupidbeauty.joyman.ui.adapter.SubtaskAdapter;
import com.stupidbeauty.joyman.util.LogUtils;
import com.stupidbeauty.joyman.viewmodel.ProjectViewModel;
import com.stupidbeauty.joyman.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * 任务详情界面
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.18
 * @since 2026-04-01
 */
public class TaskDetailActivity extends AppCompatActivity implements SubtaskAdapter.OnSubtaskClickListener {
    
    public static final String EXTRA_TASK_ID = "task_id";
    private static final String TAG = "TaskDetailActivity";
    
    private long taskId;
    private Task task;
    private TaskViewModel taskViewModel;
    private ProjectViewModel projectViewModel;
    
    private TextView textTitle;
    private TextView textStatus;
    private TextView textPriority;
    private TextView textProject;
    private TextView textCreatedAt;
    private TextView textDetailId;
    private ImageButton btnCopyTitle;
    private Spinner spinnerProject;
    private Spinner spinnerStatus;
    private View btnSaveChanges;
    private View btnCreateSubtask;
    private EditText editDetailDescription;
    
    // 子任务列表相关
    private TextView textSubtasksTitle;
    private RecyclerView recyclerViewSubtasks;
    private TextView textNoSubtasks;
    private SubtaskAdapter subtaskAdapter;
    
    // 父任务相关
    private CardView cardParentTask;
    private TextView textParentTaskTitle;
    private Button btnLinkParentTask;
    
    // 评论功能相关
    private TextView textCommentsTitle;
    private EditText editCommentInput;
    private Button btnSendComment;
    private RecyclerView recyclerViewComments;
    private TextView textNoComments;
    private CommentAdapter commentAdapter;
    
    private List<Project> projectList;
    private Long pendingProjectId;
    private Integer pendingStatusId;
    private String pendingDescription;
    private Long pendingParentId;
    
    private int[] statusIds;
    private String[] statusNames;
    private int[] statusColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.getInstance().d(TAG, "=================================================================");
        LogUtils.getInstance().d(TAG, "onCreate: START");
        
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
        
        initStatusData();
        initViews();
        loadTask();
        loadProjects();
        loadSubtasks();
        loadComments();
        
        LogUtils.getInstance().d(TAG, "onCreate: END");
    }
    
    private void initStatusData() {
        statusIds = Task.getDefaultStatusIds();
        statusNames = Task.getDefaultStatusNames();
        statusColors = new int[]{
            ContextCompat.getColor(this, R.color.status_new),
            ContextCompat.getColor(this, R.color.status_in_progress),
            ContextCompat.getColor(this, R.color.status_resolved),
            ContextCompat.getColor(this, R.color.status_feedback),
            ContextCompat.getColor(this, R.color.status_closed)
        };
    }
    
    private void initViews() {
        textTitle = findViewById(R.id.text_detail_title);
        textStatus = findViewById(R.id.text_detail_status);
        textPriority = findViewById(R.id.text_detail_priority);
        textProject = findViewById(R.id.text_detail_project);
        textCreatedAt = findViewById(R.id.text_detail_created_at);
        textDetailId = findViewById(R.id.text_detail_id);
        btnCopyTitle = findViewById(R.id.btn_copy_title);
        spinnerProject = findViewById(R.id.spinner_detail_project);
        spinnerStatus = findViewById(R.id.spinner_status);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnCreateSubtask = findViewById(R.id.btn_create_subtask);
        editDetailDescription = findViewById(R.id.edit_detail_description);
        
        textSubtasksTitle = findViewById(R.id.text_subtasks_title);
        recyclerViewSubtasks = findViewById(R.id.recycler_view_subtasks);
        textNoSubtasks = findViewById(R.id.text_no_subtasks);
        
        cardParentTask = findViewById(R.id.card_parent_task);
        textParentTaskTitle = findViewById(R.id.text_parent_task_title);
        btnLinkParentTask = findViewById(R.id.btn_link_parent_task);
        
        // 评论功能初始化
        textCommentsTitle = findViewById(R.id.text_comments_title);
        editCommentInput = findViewById(R.id.edit_comment_input);
        btnSendComment = findViewById(R.id.btn_send_comment);
        recyclerViewComments = findViewById(R.id.recycler_view_comments);
        textNoComments = findViewById(R.id.text_no_comments);
        
        // 初始化子任务列表
        recyclerViewSubtasks.setLayoutManager(new LinearLayoutManager(this));
        subtaskAdapter = new SubtaskAdapter(this);
        subtaskAdapter.setOnSubtaskClickListener(this);
        recyclerViewSubtasks.setAdapter(subtaskAdapter);
        
        // 初始化评论列表
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this);
        recyclerViewComments.setAdapter(commentAdapter);
        
        // 设置事件监听
        btnCopyTitle.setOnClickListener(v -> copyTitleToClipboard());
        btnCreateSubtask.setOnClickListener(v -> showCreateSubtaskDialog());
        btnLinkParentTask.setOnClickListener(v -> showParentTaskSelectorDialog());
        btnSendComment.setOnClickListener(v -> sendComment());
        
        cardParentTask.setOnClickListener(v -> {
            if (task != null && task.getParentId() != null) {
                Intent intent = new Intent(this, TaskDetailActivity.class);
                intent.putExtra(EXTRA_TASK_ID, task.getParentId());
                startActivity(intent);
            }
        });
        
        editDetailDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (task != null) {
                    String currentDesc = editDetailDescription.getText().toString();
                    String originalDesc = task.getDescription() != null ? task.getDescription() : "";
                    pendingDescription = currentDesc.equals(originalDesc) ? null : currentDesc;
                    updateSaveButtonState();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusNames);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (task != null) {
                    int selectedStatusId = statusIds[position];
                    pendingStatusId = (selectedStatusId != task.getStatus()) ? selectedStatusId : null;
                    updateSaveButtonState();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        spinnerProject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (projectList != null && position < projectList.size() && task != null) {
                    Project selectedProject = projectList.get(position);
                    Long selectedProjectId = (selectedProject == null) ? null : selectedProject.getId();
                    Long currentProjectId = task.getProjectId();
                    boolean same = (currentProjectId == null && selectedProjectId == null) || 
                                   (currentProjectId != null && currentProjectId.equals(selectedProjectId));
                    pendingProjectId = same ? null : selectedProjectId;
                    updateSaveButtonState();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        btnSaveChanges.setOnClickListener(v -> saveAllChanges());
        findViewById(R.id.btn_move_project).setOnClickListener(v -> showMoveProjectDialog());
        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteConfirm());
    }
    
    /**
     * 发表评论
     */
    private void sendComment() {
        if (task == null) return;
        
        String content = editCommentInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 在后台线程保存评论
        Executors.newSingleThreadExecutor().execute(() -> {
            Comment comment = new Comment(taskId, content, "用户");
            long commentId = taskViewModel.getAppDatabase().commentDao().insert(comment);
            
            runOnUiThread(() -> {
                if (commentId > 0) {
                    Toast.makeText(TaskDetailActivity.this, "评论已发表", Toast.LENGTH_SHORT).show();
                    editCommentInput.setText("");
                    // 重新加载评论列表
                    loadComments();
                } else {
                    Toast.makeText(TaskDetailActivity.this, "评论发表失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    /**
     * 加载评论列表
     */
    private void loadComments() {
        taskViewModel.getAppDatabase().commentDao().getCommentsByIssueIdLive(taskId).observe(this, comments -> {
            updateCommentList(comments);
        });
    }
    
    /**
     * 更新评论列表 UI
     */
    private void updateCommentList(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            textCommentsTitle.setVisibility(View.GONE);
            recyclerViewComments.setVisibility(View.GONE);
            textNoComments.setVisibility(View.VISIBLE);
            commentAdapter.clear();
        } else {
            textCommentsTitle.setVisibility(View.VISIBLE);
            recyclerViewComments.setVisibility(View.VISIBLE);
            textNoComments.setVisibility(View.GONE);
            commentAdapter.setComments(comments);
        }
    }
    
    private void updateSaveButtonState() {
        boolean hasChanges = (pendingStatusId != null || pendingProjectId != null || 
                             pendingDescription != null || pendingParentId != null);
        btnSaveChanges.setEnabled(hasChanges);
        btnSaveChanges.setAlpha(hasChanges ? 1.0f : 0.5f);
        
        if (hasChanges) {
            List<String> changes = new ArrayList<>();
            if (pendingStatusId != null) changes.add("状态");
            if (pendingProjectId != null) changes.add("项目");
            if (pendingDescription != null) changes.add("描述");
            if (pendingParentId != null) changes.add("父任务");
            ((TextView) btnSaveChanges).setText("保存更改：" + String.join(",", changes));
        } else {
            ((TextView) btnSaveChanges).setText("保存更改");
        }
    }
    
    private void copyTitleToClipboard() {
        if (task == null) return;
        String title = task.getTitle();
        if (title == null || title.isEmpty()) return;
        
        // 添加任务编号前缀
        StringBuilder copyContent = new StringBuilder();
        copyContent.append("#").append(task.getId()).append(" ");
        if (task.getProjectId() != null && projectList != null) {
            for (Project p : projectList) {
                if (p != null && p.getId() == task.getProjectId()) {
                    copyContent.append("[").append(p.getName()).append("] ");
                    break;
                }
            }
        }
        copyContent.append(title);
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            copyContent.append("\n\n").append(task.getDescription());
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("JoyMan 任务", copyContent.toString()));
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showParentTaskSelectorDialog() {
        if (task == null) return;
        
        // 在后台线程查询数据库，避免阻塞主线程
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Task> availableTasks = new ArrayList<>();
            List<Task> allTasks = taskViewModel.getTaskDao().getAllTasks();
            
            if (allTasks != null) {
                Long currentProjectId = task.getProjectId();
                for (Task t : allTasks) {
                    if (t.getId() != taskId) {
                        boolean isOpen = (t.getStatus() == Task.STATUS_NEW || t.getStatus() == Task.STATUS_IN_PROGRESS);
                        boolean sameProject = (currentProjectId == null && t.getProjectId() == null) ||
                                             (currentProjectId != null && currentProjectId.equals(t.getProjectId())) ||
                                             (currentProjectId == null);
                        if (isOpen && sameProject) availableTasks.add(t);
                    }
                }
            }
            
            // 切换回主线程更新 UI
            runOnUiThread(() -> {
                if (availableTasks.isEmpty()) {
                    Toast.makeText(this, "没有可选的上级任务", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_parent_task_selector, null);
                
                EditText editSearch = dialogView.findViewById(R.id.edit_parent_task_search);
                RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_view_parent_tasks);
                TextView textNoTasks = dialogView.findViewById(R.id.text_no_parent_tasks);
                
                List<Task> filteredTasks = new ArrayList<>(availableTasks);
                ParentTaskAdapter adapter = new ParentTaskAdapter(filteredTasks, taskId);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
                
                editSearch.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String keyword = s.toString().toLowerCase().trim();
                        filteredTasks.clear();
                        if (keyword.isEmpty()) {
                            filteredTasks.addAll(availableTasks);
                        } else {
                            for (Task t : availableTasks) {
                                if (String.valueOf(t.getId()).contains(keyword) || 
                                    (t.getTitle() != null && t.getTitle().toLowerCase().contains(keyword))) {
                                    filteredTasks.add(t);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        recyclerView.setVisibility(filteredTasks.isEmpty() ? View.GONE : View.VISIBLE);
                        textNoTasks.setVisibility(filteredTasks.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                
                builder.setTitle("选择上级任务")
                    .setView(dialogView)
                    .setPositiveButton("清除父任务", (dialog, which) -> {
                        pendingParentId = -1L;
                        updateSaveButtonState();
                        Toast.makeText(this, "已清除父任务", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null);
                
                AlertDialog dialog = builder.create();
                dialog.show();
                
                adapter.setOnTaskClickListener(selectedTask -> {
                    pendingParentId = selectedTask.getId();
                    updateSaveButtonState();
                    Toast.makeText(this, "已选择：" + selectedTask.getTitle(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });
        });
    }
    
    private void loadParentTaskInfo() {
        if (task == null) {
            cardParentTask.setVisibility(View.GONE);
            btnLinkParentTask.setVisibility(View.GONE);
            return;
        }
        
        if (task.getParentId() == null) {
            cardParentTask.setVisibility(View.GONE);
            btnLinkParentTask.setVisibility(View.VISIBLE);
        } else {
            btnLinkParentTask.setVisibility(View.GONE);
            loadParentTask();
        }
    }
    
    private void loadParentTask() {
        if (task == null || task.getParentId() == null) {
            cardParentTask.setVisibility(View.GONE);
            return;
        }
        
        taskViewModel.getTaskById(task.getParentId()).observe(this, parentTask -> {
            if (parentTask != null) {
                textParentTaskTitle.setText(parentTask.getTitle());
                cardParentTask.setVisibility(View.VISIBLE);
            } else {
                cardParentTask.setVisibility(View.GONE);
            }
        });
    }
    
    private void showCreateSubtaskDialog() {
        if (task == null) return;
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        EditText editTextTitle = new EditText(this);
        editTextTitle.setHint("输入子任务标题");
        editTextTitle.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(editTextTitle);
        
        Spinner spinnerProject = new Spinner(this);
        spinnerProject.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        List<String> projectNames = new ArrayList<>();
        List<Long> projectIds = new ArrayList<>();
        projectNames.add("无项目");
        projectIds.add(null);
        
        if (projectList != null) {
            for (int i = 0; i < projectList.size(); i++) {
                Project p = projectList.get(i);
                if (i == 0) continue;
                if (p != null) {
                    projectNames.add(p.getIconDisplay() + " " + p.getName());
                    projectIds.add(p.getId());
                }
            }
        }
        
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, projectNames);
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(projectAdapter);
        
        if (task.getProjectId() != null) {
            for (int i = 0; i < projectIds.size(); i++) {
                if (projectIds.get(i) != null && projectIds.get(i).equals(task.getProjectId())) {
                    spinnerProject.setSelection(i);
                    break;
                }
            }
        }
        
        layout.addView(spinnerProject);
        
        new AlertDialog.Builder(this)
            .setTitle("创建子任务")
            .setMessage("父任务：" + task.getTitle())
            .setView(layout)
            .setPositiveButton("创建", (dialog, which) -> {
                String title = editTextTitle.getText().toString().trim();
                if (!title.isEmpty()) {
                    int pos = spinnerProject.getSelectedItemPosition();
                    Long projectId = (pos > 0 && pos < projectIds.size()) ? projectIds.get(pos) : null;
                    
                    long subtaskId = taskViewModel.createTask(title);
                    Task subtask = new Task(subtaskId, title);
                    subtask.setProjectId(projectId);
                    subtask.setParentId(taskId);
                    taskViewModel.update(subtask);
                    
                    Toast.makeText(this, "子任务已创建", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "任务标题不能为空", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void loadSubtasks() {
        taskViewModel.getTaskDao().getSubtasksByParentIdLive(taskId).observe(this, subtasks -> {
            updateSubtaskList(subtasks);
        });
    }
    
    private void updateSubtaskList(List<Task> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            textSubtasksTitle.setVisibility(View.GONE);
            recyclerViewSubtasks.setVisibility(View.GONE);
            textNoSubtasks.setVisibility(View.VISIBLE);
            subtaskAdapter.setSubtasks(new ArrayList<>());
        } else {
            textSubtasksTitle.setVisibility(View.VISIBLE);
            recyclerViewSubtasks.setVisibility(View.VISIBLE);
            textNoSubtasks.setVisibility(View.GONE);
            subtaskAdapter.setSubtasks(subtasks);
        }
    }
    
    @Override
    public void onSubtaskClick(Task subtask) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(EXTRA_TASK_ID, subtask.getId());
        startActivity(intent);
    }
    
    private void loadTask() {
        taskViewModel.getTaskById(taskId).observe(this, task -> {
            if (task == null) {
                Toast.makeText(this, "任务不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            this.task = task;
            pendingStatusId = null;
            pendingProjectId = null;
            pendingDescription = null;
            pendingParentId = null;
            
            updateUI();
            updateSaveButtonState();
            loadParentTaskInfo();
        });
    }
    
    private void loadProjects() {
        projectViewModel.getAllProjects().observe(this, projects -> {
            if (isDestroyed()) return;
            
            projectList = new ArrayList<>();
            List<String> projectNames = new ArrayList<>();
            projectList.add(null);
            projectNames.add("无项目");
            
            if (projects != null) {
                for (Project project : projects) {
                    projectList.add(project);
                    projectNames.add(project.getIconDisplay() + " " + project.getName());
                }
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, projectNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProject.setAdapter(adapter);
        });
    }
    
    private void saveAllChanges() {
        if (task == null) return;
        
        boolean hasChanges = false;
        List<String> savedItems = new ArrayList<>();
        
        if (pendingStatusId != null) {
            task.setStatus(pendingStatusId);
            savedItems.add("状态：" + Task.getStatusNameById(pendingStatusId));
            pendingStatusId = null;
            hasChanges = true;
        }
        
        if (pendingProjectId != null) {
            task.setProjectId(pendingProjectId);
            savedItems.add("项目");
            pendingProjectId = null;
            hasChanges = true;
        }
        
        if (pendingDescription != null) {
            task.setDescription(pendingDescription);
            savedItems.add("描述");
            pendingDescription = null;
            hasChanges = true;
        }
        
        if (pendingParentId != null) {
            if (pendingParentId == -1L) {
                task.setParentId(null);
                savedItems.add("清除父任务");
            } else {
                task.setParentId(pendingParentId);
                savedItems.add("设置父任务");
            }
            pendingParentId = null;
            hasChanges = true;
        }
        
        if (!hasChanges) {
            Toast.makeText(this, "没有需要保存的更改", Toast.LENGTH_SHORT).show();
            return;
        }
        
        taskViewModel.update(task);
        Toast.makeText(this, "已保存：" + String.join(",", savedItems), Toast.LENGTH_SHORT).show();
        
        updateUI();
        updateSaveButtonState();
        loadParentTaskInfo();
    }
    
    private void updateUI() {
        if (task == null) return;
        
        textDetailId.setText("ID: " + task.getId());
        textTitle.setText(task.getTitle());
        editDetailDescription.setText(task.getDescription() != null ? task.getDescription() : "");
        
        String statusText = task.getStatusText();
        textStatus.setText(statusText);
        int colorIndex = task.getStatus() - Task.STATUS_NEW;
        if (colorIndex >= 0 && colorIndex < statusColors.length) {
            textStatus.setTextColor(statusColors[colorIndex]);
        }
        
        textPriority.setText("优先级：" + task.getPriorityText());
        
        Long projectId = task.getProjectId();
        if (projectId != null) {
            projectViewModel.getAllProjects().observe(this, projects -> {
                if (projects != null) {
                    for (Project p : projects) {
                        if (p.getId() == projectId) {
                            textProject.setText("所属项目：" + p.getIconDisplay() + " " + p.getName());
                            break;
                        }
                    }
                }
            });
        } else {
            textProject.setText("所属项目：无");
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        textCreatedAt.setText("创建时间：" + sdf.format(new Date(task.getCreatedAt())));
        
        int currentStatusId = task.getStatus();
        for (int i = 0; i < statusIds.length; i++) {
            if (statusIds[i] == currentStatusId) {
                spinnerStatus.setSelection(i);
                break;
            }
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
                    boolean same = (currentProjectId == null && selectedProjectId == null) ||
                                   (currentProjectId != null && currentProjectId.equals(selectedProjectId));
                    pendingProjectId = same ? null : selectedProjectId;
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
        super.onDestroy();
    }
}