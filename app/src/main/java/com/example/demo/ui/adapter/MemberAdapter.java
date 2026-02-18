package com.example.demo.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.pojo.ParsedMember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    private List<ParsedMember> members = new ArrayList<>();
    private int sortMode = 0; // 0: None, 1: Daily Desc, 2: Daily Asc, 3: Weekly Desc, 4: Weekly Asc

    public void setMembers(List<ParsedMember> members) {
        this.members = new ArrayList<>(members != null ? members : new ArrayList<>());
        sortMembers();
        notifyDataSetChanged();
    }

    public void sortByDaily() {
        if (sortMode == 1) {
            sortMode = 2; // Toggle to Asc
        } else {
            sortMode = 1; // Default Desc
        }
        sortMembers();
        notifyDataSetChanged();
    }

    public void sortByWeekly() {
        if (sortMode == 3) {
            sortMode = 4; // Toggle to Asc
        } else {
            sortMode = 3; // Default Desc
        }
        sortMembers();
        notifyDataSetChanged();
    }

    private void sortMembers() {
        if (sortMode == 0)
            return;

        Collections.sort(members, (a, b) -> {
            int valA = 0;
            int valB = 0;

            if (sortMode == 1 || sortMode == 2) { // Daily
                valA = a.detail != null ? a.detail.conDay : 0;
                valB = b.detail != null ? b.detail.conDay : 0;
            } else if (sortMode == 3 || sortMode == 4) { // Weekly
                valA = (a.detail != null && a.detail.conObj != null) ? a.detail.conObj.thisWeek : 0;
                valB = (b.detail != null && b.detail.conObj != null) ? b.detail.conObj.thisWeek : 0;
            }

            if (sortMode == 1 || sortMode == 3) { // Desc
                return Integer.compare(valB, valA);
            } else { // Asc
                return Integer.compare(valA, valB);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParsedMember member = members.get(position);

        // Name
        String playerName = member.detail != null && member.detail.playerName != null
                ? member.detail.playerName
                : member.nickname;
        holder.tvName.setText(playerName);
        holder.tvName.setOnClickListener(v -> {
            Context context = v.getContext();
            // Copy to clipboard
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PlayerName", playerName);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "已复制: " + playerName, Toast.LENGTH_SHORT).show();
            }

            // Toggle Expand/Collapse
            TextView tv = (TextView) v;
            // Check if currently single line (ellipsize is strictly set or maxLines=1)
            // But standard check:
            if (tv.getMaxLines() == 1 || tv.getEllipsize() != null) {
                // Expand
                tv.setSingleLine(false);
                tv.setMaxLines(10);
                tv.setEllipsize(null);
            } else {
                // Collapse
                tv.setSingleLine(true);
                tv.setMaxLines(1);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            }
        });

        // Avatar (Initial)
        String initial = playerName != null && !playerName.isEmpty() ? playerName.substring(0, 1).toUpperCase() : "?";
        holder.tvAvatar.setText(initial);

        // Contributions
        int dailyCon = member.detail != null ? member.detail.conDay : 0;
        holder.tvConDay.setText(formatNumber(dailyCon));

        int weeklyCon = (member.detail != null && member.detail.conObj != null) ? member.detail.conObj.thisWeek : 0;
        holder.tvConWeek.setText(formatNumber(weeklyCon));

        int lastWeekCon = (member.detail != null && member.detail.conObj != null) ? member.detail.conObj.lastWeek : 0;
        holder.tvConLastWeek.setText(formatNumber(lastWeekCon));

        int beforeLastWeekCon = (member.detail != null && member.detail.conObj != null)
                ? member.detail.conObj.beforeLastWeek
                : 0;
        holder.tvConBeforeLastWeek.setText(formatNumber(beforeLastWeekCon));
    }

    private String formatNumber(int num) {
        if (num >= 1000000) {
            return String.format("%.1fM", num / 1000000.0);
        } else if (num >= 1000) {
            return String.format("%.1fK", num / 1000.0);
        }
        return String.valueOf(num);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        TextView tvName;
        TextView tvConDay;
        TextView tvConWeek;
        TextView tvConLastWeek;
        TextView tvConBeforeLastWeek;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvConDay = itemView.findViewById(R.id.tvConDay);
            tvConWeek = itemView.findViewById(R.id.tvConWeek);
            tvConLastWeek = itemView.findViewById(R.id.tvConLastWeek);
            tvConBeforeLastWeek = itemView.findViewById(R.id.tvConBeforeLastWeek);
        }
    }
}
