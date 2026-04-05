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
import com.stupidbeauty.joyman.viewModel.ProjectViewModel;
import com.stupidbeauty.joyman.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * ‰ªªÂä°ËØ¶ÊÉÖÁïåÈù¢
 * 
 * @author Â§™ÊûÅÁæéÊúØÂÖ•Á®ãÁ©∫Áîü
 * @version 1.0.12
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
    private Long pendingProjectId;
    private Integer pendingStatusId;
    
    private int[] statusIds;
    private String[] statusNames;
    private int[] statusColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("‰ªªÂä°ËØ¶ÊÉ¢");
        }
        
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0);
        if (taskId == 0) {
            Toast.makeText(this, "Êó†ÊïàÁöÑ‰ªªÂä° ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        
        initStatusData();
        initViews();
        loadTask();
        loadProjects();
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
        textDescription = findViewById(R.id.text_detail_description);
        textStatus = findViewById(R.id.text_detail_status);
        textPriority = findViewById(R.id.text_detail_priority);
        textProject = findViewById(R.id.text_detail_project);
        textCreatedAt = findViewById(R.id.text_detail_created_at);
        btnCopyTitle = findViewById(R.id.btn_copy_title);
        spinnerProject = findViewById(R.id.spinner_detail_project);
        spinnerStatus = findViewById(R.id.spinner_status);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        
        btnCopyTitle.setOnClickListener(v -> copyTitleToClipboard());
        
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusNames);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (task != null) {
                    int selectedStatusId = statusIds[position];
                    if (selectedStatusId != task.getStatus()) {
                        pendingStatusId = selectedStatusId;
                    } else {
                        pendingStatusId = null;
                    }
                    updateSaveButtonState();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
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
                        }
                        updateSaveButtonState();
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        btnSaveChanges.setOnClickListener(v -> saveAllChanges());
        findViewById(R.id.btn_move_project).setOnClickListener(v -> showMoveProjectDialog());
        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteConfirm());
    }
    
    private void updateSaveButtonState() {
        boolean hasChanges = (pendingStatusId != null || pendingProjectId != null);
        btnSaveChanges.setEnabled(hasChanges);
        btnSaveChanges.setAlpha(hasChanges ? 1.0f : 0.5f);
        
        if (hasChanges) {
            StringBuilder hint = new StringBuilder("‰øùÂ≠òÊõ¥ÊîπÔºö");
            List<String> changes = new ArrayList<>();
            if (pendingStatusId != null) changes.add(Áä∂ÊÄÅ");
            if (pendingProjectId != null) changes.add("È°πÁõÆ");
            hint.append(String.join("Ôºå ", changes));
            ((TextView) btnSaveChanges).setText(hint.toString());
        } else {
            ((TextView) btnSaveChanges).setText("‰øùÂ≠òÊõ¥Êîπ");
        }
    }
    
    /**
     * È§çÂà∂‰ªªÂä°Ê†áÈ¢úÂà∞Ââ™Ë∞êÊùøÊàëÈÄö
     */
    private void copyTitleToClipboard() {
        if (task == null) {
            Toast.makeText(this, "‰ªªÂä°Êï∞ÊçÆÊö™Âä†ËΩΩ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String title = task.getTitle();
        if (title == null || title.isEmpty()) {
            Toast.makeText(this, "‰ªªÂä°Ê†áÈ¢ò‰∏∫Á©∫", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Âª∫‰∫∫ÊñπÂ∞ëÈ°πÁõÆ ËæìÊûü] ‰ªªÂä°
        String copyContent = title;
        if (task.getProjectId() != null && projectList != null) {
            long targetProjectId = task.getProjectId();
            for (Project p : projectList) {
                if (p != null && p.getId() == targetProjectId) {
                    copyContent = "[" + p.getName() + "] " + title;
                    break;
                }
            }
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "Êó†Ê≥ïËØØÊï∞Êãñ‰∫∫", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipData clip = ClipData.newPlainText("JoyMan ‰ªªÂä°Ê†áÈ¢ú", copyContent);
        clipboard.setPrimaryClip(clip);
        
        String toastMessage = "Â∑≤‰øù:" + copyContent;
        if (toastMessage.length() > 50) {
            toastMessage = toastMessage.substring(0, 47) + "...";
        }
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }
    
    private void loadTask() {
        taskViewModel.getTaskById(taskId).observe(this, task -> {
            if (task == null) {
                Toast.makeText(this,"‰ªªÂä°‰∏çÂÆò", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            this.task = task;
            pendingStatusId = null;
            pendingProjectId = null;
            updateUI();
            updateSaveButtonState();
        });
    }
    
    private void loadProjects() {
        projectViewModel.getAllProjects().observe(this, projects -> {
            if (isDestroyed()) return;
            
            projectList = new ArrayList<>();
            List<String> projectNames = new ArrayList<>();
            
            projectList.add(null);
            projectNames.add("Êó†È°πÁõÆ");
            
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
        if (task == null) {
            Toast.makeText(this, "‰ªªÂä°Êï∞ÊçÆÊö™Âä†ËΩΩ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean hasChanges = false;
        List<String> savedItems = new ArrayList<>();
        
        if (pendingStatusId != null) {
            task.setStatus(pendingStatusId);
            savedItems.add("Áä∂ÊÄÅÔºö" + Task.getStatusNameById(pendingStatusId));
            pendingStatusId = null;
            hasChanges = true;
        }
        
        if (pendingProjectId != null) {
            task.setProjectId(pendingProjectId);
            String projectName = "Êú™Áüß";
            if (projectList != null) {
                for (Project p : projectList) {
                    if (p != null && p.getId() == pendingProjectId) {
                        projectName = p.getName();
                        break;
                    }
                }
            }
            savedItems.add("È°πÁõÆÔºö" + projectName);
            pendingProjectId = null;
            hasChanges = true;
        }
        
        if (!hasChanges) {
            Toast.makeText(this, "Ê≤°ÊúâÈúÄË¶πÂú∞ÈÉ∏Ê°Ü", Toast.LENGTH_SHORT).show();
            return;
        }
        
        taskViewModel.update(task);
        Toast.makeText(this, "Â∑≤‰øùÂ≠òÔºö" + String.join(",", savedItems), Toast.LENGTH_SHORT).show();
        updateUI();
        updateSaveButtonState();
    }
    
    private void updateUI() {
        if (task == null) return;
        
        textTitle.setText(task.getTitle());
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            textDescription.setText(task.getDescription());
            textDescription.setVisibility(View.VISIBLE);
        } else {
            textDescription.setVisibility(View.GONE);
        }
        
        updateStatusUI();
        textPriority.setText("‰ºòÂÖàÁ∫ßÔºö" + task.getPriorityText());
        
        Long projectId = task.getProjectId();
        if (projectId != null) {
            projectViewModel.getAllProjects().observe(this, projects -> {
                boolean found = false;
                if (projects != null) {
                    for (Project p : projects) {
                        if (p.getId() == projectId) {
                            textProject.setText("ÊâÄÂ±ûÈ°πÁõÆÔºö" + p.getIconDisplay() + " " + p.getName());
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
                    textProject.setText("ÊâÄÂ±ûÈ°πÁõÆÔºöÊú¶Áü•È°πÁõÆ (ID: " + projectId + ")");
                }
            });
        } else {
            textProject.setText("ÊâÄÂ±ûÈ°πÁõÆÔºöÊú¨");
            if (projectList != null) spinnerProject.setSelection(0);
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        textCreatedAt.setText("ÂàõÂª∫Êó∂Èó¥Ôºö" + sdf.format(new Date(task.getCreatedAt())));
        updateStatusSpinnerSelection();
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
        if (projectList == null) {
            Toast.makeText(this, "È°πÁõ¢y•Ë:hnyÊÓ9¨ÓKã¢‚‚‚"¬Fˆ7B‰ƒT‰uDÖı4Ñı%BíÁ6Ü˜rÇì∞¢&WGW&„∞¢–¢ ¢7G&ñÊuµ“&ˆ¶V7DÊ÷W2“ÊWr7G&ñÊu∑&ˆ¶V7D∆ó7BÁ6ó¶RÇï”∞¢f˜"ÜñÁBí“≤í¬&ˆ¶V7D∆ó7BÁ6ó¶RÇì≤í≤≤í∞¢&ˆ¶V7B“&ˆ¶V7D∆ó7BÊvWBÜíì∞¢&ˆ¶V7DÊ÷W5∂ï““á”“ÁV∆¬íÚ.izöûy∫‚"¢áÊvWDñ6ˆ‰Fó7∆íÇí≤""≤ÊvWDÊ÷RÇíì∞¢–¢ ¢ñÁB7W'&VÁDñÊFWÇ“∞¢ñbáF6≤“ÁV∆¬bbF6≤ÊvWE&ˆ¶V7DñBÇí“ÁV∆¬í∞¢f˜"ÜñÁBí“≤í¬&ˆ¶V7D∆ó7BÁ6ó¶RÇì≤í≤≤í∞¢&ˆ¶V7B“&ˆ¶V7D∆ó7BÊvWBÜíì∞¢ñbá“ÁV∆¬bbÊvWDñBÇí”“F6≤ÊvWE&ˆ¶V7DñBÇíí∞¢7W'&VÁDñÊFWÇ“ì∞¢'&V≥∞¢–¢–¢–¢ ¢ÊWr∆W'DFñ∆ˆr‰'Vñ∆FW"áFÜó2ê¢Á6WEFóF∆RÇ.k{æX™éX´öûy∫‚"ê¢Á6WE6ñÊv∆T6Üˆñ6TóFV◊2á&ˆ¶V7DÊ÷W2¬7W'&VÁDñÊFWÇ¬ÜFñ∆ˆr¬vÜñ6Çí”‚∞¢&ˆ¶V7B6V∆V7FVE&ˆ¶V7B“&ˆ¶V7D∆ó7BÊvWBávÜñ6Çì∞¢∆ˆÊr6V∆V7FVE&ˆ¶V7DñB“á6V∆V7FVE&ˆ¶V7B”“ÁV∆¬íÚÁV∆¬¢6V∆V7FVE&ˆ¶V7BÊvWDñBÇì∞¢ ¢ñbáF6≤“ÁV∆¬í∞¢∆ˆÊr7W'&VÁE&ˆ¶V7DñB“F6≤ÊvWE&ˆ¶V7DñBÇì∞¢&ˆˆ∆V‚ó4ÁV∆ƒ&˜FÇ“Ü7W'&VÁE&ˆ¶V7DñB”“ÁV∆¬bb6V∆V7FVE&ˆ¶V7DñB”“ÁV∆¬ì∞¢&ˆˆ∆V‚ó56÷Uf«VR“Ü7W'&VÁE&ˆ¶V7DñB“ÁV∆¬bb6V∆V7FVE&ˆ¶V7DñB“ÁV∆¬bb7W'&VÁE&ˆ¶V7DñBÊWV«2á6V∆V7FVE&ˆ¶V7DñBíì∞¢ ¢ñbÜó4ÁV∆ƒ&˜FÇ«¬ó56÷Uf«VRí∞¢VÊFñÊu&ˆ¶V7DñB“ÁV∆√∞¢“V«6R∞¢VÊFñÊu&ˆ¶V7DñB“6V∆V7FVE&ˆ¶V7DñC∞¢–¢WFFU6fT'WGFˆÂ7FFRÇì∞¢–¢Fñ∆ˆrÊFó6÷ó72Çì∞¢“ê¢Á6WDÊVvFófT'WGFˆ‚Ç.˚»ŒkhÇ"¬ÁV∆¬ê¢Á6Ü˜rÇì∞¢–¢ ¢&ófFRfˆñB6Ü˜tFV∆WFT6ˆÊfó&“Çí∞¢ñbáF6≤”“ÁV∆¬í&WGW&„∞¢ÊWr∆W'DFñ∆ˆr‰'Vñ∆FW"áFÜó2ê¢Á6WEFóF∆RÇ.XäôöNKªæX™"ê¢Á6WD÷W76vRÇ.jÓZÈÆähXäôöNKªæX™¬""≤F6≤ÊvWEFóF∆RÇí≤%¬"Y	~˚…Ú"ê¢Á6WE˜6óFófT'WGFˆ‚Ç.XäôöB"¬ÜFñ∆ˆr¬vÜñ6Çí”‚∞¢F6µfñWt÷ˆFV¬ÊFV∆WFT'îñBáF6¥ñBì∞¢Fˆ7BÊ÷∂UFWáBáFÜó2¬.KªæX™[{.XäôöB"¬Fˆ7B‰ƒT‰uDÖı4Ñı%BíÁ6Ü˜rÇì∞¢fñÊó6ÇÇì∞¢“ê¢Á6WDÊVvFófT'WGFˆ‚Ç.˚»ŒkhÇ"¬ÁV∆¬ê¢Á6Ü˜rÇì∞¢–¢ ¢˜fW'&ñFP¢V&∆ñ2&ˆˆ∆V‚ˆ‰˜FñˆÁ4óFV’6V∆V7FVBÑ÷VÁTóFV“óFV“í∞¢ñbÜóFV“ÊvWDóFV‘ñBÇí”“ÊG&ˆñBÂ"ÊñBÊÜˆ÷Rí∞¢fñÊó6ÇÇì∞¢&WGW&‚G'VS∞¢–¢&WGW&‚7WW"Êˆ‰˜FñˆÁ4óFV’6V∆V7FVBÜóFV“ì∞¢–¢ ¢˜fW'&ñFP¢&˜FV7FVBfˆñBˆ‰FW7G&˜íÇí∞¢7WW"Êˆ‰FW7G&˜íÇì∞¢–ß–†