package com.example.demo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.example.demo.ui.adapter.ApplyAdapter;
import com.example.demo.ui.model.ApplyUserData;
import com.example.demo.ui.model.UserAccount;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class ApplyFragment extends Fragment implements AccountManager.OnDataChangeListener {

    private TextView tvSelectedGameUser;
    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutBatchActions;
    private MaterialCheckBox cbSelectAll;
    private MaterialButton btnRejectAll;
    private MaterialButton btnApproveAll;
    private ProgressBar progressBar;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private AccountManager accountManager;
    private ApplyAdapter adapter;
    private UserAccount.GameUserData currentGameUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountManager = AccountManager.getInstance(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        accountManager.addListener(this);
        updateData(AccountManager.getInstance(requireContext()).getCurrentAccount());
    }

    private void updateData(UserAccount.GameUserData gameUserData) {
        if (gameUserData != currentGameUser) {
            // Account changed
            selectGameUser(gameUserData);
        } else if (adapter.getItemCount() == 0) {
            selectGameUser(gameUserData);
        } else {
            // Check if title update is needed
            if (gameUserData != null) {
                tvSelectedGameUser.setText(gameUserData.title + " (S" + gameUserData.archIndex + ")");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_apply, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide local selector as ArmyFragment handles it
        view.findViewById(R.id.layoutGameUserSelector).setVisibility(View.GONE);

        tvSelectedGameUser = view.findViewById(R.id.tvSelectedGameUser);
        recyclerView = view.findViewById(R.id.recyclerView);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutBatchActions = view.findViewById(R.id.layoutBatchActions);
        cbSelectAll = view.findViewById(R.id.cbSelectAll);
        btnRejectAll = view.findViewById(R.id.btnRejectAll);
        btnApproveAll = view.findViewById(R.id.btnApproveAll);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadApplyList);

        adapter = new ApplyAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnApplyActionListener(new ApplyAdapter.OnApplyActionListener() {
            @Override
            public void onApprove(ApplyUserData apply) {
                handleSingleApply(apply, true);
            }

            @Override
            public void onReject(ApplyUserData apply) {
                handleSingleApply(apply, false);
            }

            @Override
            public void onSelectionChanged(int count) {
                updateBatchButtons(count);
            }
        });

        // GameUser 选择器
        view.findViewById(R.id.layoutGameUserSelector).setOnClickListener(v -> showGameUserSelector());

        // 全选
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.selectAll(isChecked);
        });

        // 批量拒绝
        btnRejectAll.setOnClickListener(v -> showBatchConfirmDialog(false));

        // 批量通过
        btnApproveAll.setOnClickListener(v -> showBatchConfirmDialog(true));
    }

    private void selectGameUser(UserAccount.GameUserData gameUser) {
        if (gameUser == null) {
            showEmptyState();
            return;
        }
        currentGameUser = gameUser;
        tvSelectedGameUser.setText(gameUser.title + " (S" + gameUser.archIndex + ")");
        // 申请列表是实时数据，总是从服务器加载
        loadApplyList();
    }

    private void loadApplyList() {
        if (currentGameUser == null || currentGameUser.gameUser == null) {
            showEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        accountManager.getApplyList(currentGameUser, 1, 50, new AccountManager.ApplyListCallback() {
            @Override
            public void onSuccess(List<ApplyUserData> applyList) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (applyList != null && !applyList.isEmpty()) {
                            showApplyList(applyList);
                            currentGameUser.cachedApplyList = applyList;
                            accountManager.saveCache(currentGameUser);
                        } else {
                            showEmptyState();
                        }
                        swipeRefresh.setRefreshing(false);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        showEmptyState();
                        swipeRefresh.setRefreshing(false);
                    });
                }
            }
        });
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutBatchActions.setVisibility(View.GONE);
    }

    private void showApplyList(List<ApplyUserData> applyList) {
        adapter.setApplyList(applyList);
        recyclerView.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutBatchActions.setVisibility(View.VISIBLE);
    }

    private void handleSingleApply(ApplyUserData apply, boolean approve) {
        List<ApplyUserData> singleList = new ArrayList<>();
        singleList.add(apply);

        doAudit(singleList, approve, () -> {
            adapter.removeApply(apply);
            if (adapter.getItemCount() == 0) {
                showEmptyState();
            }
        });
    }

    private void updateBatchButtons(int count) {
        btnApproveAll.setEnabled(count > 0);
        btnRejectAll.setEnabled(count > 0);
    }

    private void showBatchConfirmDialog(boolean approve) {
        List<ApplyUserData> selected = adapter.getSelectedApplies();
        if (selected.isEmpty()) {
            return;
        }

        String action = approve ? "通过" : "拒绝";
        new AlertDialog.Builder(requireContext())
                .setTitle("批量" + action)
                .setMessage("确定要" + action + " " + selected.size() + " 个申请吗？")
                .setPositiveButton("确定", (dialog, which) -> doBatchAudit(selected, approve))
                .setNegativeButton("取消", null)
                .show();
    }

    private void doBatchAudit(List<ApplyUserData> applies, boolean approve) {
        doAudit(applies, approve, () -> {
            loadApplyList();
        });
    }

    private void doAudit(List<ApplyUserData> applies, boolean approve, Runnable onComplete) {
        if (currentGameUser == null || currentGameUser.gameUser == null) {
            Toast.makeText(requireContext(), "请先选择账号", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        accountManager.auditApply(currentGameUser, applies, approve, (successCount, errorMessage) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (successCount > 0) {
                        String action = approve ? "通过" : "拒绝";
                        Toast.makeText(requireContext(), "已" + action + " " + successCount + " 个申请", Toast.LENGTH_SHORT)
                                .show();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    } else {
                        String msg = errorMessage != null ? "操作失败: " + errorMessage : "操作失败，请查看日志";
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showGameUserSelector() {
        List<UserAccount.GameUserData> allGameUsers = accountManager.getAllGameUsers();
        if (allGameUsers == null || allGameUsers.isEmpty()) {
            Toast.makeText(requireContext(), "没有可用账号", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[allGameUsers.size()];
        for (int i = 0; i < allGameUsers.size(); i++) {
            UserAccount.GameUserData gu = allGameUsers.get(i);
            items[i] = gu.title + " (S" + gu.archIndex + ")";
        }

        int selectedIndex = allGameUsers.indexOf(currentGameUser);
        if (selectedIndex < 0)
            selectedIndex = 0;

        new AlertDialog.Builder(requireContext())
                .setTitle("选择 GameUser")
                .setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
                    selectGameUser(allGameUsers.get(which));
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        accountManager.removeListener(this);
    }

    @Override
    public void onCurrentAccountChanged(UserAccount.GameUserData gameUser) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> selectGameUser(gameUser));
        }
    }

    @Override
    public void onGameUserChanged(UserAccount.GameUserData gameUser) {
        if (currentGameUser == gameUser && getActivity() != null) {
            // Maybe refresh list if needed? For now do nothing unless explicit refresh
            // requested
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

    @Override
    public void onAccountsChanged() {
        // Optional: Handle account list changes (e.g., if current account is deleted)
    }
}
