package com.example.demo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.pojo.GameUser;
import com.example.demo.thrift.TasksInfo;
import com.example.demo.ui.AccountManager;
import com.example.demo.ui.adapter.TaskAdapter;
import com.example.demo.ui.model.UserAccount;

public class AccountTasksFragment extends Fragment implements AccountManager.OnDataChangeListener {

    private RecyclerView recyclerTasks;
    private TaskAdapter taskAdapter;
    private TextView tvOneClickTasks;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private UserAccount.GameUserData currentGameUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        tvOneClickTasks = view.findViewById(R.id.tvOneClickTasks);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        taskAdapter = new TaskAdapter();
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(taskAdapter);

        swipeRefresh.setOnRefreshListener(this::refreshTasks);

        tvOneClickTasks.setOnClickListener(v -> {
            if (currentGameUser != null) {
                oneClickTasks();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        AccountManager.getInstance(requireContext()).addListener(this);

        UserAccount.GameUserData current = AccountManager.getInstance(requireContext()).getCurrentAccount();
        // Check if account changed while we were paused (or first load)
        if (current != this.currentGameUser) {
            updateData(current);
        } else if (current != null && taskAdapter.getItemCount() == 0) {
            // Same account but no data, try cache
            if (current.cachedTasks != null) {
                taskAdapter.setTasksInfo(current.cachedTasks);
            } else {
                updateData(current);
            }
        }

        // Ensure currentGameUser is set
        this.currentGameUser = current;
    }

    @Override
    public void onPause() {
        super.onPause();
        AccountManager.getInstance(requireContext()).removeListener(this);
    }

    @Override
    public void onCurrentAccountChanged(UserAccount.GameUserData gameUser) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Determine if we should refresh.
                // onCurrentAccountChanged usually means user switched account manually or
                // programmatically.
                // Constraint: "refresh only when ... switching user".
                // So YES, we should refresh here.
                updateData(gameUser);
            });
        }
    }

    @Override
    public void onGameUserChanged(UserAccount.GameUserData gameUser) {
        if (getActivity() != null && gameUser == AccountManager.getInstance(requireContext()).getCurrentAccount()) {
            // GameUserData changed (e.g. tasks done).
            // Should we refresh list?
            // "don't always refresh".
            // But if we did a task, we probably want to see updated status.
            // Let's keep this or make it conditional?
            // User said "only refresh on manual or switch user".
            // If internal state changed, maybe we don't need to full fetch if we update
            // locally?
            // But usually we need to re-fetch status.
            // Let's comment out auto-refresh here unless we just did a task (which calls
            // refreshTasks explicitly anyway).

            // getActivity().runOnUiThread(() -> updateData(gameUser));
        }
    }

    @Override
    public void onAccountsChanged() {
    }

    @Override
    public void onError(String message) {
    }

    public void updateData(UserAccount.GameUserData gameUserData) {
        this.currentGameUser = gameUserData;
        if (currentGameUser != null && currentGameUser.gameUser != null) {
            // Ideally we should have the TasksInfo already, but it might not be loaded.
            // Usually refreshGameUserData loads it.
            // We can trigger a refresh if needed or rely on the parent logic.
            // For now, let's just Refresh if we have user.
            refreshTasks();
        }
    }

    private void refreshTasks() {
        if (currentGameUser == null || currentGameUser.gameUser == null)
            return;

        swipeRefresh.setRefreshing(true);
        new Thread(() -> {
            try {
                TasksInfo info = currentGameUser.gameUser.getTasksStatus();
                // Also update tasks info in account manager/cache?
                // For now just update UI.
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        taskAdapter.setTasksInfo(info);
                        currentGameUser.cachedTasks = info;
                        AccountManager.getInstance(requireContext()).saveCache(currentGameUser);
                        swipeRefresh.setRefreshing(false);
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), "获取任务失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void oneClickTasks() {
        if (currentGameUser == null)
            return;

        swipeRefresh.setRefreshing(true);
        AccountManager.getInstance(requireContext()).oneClickTasks(currentGameUser, new AccountManager.TaskCallback() {
            @Override
            public void onResult(String result) {
                if (getActivity() != null) {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show();
                    // Auto-refresh handled by AccountManager or onGameUserChanged
                    refreshTasks();
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    swipeRefresh.setRefreshing(false);
                    String msg = message != null ? message : "Unknown error";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
