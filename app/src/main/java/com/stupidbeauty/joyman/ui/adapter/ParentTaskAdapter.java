package com.stupidbeauty.joyman.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stupidbeauty.joyman.R;
import com.stupidbeauty.joyman.data.database.entity.Task;

import java.util.ArrayList;
import java.util.List;


/**
 * 父任务选择对话框中的任务列表适配器
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.1
 * @since 2026-04-07
 */
public class ParentTaskAdapter extends RecyclerView.Adapter<ParentTaskAdapter.ViewHolder> {
    
    private List<Task> tasks;
    private final long currentTaskId; // 当前任务 ID，用于排除自身
    private OnTaskClickListener listener;
    
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }
    
    public ParentTaskAdapter(List<Task> tasks, long currentTaskId) {
        this.tasks = new ArrayList<>(tasks);
        this.currentTaskId = currentTaskId;
    }
    
    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parent_task, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        
        // 格式化任务 ID 显示（截断长编号）
        String taskIdStr = String.valueOf(task.getId());
        String displayId;
        if (taskIdStr.length() > 8) {
            // 显示后 8 位
            displayId = "... " + taskIdStr.substring(taskIdStr.length() - 8);
        } else {
            displayId = "#" + taskIdStr;
        }
        
        holder.textTaskId.setText(displayId);
        holder.textTaskTitle.setText(task.getTitle());
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return tasks.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTaskId;
        TextView textTaskTitle;
        
        ViewHolder(View itemView) {
            super(itemView);
            textTaskId = itemView.findViewById(R.id.text_parent_task_id);
            textTaskTitle = itemView.findViewById(R.id.text_parent_task_title);
        }
    }
}