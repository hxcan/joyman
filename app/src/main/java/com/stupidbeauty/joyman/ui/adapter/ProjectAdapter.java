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
import com.stupidbeauty.joyman.data.database.entity.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Project RecyclerView 适配器
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-04-01
 */
public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {
    
    private List<Project> projectList;
    private Context context;
    private OnProjectClickListener listener;
    
    public interface OnProjectClickListener {
        void onProjectClick(Project project);
        void onProjectLongClick(Project project);
    }
    
    public ProjectAdapter(Context context, OnProjectClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.projectList = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.bind(project);
    }
    
    @Override
    public int getItemCount() {
        return projectList.size();
    }
    
    public void setProjects(List<Project> projects) {
        this.projectList.clear();
        if (projects != null) {
            this.projectList.addAll(projects);
        }
        notifyDataSetChanged();
    }
    
    public void addProject(Project project) {
        projectList.add(0, project);
        notifyItemInserted(0);
    }
    
    public void removeProject(int position) {
        if (position >= 0 && position < projectList.size()) {
            projectList.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public void updateProject(Project project) {
        for (int i = 0; i < projectList.size(); i++) {
            if (projectList.get(i).getId() == project.getId()) {
                projectList.set(i, project);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    class ProjectViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textName;
        private TextView textCount;
        private View colorIndicator;
        
        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_project_name);
            textCount = itemView.findViewById(R.id.text_project_count);
            colorIndicator = itemView.findViewById(R.id.view_color_indicator);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position >= 0 && listener != null) {
                    listener.onProjectClick(projectList.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position >= 0 && listener != null) {
                    listener.onProjectLongClick(projectList.get(position));
                    return true;
                }
                return false;
            });
        }
        
        public void bind(Project project) {
            textName.setText(project.getName());
            
            // 设置颜色指示器
            try {
                colorIndicator.setBackgroundColor(Color.parseColor(project.getColor()));
            } catch (Exception e) {
                colorIndicator.setBackgroundColor(Color.parseColor(Project.DEFAULT_COLOR));
            }
            
            // 设置图标
            if (project.hasIcon()) {
                textName.setText(project.getIconDisplay() + " " + project.getName());
            } else {
                textName.setText("📁 " + project.getName());
            }
            
            // TODO: 显示任务数量（需要后续实现）
            textCount.setVisibility(View.GONE);
        }
    }
}