package com.example.demo.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.ui.model.ApplyUserData;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApplyAdapter extends RecyclerView.Adapter<ApplyAdapter.ViewHolder> {

    private List<ApplyUserData> applyList = new ArrayList<>();
    private final Set<Integer> selectedPositions = new HashSet<>();
    private OnApplyActionListener listener;

    public interface OnApplyActionListener {
        void onApprove(ApplyUserData apply);
        void onReject(ApplyUserData apply);
        void onSelectionChanged(int count);
    }

    public void setApplyList(List<ApplyUserData> applyList) {
        this.applyList = applyList != null ? applyList : new ArrayList<>();
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    public void setOnApplyActionListener(OnApplyActionListener listener) {
        this.listener = listener;
    }

    public void selectAll(boolean select) {
        selectedPositions.clear();
        if (select) {
            for (int i = 0; i < applyList.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public List<ApplyUserData> getSelectedApplies() {
        List<ApplyUserData> result = new ArrayList<>();
        for (int pos : selectedPositions) {
            if (pos >= 0 && pos < applyList.size()) {
                result.add(applyList.get(pos));
            }
        }
        return result;
    }

    public void removeApply(ApplyUserData apply) {
        int index = applyList.indexOf(apply);
        if (index >= 0) {
            applyList.remove(index);
            selectedPositions.remove(index);
            notifyItemRemoved(index);
            notifySelectionChanged();
        }
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(selectedPositions.size());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_apply, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplyUserData apply = applyList.get(position);
        boolean isSelected = selectedPositions.contains(position);

        // 使用 detail.playerName
        holder.tvPlayerName.setText(apply.getPlayerName());

        // 等级
        holder.tvLevel.setText(String.valueOf(apply.getLevel()));

        // 战力
        holder.tvPower.setText(formatNumber(apply.getDps()));

        //登录时间
        holder.tvTime.setText(apply.getLoginTime());
        // VIP
        String vipStr = apply.getVip();
        if (vipStr != null && !vipStr.isEmpty()) {
            holder.tvVip.setVisibility(View.VISIBLE);
            holder.tvVip.setText(vipStr);
            // VIP 颜色根据等级变化
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(4f);
            try {
                int vip = Integer.parseInt(vipStr);
                if (vip >= 8) {
                    bg.setColor(Color.parseColor("#FF9800"));
                } else if (vip >= 4) {
                    bg.setColor(Color.parseColor("#2196F3"));
                } else {
                    bg.setColor(Color.parseColor("#9E9E9E"));
                }
            } catch (NumberFormatException e) {
                bg.setColor(Color.parseColor("#9E9E9E"));
            }
            holder.tvVip.setBackground(bg);
        } else {
            holder.tvVip.setVisibility(View.GONE);
        }

        // 选择框
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(isSelected);
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

        // 通过按钮
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(apply);
            }
        });

        // 拒绝按钮
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(apply);
            }
        });
    }

    private String formatNumber(double num) {
        if (num >= 1000000) {
            return String.format("%.1fM", num / 1000000.0);
        } else if (num >= 1000) {
            return String.format("%.1fK", num / 1000.0);
        }
        return String.valueOf((long) num);
    }

    @Override
    public int getItemCount() {
        return applyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        TextView tvPlayerName;
        TextView tvVip;
        TextView tvLevel;
        TextView tvPower;
        TextView tvTime;
        MaterialButton btnApprove;
        MaterialButton btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvVip = itemView.findViewById(R.id.tvVip);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvPower = itemView.findViewById(R.id.tvPower);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
