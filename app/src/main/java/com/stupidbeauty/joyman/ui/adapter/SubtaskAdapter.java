package com.stupidbeauty.joyman.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.stupidbeauty.joyman.R;
import com.stupidbeauty.joyman.data.database.entity.Task;

import java.util.ArrayList;
import java.util.List;


/**
 * 子任务列表适配器
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-04-05
 */
public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder> {
    
    private Context context;
    private List<Task> subtaskList = new ArrayList<>();
    private OnSubtaskClickListener listener;
    
    public SubtaskAdapter(Context context) {
        this.context = context;
    }
    
    public void setSubtasks(List<Task> subtasks) {
        this.subtaskList.clear();
        if (subtasks != null) {
            this.subtaskList.addAll(subtasks);
        }
        notifyDataSetChanged();
    }
    
    public void setOnSubtaskClickListener(OnSubtaskClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public SubtaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_subtask, parent, false);
        return new SubtaskViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SubtaskViewHolder holder, int position) {
        Task subtask = subtaskList.get(position);
        holder.bind(subtask, listener);
    }
    
    @Override
    public int getItemCount() {
        return subtaskList.size();
    }
    
    /**
     * 子任务项点击监听器
     */
    public interface OnSubtaskClickListener {
        void onSubtaskClick(Task subtask);
    }
    
    /**
     * 子任务列表项 ViewHolder
     */
    static class SubtaskViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textStatus;
        TextView textPriority;
        
        SubtaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_subtask_title);
            textStatus = itemView.findViewById(R.id.text_subtask_status);
            textPriority = itemView.findViewById(R.id.text_subtask_priority);
        }
        
        void bind(Task subtask, OnSubtaskClickListener listener) {
            textTitle.setText(subtask.getTitle());
            textStatus.setText(subtask.getStatusText());
            textPriority.setText("优先级：" + subtask.getPriorityText());
            
            // 设置状态颜色
            int colorIndex = subtask.getStatus() - Task.STATUS_NEW;
            int[] statusColors = new int[]{
                ContextCompat.getColor(itemView.getContext(), R.color.status_new),
                ContextCompat.getColor(itemView.getContext(), R.color.status_in_progress),
                ContextCompat.getColor(itemView.getContext(), R.color.status_resolved),
                ContextCompat.getColor(itemView.getContext(), R.color.status_feedback),
                ContextCompat.getColor(itemView.getContext(), R.color.status_closed)
            };
            
            if (colorIndex >= 0 && colorIndex < statusColors.length) {
                textStatus.setTextColor(statusColors[colorIndex]);
            } else {
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubtaskClick(subtask);
                }
            });
        }
    }
}