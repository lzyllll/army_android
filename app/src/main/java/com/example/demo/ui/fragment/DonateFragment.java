package com.example.demo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.ui.AccountManager;
import com.example.demo.ui.adapter.DonateAdapter;
import com.example.demo.ui.model.UserAccount;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.ArrayList;

public class DonateFragment extends Fragment implements AccountManager.OnDataChangeListener {

    private TextView tvRefresh;
    private RecyclerView recyclerView;
    private TextView tvSelectedCount;
    private MaterialButton btnDonateAll;
    private ProgressBar progressBar;

    private AccountManager accountManager;
    private DonateAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountManager = AccountManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvRefresh = view.findViewById(R.id.tvRefresh);
        recyclerView = view.findViewById(R.id.recyclerView);
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount);
        btnDonateAll = view.findViewById(R.id.btnDonateAll);
        progressBar = view.findViewById(R.id.progressBar);

        adapter = new DonateAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnSelectionChangedListener(count -> {
            tvSelectedCount.setText("已选: " + count);
            btnDonateAll.setEnabled(count > 0);
        });

        // 全选 - 移除
        // cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
        // adapter.selectAll(isChecked);
        // });

        // 刷新
        tvRefresh.setOnClickListener(v -> refreshData());

        // 一键贡献
        btnDonateAll.setOnClickListener(v -> showConfirmDialog());

        // 加载数据
        loadData();
    }

    private void loadData() {
        // 获取选中的 GameUser
        List<UserAccount.GameUserData> allGameUsers = accountManager.getSelectedGameUsers();
        if (allGameUsers != null && !allGameUsers.isEmpty()) {
            adapter.setGameUsers(allGameUsers);
        } else {
            adapter.setGameUsers(null);
        }
    }

    private void refreshData() {
        List<UserAccount.GameUserData> allGameUsers = accountManager.getSelectedGameUsers();
        if (allGameUsers == null || allGameUsers.isEmpty()) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // We no longer query API for contribution status.
        // Just reload local data to ensure UI is up-to-date with AccountManager
        // Actually AccountManager already holds the data.
        // We might want to refresh *other* data?
        // User said "No longer use API to query contribution status".
        // So we just simulate a refresh or do nothing?
        // Let's just update the adapter.
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                adapter.setGameUsers(allGameUsers);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "列表已更新", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showConfirmDialog() {
        // Use selected users from ADAPTER (what user checked in UI)
        List<UserAccount.GameUserData> allUsers = adapter.getSelectedGameUsers();

        final List<UserAccount.GameUserData> selected = allUsers;

        // Filter those who can donate (optional logic, for now use all or let user
        // decide)
        // Or strictly use what's shown in adapter? Adapter shows all.
        // Let's use all.

        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "没有可用账号", Toast.LENGTH_SHORT).show();
            return;
        }

        List<UserAccount.GameUserData> toDonate = new ArrayList<>();
        int count = 0;
        for (UserAccount.GameUserData gu : selected) {
            if (gu.lastDonateTime > 0 && android.text.format.DateUtils.isToday(gu.lastDonateTime)) {
                continue;
            }
            toDonate.add(gu);
            count++;
        }

        if (toDonate.isEmpty()) {
            Toast.makeText(requireContext(), "选中的账号今日均已贡献", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("即将对 ").append(count).append(" 个账号进行贡献:\n\n");

        // Show first few names to avoid too long dialog
        int shown = 0;
        for (UserAccount.GameUserData gu : toDonate) {
            if (shown < 5) {
                sb.append(gu.title).append(" (S").append(gu.archIndex).append(")\n");
            }
            shown++;
        }
        if (count > 5) {
            sb.append("...等 ").append(count).append(" 个账号");
        }

        final List<UserAccount.GameUserData> finalToDonate = toDonate;

        new AlertDialog.Builder(requireContext())
                .setTitle("确认贡献")
                .setMessage(sb.toString())
                .setPositiveButton("确认", (dialog, which) -> doDonate(finalToDonate))
                .setNegativeButton("取消", null)
                .show();
    }

    private void doDonate(List<UserAccount.GameUserData> gameUsers) {
        progressBar.setVisibility(View.VISIBLE);
        btnDonateAll.setEnabled(false);

        accountManager.donateAll(gameUsers, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    btnDonateAll.setEnabled(true);
                    Toast.makeText(requireContext(), "执行完成", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        accountManager.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        accountManager.removeListener(this);
    }

    @Override
    public void onAccountsChanged() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                loadData();
            });
        }
    }

    @Override
    public void onGameUserChanged(UserAccount.GameUserData gameUser) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // adapter.setGameUsers(accountManager.getSelectedGameUsers());
                adapter.notifyDataSetChanged();
            });
        }
    }

    @Override
    public void onError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
