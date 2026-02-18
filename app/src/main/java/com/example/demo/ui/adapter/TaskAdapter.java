package com.example.demo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.pojo.GameUser;
import com.example.demo.thrift.Task;
import com.example.demo.thrift.TasksInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<GameUser.TaskRequirement> tasks;
    private Map<String, Integer> currentValues = new HashMap<>();

    public TaskAdapter() {
        this.tasks = GameUser.getRequiredTasks();
    }

    public void setTasksInfo(TasksInfo info) {
        currentValues.clear();
        if (info != null && info.tasks != null) {
            for (Task t : info.tasks) {
                try {
                    currentValues.put(t.id, Integer.parseInt(t.value));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameUser.TaskRequirement task = tasks.get(position);
        int current = currentValues.getOrDefault(task.id, 0);

        holder.tvTaskName.setText(task.name);
        holder.tvTaskProgress.setText(current + "/" + task.max);

        holder.progressBar.setMax(task.max);
        holder.progressBar.setProgress(current);

        if (current >= task.max) {
            holder.tvTaskStatus.setText("已完成");
            holder.tvTaskStatus.setTextColor(0xFF4CAF50); // Green
        } else {
            holder.tvTaskStatus.setText("进行中");
            holder.tvTaskStatus.setTextColor(0xFFFA8C16); // Orange
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName;
        TextView tvTaskProgress;
        ProgressBar progressBar;
        TextView tvTaskStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTaskProgress = itemView.findViewById(R.id.tvTaskProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);

        }
    }
}
