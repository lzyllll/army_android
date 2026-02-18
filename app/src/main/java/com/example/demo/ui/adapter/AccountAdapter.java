package com.example.demo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.demo.ui.AccountManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.ui.model.UserAccount;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    private List<UserAccount> accounts;
    private final Set<Integer> expandedPositions = new HashSet<>();
    private OnAccountActionListener listener;

    public interface OnAccountActionListener {
        void onDeleteAccount(UserAccount account);

        void onAddArchive(UserAccount account);

        void onArchiveSelectedChanged(UserAccount account, UserAccount.GameUserData gameUser, boolean selected);
    }

    public AccountAdapter(List<UserAccount> accounts) {
        this.accounts = accounts;
    }

    public void setAccounts(List<UserAccount> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    /**
     * 展开指定账号位置
     */
    public void expandPosition(int position) {
        if (position >= 0 && position < accounts.size()) {
            expandedPositions.add(position);
            notifyItemChanged(position);
        }
    }

    public void setOnAccountActionListener(OnAccountActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserAccount account = accounts.get(position);
        boolean isExpanded = expandedPositions.contains(position);

        holder.tvUsername.setText(account.username);
        holder.tvUid.setText("UID: " + account.uid);
        holder.tvArchiveCount.setText(account.gameUsers.size() + " 存档");

        // 展开/收起箭头
        holder.ivExpand.setRotation(isExpanded ? 180 : 0);

        // 存档列表
        holder.layoutArchives.removeAllViews();
        holder.layoutArchives.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.layoutActions.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if (isExpanded) {
            for (UserAccount.GameUserData gu : account.gameUsers) {
                View archiveView = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_archive, holder.layoutArchives, false);

                TextView tvIndex = archiveView.findViewById(R.id.tvIndex);
                TextView tvTitle = archiveView.findViewById(R.id.tvTitle);
                TextView tvDatetime = archiveView.findViewById(R.id.tvDatetime);
                // MaterialButton btnOneClick = archiveView.findViewById(R.id.btnOneClick); //
                // Removed
                CheckBox checkbox = archiveView.findViewById(R.id.checkbox);
                RadioButton rbCurrent = archiveView.findViewById(R.id.rbCurrent);

                tvIndex.setText("S" + gu.archIndex);
                tvTitle.setText(gu.title);
                tvDatetime.setText(gu.datetime);
                checkbox.setChecked(gu.selected);

                // Current Account Logic
                rbCurrent
                        .setChecked(gu == AccountManager.getInstance(holder.itemView.getContext()).getCurrentAccount());
                rbCurrent.setOnClickListener(v -> {
                    AccountManager.getInstance(holder.itemView.getContext()).setCurrentAccount(gu);
                    notifyDataSetChanged();
                });

                // Removed btnOneClick.setOnClickListener block

                int archIndex = gu.archIndex;
                checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (listener != null) {
                        listener.onArchiveSelectedChanged(account, gu, isChecked);
                    }
                });

                holder.layoutArchives.addView(archiveView);
            }
        }

        // 点击展开/收起
        holder.itemView.setOnClickListener(v -> {
            if (expandedPositions.contains(position)) {
                expandedPositions.remove(position);
            } else {
                expandedPositions.add(position);
            }
            notifyItemChanged(position);
        });

        // 删除账号
        holder.tvDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAccount(account);
            }
        });

        // 添加存档
        holder.tvAddArchive.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddArchive(account);
            }
        });
    }

    @Override
    public int getItemCount() {
        return accounts != null ? accounts.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        TextView tvUid;
        TextView tvArchiveCount;
        ImageView ivExpand;
        LinearLayout layoutArchives;
        LinearLayout layoutActions;
        TextView tvAddArchive;
        TextView tvDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvUid = itemView.findViewById(R.id.tvUid);
            tvArchiveCount = itemView.findViewById(R.id.tvArchiveCount);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            layoutArchives = itemView.findViewById(R.id.layoutArchives);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            tvAddArchive = itemView.findViewById(R.id.tvAddArchive);
            tvDelete = itemView.findViewById(R.id.tvDelete);
        }
    }
}
