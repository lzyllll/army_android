package com.example.demo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.demo.R;

import com.example.demo.ui.AccountManager;
import com.example.demo.ui.model.UserAccount;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class ArmyFragment extends Fragment implements AccountManager.OnDataChangeListener {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ProgressBar progressBar;

    private AccountManager accountManager;
    private UserAccount.GameUserData currentGameUser;

    private DashboardAdapter dashboardAdapter;

    // Fragments manage their own data via AccountManager events

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountManager = AccountManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_army, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
        progressBar = view.findViewById(R.id.progressBar);

        // Setup ViewPager
        dashboardAdapter = new DashboardAdapter(this);
        viewPager.setAdapter(dashboardAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("军队");
                            break;
                        case 1:
                            tab.setText("个人");
                            break;
                        case 2:
                            tab.setText("成员");
                            break;
                        case 3:
                            tab.setText("任务");
                            break;
                        case 4:
                            tab.setText("申请");
                            break;
                    }
                }).attach();
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
    public void onCurrentAccountChanged(UserAccount.GameUserData gameUser) {
        // Dashboard fragments will update themselves
    }

    @Override
    public void onGameUserChanged(UserAccount.GameUserData gameUser) {
        // Child fragments handle this
    }

    @Override
    public void onError(String message) {
    }

    @Override
    public void onAccountsChanged() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                List<UserAccount.GameUserData> selectedUsers = accountManager.getSelectedGameUsers();
                UserAccount.GameUserData current = accountManager.getCurrentAccount();

                // If current account is no longer selected, switch to first selected
                if (!selectedUsers.contains(current)) {
                    if (!selectedUsers.isEmpty()) {
                        accountManager.setCurrentAccount(selectedUsers.get(0));
                    } else {
                        accountManager.setCurrentAccount(null);
                    }
                } else {
                    // Current account still selected, fragments will update themselves
                }
            });
        }
    }

    // ViewPager Adapter
    private class DashboardAdapter extends FragmentStateAdapter {

        public DashboardAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new UnionInfoFragment();
                case 1:
                    return new SelfInfoFragment();
                case 2:
                    return new UnionMembersFragment();
                case 3:
                    return new AccountTasksFragment();
                case 4:
                    return new ApplyFragment();
                default:
                    return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
