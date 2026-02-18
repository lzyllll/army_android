package com.example.demo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.pojo.ParsedMember;
import com.example.demo.ui.AccountManager;
import com.example.demo.ui.adapter.MemberAdapter;
import com.example.demo.ui.model.UserAccount;

import java.util.List;

public class UnionMembersFragment extends Fragment implements AccountManager.OnDataChangeListener {

    private RecyclerView recyclerMembers;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private TextView tvHeaderDaily, tvHeaderWeekly;
    private android.widget.EditText etSearchName;
    private android.widget.Spinner spinnerFilter;
    private com.google.android.material.button.MaterialButton btnCopyAll;

    private MemberAdapter memberAdapter;
    private UserAccount.GameUserData currentGameUser;

    private List<ParsedMember> allMembers;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_union_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerMembers = view.findViewById(R.id.recyclerMembers);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tvHeaderDaily = view.findViewById(R.id.tvHeaderDaily);
        tvHeaderWeekly = view.findViewById(R.id.tvHeaderWeekly);
        etSearchName = view.findViewById(R.id.etSearchName);
        spinnerFilter = view.findViewById(R.id.spinnerFilter);
        btnCopyAll = view.findViewById(R.id.btnCopyAll);

        memberAdapter = new MemberAdapter();
        recyclerMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMembers.setAdapter(memberAdapter);

        // Setup Filter Spinner
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new String[] { "全部", "日贡 >= 1100", "日贡 < 1100" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(
                () -> loadMembers(AccountManager.getInstance(requireContext()).getCurrentAccount()));

        tvHeaderDaily.setOnClickListener(v -> {
            if (memberAdapter != null)
                memberAdapter.sortByDaily();
        });

        tvHeaderWeekly.setOnClickListener(v -> {
            if (memberAdapter != null)
                memberAdapter.sortByWeekly();
        });

        // Filter Listeners
        etSearchName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMembers();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterMembers();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnCopyAll.setOnClickListener(v -> copyFilteredNames());
    }

    @Override
    public void onResume() {
        super.onResume();
        AccountManager.getInstance(requireContext()).addListener(this);
        // Do NOT load members automatically on resume
        // loadMembers(AccountManager.getInstance(requireContext()).getCurrentAccount());

        // Just show current list if adapter has data?
        // Logic: if we switch back to this tab, we want to see what was there.
        // Adapter retains data as long as fragment is alive.
        // If fragment was destroyed, we might need to reload from cache if possible, or
        // leave empty until refresh?
        // Assuming we want to show stale data if available.
        // Since we don't have local cache for members list easily accessible without
        // fetch (unless we add it to GameUserData),
        // we might leave it as is or trigger load if empty?
        // User asked "don't always refresh".
        // If we have data, don't refresh.
        UserAccount.GameUserData current = AccountManager.getInstance(requireContext()).getCurrentAccount();

        if (current != this.currentGameUser) {
            // Account changed, force update
            this.currentGameUser = current;
            if (current != null) {
                if (current.cachedMembers != null && !current.cachedMembers.isEmpty()) {
                    memberAdapter.setMembers(current.cachedMembers);
                } else {
                    loadMembers(current);
                }
            } else {
                memberAdapter.setMembers(null);
            }
        } else if (memberAdapter.getItemCount() == 0) {
            if (current != null) {
                if (current.cachedMembers != null && !current.cachedMembers.isEmpty()) {
                    memberAdapter.setMembers(current.cachedMembers);
                } else {
                    loadMembers(current);
                }
            }
            this.currentGameUser = current;
        }
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
                this.currentGameUser = gameUser;
                loadMembers(gameUser);
            });
        }
    }

    @Override
    public void onGameUserChanged(UserAccount.GameUserData gameUser) {
        // Members list usually doesn't update with simple GameUser changes unless we
        // reload
    }

    @Override
    public void onAccountsChanged() {
    }

    @Override
    public void onError(String message) {
    }

    private void loadMembers(UserAccount.GameUserData gameUserData) {
        if (gameUserData == null || gameUserData.gameUser == null)
            return;

        new Thread(() -> {
            try {
                List<ParsedMember> members = gameUserData.gameUser.getMembers(0);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allMembers = members; // Save to master list
                        gameUserData.cachedMembers = members;
                        AccountManager.getInstance(requireContext()).saveCache(gameUserData);
                        swipeRefresh.setRefreshing(false);
                        filterMembers(); // Update display
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> swipeRefresh.setRefreshing(false));
                }
            }
        }).start();
    }

    private void filterMembers() {
        if (allMembers == null || memberAdapter == null)
            return;

        String query = etSearchName.getText().toString().trim().toLowerCase();
        int filterPos = spinnerFilter.getSelectedItemPosition(); // 0: All, 1: >=1100, 2: <1100

        java.util.List<ParsedMember> filtered = new java.util.ArrayList<>();

        for (ParsedMember m : allMembers) {
            // 1. Name Filter
            String name = (m.detail != null && m.detail.playerName != null) ? m.detail.playerName : m.nickname;
            if (name == null)
                name = "";
            if (!query.isEmpty() && !name.toLowerCase().contains(query)) {
                continue;
            }

            // 2. Contribution Filter
            int daily = m.detail != null ? m.detail.conDay : 0;
            if (filterPos == 1 && daily < 1100)
                continue; // Show >= 1100
            if (filterPos == 2 && daily >= 1100)
                continue; // Show < 1100

            filtered.add(m);
        }
        memberAdapter.setMembers(filtered);
    }

    private void copyFilteredNames() {
        if (memberAdapter == null || memberAdapter.getItemCount() == 0)
            return;

        StringBuilder sb = new StringBuilder();
        // Access members from adapter or re-filter? Adapter logic might be
        // encapsulated.
        // It's safer to re-run filter or expose list from adapter.
        // Let's modify Adapter to expose current list or just use filter here logic.
        // Since we update adapter with filtered list, let's ask adapter for items?
        // Adapter.getMembers() is not standard.
        // Re-run filter logic is safest locally.

        String query = etSearchName.getText().toString().trim().toLowerCase();
        int filterPos = spinnerFilter.getSelectedItemPosition();

        int count = 0;
        if (allMembers != null) {
            for (ParsedMember m : allMembers) {
                String name = (m.detail != null && m.detail.playerName != null) ? m.detail.playerName : m.nickname;
                if (name == null)
                    name = "";
                if (!query.isEmpty() && !name.toLowerCase().contains(query))
                    continue;

                int daily = m.detail != null ? m.detail.conDay : 0;
                if (filterPos == 1 && daily < 1100)
                    continue;
                if (filterPos == 2 && daily >= 1100)
                    continue;

                sb.append(name).append("\n");
                count++;
            }
        }

        if (count > 0) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) requireContext()
                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("FilteredMembers", sb.toString()));
                android.widget.Toast
                        .makeText(requireContext(), "已复制 " + count + " 位成员", android.widget.Toast.LENGTH_SHORT).show();
            }
        } else {
            android.widget.Toast.makeText(requireContext(), "没有可复制的成员", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
