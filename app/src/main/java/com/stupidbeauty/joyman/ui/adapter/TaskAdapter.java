package com.stupidbeauty.joyman.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.stupidbeauty.joyman.R;
import com.stupidbeauty.joyman.data.database.entity.Task;

import java.util.List;

/**
 * 任务列表适配器
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private List<Task> taskList;
    private OnTaskClickListener listener;
    private Context context;

    /**
     * 任务点击监听接口
     */
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
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
     * ViewHolder 类
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textTitle;
        private TextView textDescription;
        private TextView textPriority;
        private TextView textStatus;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_task_title);
            textDescription = itemView.findViewById(R.id.text_task_description);
            textPriority = itemView.findViewById(R.id.text_task_priority);
            textStatus = itemView.findViewById(R.id.text_task_status);
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskClick(taskList.get(position));
                }
            });
        }

        public void bind(Task task) {
            textTitle.setText(task.getTitle());
            
            // 描述截断显示
            String desc = task.getDescription();
            if (desc != null && desc.length() > 50) {
                desc = desc.substring(0, 50) + "...";
            }
            textDescription.setText(desc != null ? desc : "");
            
            // 优先级
            textPriority.setText(task.getPriorityText());
            setPriorityColor(task.getPriority());
            
            // 状态
            textStatus.setText(task.getStatusText());
        }

        /**
         * 根据优先级设置颜色
         */
        private void setPriorityColor(int priority) {
            int color;
            switch (priority) {
                case Task.PRIORITY_LOW:
                    color = context.getResources().getColor(android.R.color.darker_gray);
                    break;
                case Task.PRIORITY_NORMAL:
                    color = context.getResources().getColor(android.R.color.black);
                    break;
                case Task.PRIORITY_HIGH:
                    color = context.getResources().getColor(android.R.color.holo_orange_dark);
                    break;
                case Task.PRIORITY_URGENT:
                    color = context.getResources().getColor(android.R.color.holo_red_dark);
                    break;
                default:
                    color = context.getResources().getColor(android.R.color.black);
            }
            textPriority.setTextColor(color);
        }
    }
}
