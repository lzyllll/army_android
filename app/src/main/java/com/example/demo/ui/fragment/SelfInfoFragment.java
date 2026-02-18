package com.example.demo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.demo.R;
import com.example.demo.pojo.MemberDetail;
import com.example.demo.pojo.ParsedUnionAndMe;
import com.example.demo.ui.AccountManager;
import com.example.demo.ui.model.UserAccount;

import org.apache.thrift.TException;

public class SelfInfoFragment extends Fragment implements AccountManager.OnDataChangeListener {

    private TextView tvAvatarText;
    private TextView tvNickname;
    private TextView tvUserId;
    private TextView tvRole;
    private TextView tvMoney;
    private TextView tvDailyCon;
    private TextView tvWeeklyCon;
    private TextView tvLastWeekCon;
    private TextView tvTotalCon;

    // New fields
    private TextView tvLevel;
    private TextView tvVip;
    private TextView tvMilitaryRank;
    private TextView tvLoginTime;
    private TextView tvLife;
    private TextView tvDps;
    private TextView tvPk;
    private TextView tvBattleTime;
    private TextView tvBattleMap;
    private TextView tvBattleDuration;
    private TextView tvBattleScore;

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_self_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvAvatarText = view.findViewById(R.id.tvAvatarText);
        tvNickname = view.findViewById(R.id.tvNickname);
        tvUserId = view.findViewById(R.id.tvUserId);
        tvRole = view.findViewById(R.id.tvRole);
        tvMoney = view.findViewById(R.id.tvMoney);
        tvDailyCon = view.findViewById(R.id.tvDailyCon);
        tvWeeklyCon = view.findViewById(R.id.tvWeeklyCon);
        tvLastWeekCon = view.findViewById(R.id.tvLastWeekCon);
        tvTotalCon = view.findViewById(R.id.tvTotalCon);

        tvLevel = view.findViewById(R.id.tvLevel);
        tvVip = view.findViewById(R.id.tvVip);
        tvMilitaryRank = view.findViewById(R.id.tvMilitaryRank);
        tvLoginTime = view.findViewById(R.id.tvLoginTime);
        tvLife = view.findViewById(R.id.tvLife);
        tvDps = view.findViewById(R.id.tvDps);
        tvPk = view.findViewById(R.id.tvPk);
        tvBattleTime = view.findViewById(R.id.tvBattleTime);
        tvBattleMap = view.findViewById(R.id.tvBattleMap);
        tvBattleDuration = view.findViewById(R.id.tvBattleDuration);
        tvBattleMap = view.findViewById(R.id.tvBattleMap);
        tvBattleDuration = view.findViewById(R.id.tvBattleDuration);
        tvBattleScore = view.findViewById(R.id.tvBattleScore);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::refreshData);
    }

    @Override
    public void onResume() {
        super.onResume();
        AccountManager.getInstance(requireContext()).addListener(this);
        // Only update from cache
        updateUI(AccountManager.getInstance(requireContext()).getCurrentAccount());
    }

    private void updateUI(UserAccount.GameUserData gameUserData) {
        if (gameUserData != null) {
            updateUI(gameUserData.unionAndMe);
        }
    }

    private void refreshData() {
        UserAccount.GameUserData gameUser = AccountManager.getInstance(requireContext()).getCurrentAccount();
        if (gameUser != null && gameUser.gameUser != null) {
            if (gameUser != null && gameUser.gameUser != null) {
                AccountManager.getInstance(requireContext()).refreshGameUserData(gameUser, () -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateUI(gameUser);
                            swipeRefresh.setRefreshing(false);
                        });
                    }
                });
            } else {
                swipeRefresh.setRefreshing(false);
            }
        } else {
            swipeRefresh.setRefreshing(false);
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
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> refreshData());
            }
        }
    }

    @Override
    public void onGameUserChanged(UserAccount.GameUserData gameUser) {
        if (getActivity() != null && gameUser == AccountManager.getInstance(requireContext()).getCurrentAccount()) {
            getActivity().runOnUiThread(() -> updateData(gameUser));
        }
    }

    @Override
    public void onAccountsChanged() {
    }

    @Override
    public void onError(String message) {
    }

    private void updateData(UserAccount.GameUserData gameUserData) {
        if (gameUserData != null) {
            updateUI(gameUserData);
        }
    }

    private void updateUI(ParsedUnionAndMe info) {

        if (info != null && info.member != null) {
            tvNickname.setText(info.member.detail.playerName);
            if (info.member.detail.playerName != null && !info.member.detail.playerName.isEmpty()) {
                tvAvatarText.setText(info.member.detail.playerName.substring(0, 1).toUpperCase());
            } else {
                tvAvatarText.setText("?");
            }

            // Find the UserAccount that contains the current GameUserData
            UserAccount.GameUserData currentData = AccountManager.getInstance(requireContext()).getCurrentAccount();
            String uid = "Unknown";
            if (currentData != null) {
                for (UserAccount account : AccountManager.getInstance(requireContext()).getAccounts()) {
                    for (UserAccount.GameUserData gameData : account.gameUsers) {
                        if (gameData == currentData) {
                            uid = account.uid;
                            break;
                        }
                    }
                    if (!"Unknown".equals(uid))
                        break;
                }
            }
            tvUserId.setText("ID: " + uid);

            tvRole.setText(info.member.roleName);
            tvTotalCon.setText(formatNumber(info.member.contribution));

            if (info.member.detail != null) {
                MemberDetail detail = info.member.detail;
                tvMoney.setText(formatNumber(detail.money));

                tvDailyCon.setText(formatNumber(detail.conDay));

                // Profile
                tvLevel.setText("Lv." + detail.lv);
                tvVip.setText(detail.vip);
                tvMilitaryRank.setText(detail.militaryRank);
                tvLoginTime.setText("Last login: " + detail.loginTime);

                // Stats
                tvLife.setText(formatNumber(detail.life));
                // Use formatNumber for DPS to keep consistency or %.1f if needed. formatNumber
                // is checking >=1000.
                tvDps.setText(formatNumber(detail.dps));
                tvPk.setText(detail.pkS + " / " + detail.pkW);

                // Battle
                tvBattleTime.setText(detail.bt);
                tvBattleMap.setText(MemberDetail.getMapName(detail.mp));
                tvBattleDuration.setText(String.format("%.1fs", detail.lt));
                tvBattleScore.setText(String.valueOf(detail.score));

                if (detail.conObj != null) {
                    tvWeeklyCon.setText(formatNumber(detail.conObj.thisWeek));
                    tvLastWeekCon.setText(formatNumber(detail.conObj.lastWeek));
                }
            }
        }
    }

    private String formatNumber(double num) {
        if (num >= 1000000) {
            return String.format("%.1fM", num / 1000000.0);
        } else if (num >= 1000) {
            return String.format("%.1fK", num / 1000.0);
        }
        return String.valueOf((long) num);
    }
}
