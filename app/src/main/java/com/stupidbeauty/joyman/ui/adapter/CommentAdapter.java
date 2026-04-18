package com.stupidbeauty.joyman.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stupidbeauty.joyman.R;
import com.stupidbeauty.joyman.data.database.entity.Comment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 评论列表适配器
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-04-18
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    
    private List<Comment> commentList;
    private Context context;
    private SimpleDateFormat dateFormat;
    
    public CommentAdapter(Context context) {
        this.context = context;
        this.commentList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        
        // 设置作者
        holder.textAuthor.setText(comment.getAuthor());
        
        // 设置内容
        holder.textContent.setText(comment.getContent());
        
        // 设置时间
        String timeStr = dateFormat.format(new Date(comment.getCreatedOn()));
        holder.textTime.setText(timeStr);
    }
    
    @Override
    public int getItemCount() {
        return commentList.size();
    }
    
    /**
     * 设置评论列表
     * @param comments 评论列表
     */
    public void setComments(List<Comment> comments) {
        this.commentList = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * 添加单条评论
     * @param comment 评论对象
     */
    public void addComment(Comment comment) {
        this.commentList.add(0, comment); // 添加到开头（最新的在前）
        notifyItemInserted(0);
    }
    
    /**
     * 清空评论列表
     */
    public void clear() {
        int size = commentList.size();
        commentList.clear();
        if (size > 0) {
            notifyItemRangeRemoved(0, size);
        }
    }
    
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView textAuthor;
        TextView textContent;
        TextView textTime;
        
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            textAuthor = itemView.findViewById(R.id.text_comment_author);
            textContent = itemView.findViewById(R.id.text_comment_content);
            textTime = itemView.findViewById(R.id.text_comment_time);
        }
    }
}