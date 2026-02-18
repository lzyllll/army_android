package com.example.demo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.ui.model.UserAccount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DonateAdapter extends RecyclerView.Adapter<DonateAdapter.ViewHolder> {

    private List<UserAccount.GameUserData> gameUsers = new ArrayList<>();
    private final Set<Integer> selectedPositions = new HashSet<>();
    private OnSelectionChangedListener selectionListener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public void setGameUsers(List<UserAccount.GameUserData> gameUsers) {
        this.gameUsers = gameUsers != null ? gameUsers : new ArrayList<>();
        selectedPositions.clear();
        // 默认选中所有启用的
        for (int i = 0; i < this.gameUsers.size(); i++) {
            if (this.gameUsers.get(i).enabled && this.gameUsers.get(i).selected) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public void selectAll(boolean select) {
        selectedPositions.clear();
        if (select) {
            for (int i = 0; i < gameUsers.size(); i++) {
                if (gameUsers.get(i).enabled) {
                    selectedPositions.add(i);
                }
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public List<UserAccount.GameUserData> getSelectedGameUsers() {
        List<UserAccount.GameUserData> result = new ArrayList<>();
        for (int pos : selectedPositions) {
            if (pos >= 0 && pos < gameUsers.size()) {
                result.add(gameUsers.get(pos));
            }
        }
        return result;
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedPositions.size());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_donate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserAccount.GameUserData gu = gameUsers.get(position);
        boolean isSelected = selectedPositions.contains(position);

        holder.tvTitle.setText(gu.title);
        holder.tvArchiveIndex.setText("S" + gu.archIndex);
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(isSelected);

        // Local contribution status check
        boolean isDonatedToday = gu.lastDonateTime > 0 && android.text.format.DateUtils.isToday(gu.lastDonateTime);

        if (isDonatedToday) {
            holder.tvDailyContribution.setText("1100");
            holder.progressBar.setProgress(100);
            holder.tvProgress.setText("100%");
            holder.tvStatus.setText("已完成");
            holder.tvStatus.setTextColor(0xFF4CAF50);
        } else {
            holder.tvDailyContribution.setText("0"); // Or "Unknown"
            holder.progressBar.setProgress(0);
            holder.tvProgress.setText("0%");
            holder.tvStatus.setText("未贡献");
            holder.tvStatus.setTextColor(0xFF999999);
        }

        // 日志
        if (gu.lastActionLog != null && !gu.lastActionLog.isEmpty()) {
            holder.tvLog.setVisibility(View.VISIBLE);
            holder.tvLog.setText(gu.lastActionLog);
        } else {
            holder.tvLog.setVisibility(View.GONE);
        }

        // 选择事件
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (isChecked) {
                    selectedPositions.add(adapterPosition);
                } else {
                    selectedPositions.remove(adapterPosition);
                }
                notifySelectionChanged();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            holder.checkbox.toggle();
        });
    }

    private String formatNumber(int num) {
        if (num >= 1000) {
            return String.format("%.1fK", num / 1000.0);
        }
        return String.valueOf(num);
    }

    @Override
    public int getItemCount() {
        return gameUsers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        TextView tvTitle;
        TextView tvArchiveIndex;
        TextView tvDailyContribution;
        TextView tvProgress;
        TextView tvStatus;
        TextView tvLog;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArchiveIndex = itemView.findViewById(R.id.tvArchiveIndex);
            tvDailyContribution = itemView.findViewById(R.id.tvDailyContribution);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvLog = itemView.findViewById(R.id.tvLog);
        }
    }
}
