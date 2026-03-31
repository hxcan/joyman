package com.stupidbeauty.joyman.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stupidbeauty.joyman.R;
import com.stupidbeauty.joyman.data.database.entity.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Task RecyclerView 适配器
 * 
 * 用于在列表中展示任务项
 * 支持点击、长按、滑动等操作
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-03-31
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private List<Task> taskList;
    private Context context;
    private OnTaskClickListener listener;
    private SimpleDateFormat dateFormat;
    
    /**
     * 任务点击监听器接口
     */
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task);
        void onTaskComplete(Task task);
    }
    
    public TaskAdapter(Context context, OnTaskClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.taskList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }
    
    @Override
    public int getItemCount() {
        return taskList.size();
    }
    
    /**
     * 更新任务列表
     */
    public void setTasks(List<Task> tasks) {
        this.taskList.clear();
        if (tasks != null) {
            this.taskList.addAll(tasks);
        }
        notifyDataSetChanged();
    }
    
    /**
     * 添加单个任务
     */
    public void addTask(Task task) {
        taskList.add(0, task);
        notifyItemInserted(0);
    }
    
    /**
     * 移除任务
     */
    public void removeTask(int position) {
        if (position >= 0 && position < taskList.size()) {
            taskList.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    /**
     * 更新任务
     */
    public void updateTask(Task task) {
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId() == task.getId()) {
                taskList.set(i, task);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    /**
     * ViewHolder 类
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textTitle;
        private TextView textDescription;
        private TextView textStatus;
        private TextView textTime;
        private View priorityIndicator;
        
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_task_title);
            textDescription = itemView.findViewById(R.id.text_task_description);
            textStatus = itemView.findViewById(R.id.text_task_status);
            textTime = itemView.findViewById(R.id.text_task_time);
            priorityIndicator = itemView.findViewById(R.id.view_priority_indicator);
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position >= 0 && listener != null) {
                    listener.onTaskClick(taskList.get(position));
                }
            });
            
            // 设置长按事件
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position >= 0 && listener != null) {
                    listener.onTaskLongClick(taskList.get(position));
                    return true;
                }
                return false;
            });
            
            // 设置完成按钮点击
            textStatus.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position >= 0 && listener != null) {
                    listener.onTaskComplete(taskList.get(position));
                }
            });
        }
        
        /**
         * 绑定任务数据到视图
         */
        public void bind(Task task) {
            // 设置标题
            textTitle.setText(task.getTitle());
            
            // 设置描述
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                textDescription.setText(task.getDescription());
                textDescription.setVisibility(View.VISIBLE);
            } else {
                textDescription.setVisibility(View.GONE);
            }
            
            // 设置状态文本和颜色
            if (task.isDone()) {
                textStatus.setText("✓");
                textStatus.setTextColor(Color.parseColor("#4CAF50"));
                textTitle.setPaintFlags(textTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (task.isCancelled()) {
                textStatus.setText("✗");
                textStatus.setTextColor(Color.parseColor("#F44336"));
                textTitle.setPaintFlags(textTitle.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textStatus.setText("○");
                textStatus.setTextColor(Color.parseColor("#9E9E9E"));
                textTitle.setPaintFlags(textTitle.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }
            
            // 设置时间
            textTime.setText(dateFormat.format(new Date(task.getCreatedAt())));
            
            // 设置优先级指示器颜色
            int priorityColor;
            switch (task.getPriority()) {
                case Task.PRIORITY_URGENT:
                    priorityColor = Color.parseColor("#F44336"); // 红色
                    break;
                case Task.PRIORITY_HIGH:
                    priorityColor = Color.parseColor("#FF9800"); // 橙色
                    break;
                case Task.PRIORITY_NORMAL:
                    priorityColor = Color.parseColor("#2196F3"); // 蓝色
                    break;
                case Task.PRIORITY_LOW:
                default:
                    priorityColor = Color.parseColor("#9E9E9E"); // 灰色
                    break;
            }
            priorityIndicator.setBackgroundColor(priorityColor);
            
            // 设置过期提示
            if (task.isOverdue()) {
                textTime.setTextColor(Color.parseColor("#F44336"));
            } else {
                textTime.setTextColor(Color.parseColor("#9E9E9E"));
            }
        }
    }
}