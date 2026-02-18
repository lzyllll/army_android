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

import com.example.demo.R;
import com.example.demo.pojo.ParsedUnionAndMe;
import com.example.demo.ui.AccountManager;

import org.apache.thrift.TException;
import com.example.demo.ui.model.UserAccount;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class UnionInfoFragment extends Fragment implements AccountManager.OnDataChangeListener {

    private TextView tvUnionName;
    private TextView tvUnionLevel;
    private TextView tvUnionPower;
    private TextView tvUnionMembers;
    private TextView tvAnnouncement;

    // Detailed Info
    private TextView tvTotalMoney;
    private TextView tvTotalDps;
    private TextView tvBattleScore;
    private TextView tvKingName;

    // System Info
    private TextView tvUnionId;
    private TextView tvTotalExp;
    private TextView tvTotalContribution;
    private TextView tvDissolveDate;

    // Limits & Links
    private TextView tvVipLimit;
    private TextView tvDpsLimit;
    private TextView tvUrl;

    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_union_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvUnionName = view.findViewById(R.id.tvUnionName);
        tvUnionLevel = view.findViewById(R.id.tvUnionLevel);
        tvUnionPower = view.findViewById(R.id.tvUnionPower);
        tvUnionMembers = view.findViewById(R.id.tvUnionMembers);
        tvAnnouncement = view.findViewById(R.id.tvAnnouncement);

        tvTotalMoney = view.findViewById(R.id.tvTotalMoney);
        tvTotalDps = view.findViewById(R.id.tvTotalDps);
        tvBattleScore = view.findViewById(R.id.tvBattleScore);
        tvKingName = view.findViewById(R.id.tvKingName);

        tvUnionId = view.findViewById(R.id.tvUnionId);
        tvTotalExp = view.findViewById(R.id.tvTotalExp);
        tvTotalContribution = view.findViewById(R.id.tvTotalContribution);
        tvDissolveDate = view.findViewById(R.id.tvDissolveDate);

        tvVipLimit = view.findViewById(R.id.tvVipLimit);
        tvDpsLimit = view.findViewById(R.id.tvDpsLimit);
        tvUrl = view.findViewById(R.id.tvUrl);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::refreshData);
    }

    @Override
    public void onResume() {
        super.onResume();
        AccountManager.getInstance(requireContext()).addListener(this);
        // Only update UI from cache, do not trigger fetch
        updateUI(AccountManager.getInstance(requireContext()).getCurrentAccount());
    }

    private void updateUI(UserAccount.GameUserData gameUserData) {
        if (gameUserData == null)
            return;
        updateUI(gameUserData.unionAndMe);
    }

    private void refreshData() {
        UserAccount.GameUserData gameUser = AccountManager.getInstance(requireContext()).getCurrentAccount();
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
                refreshData();
            });
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
        if (gameUserData == null)
            return;
        updateUI(gameUserData);
    }

    private void updateUI(ParsedUnionAndMe info) {
        if (info != null && info.union != null) {
            tvUnionName.setText(info.union.title);
            tvUnionLevel.setText("Lv." + info.union.level);
            tvUnionMembers.setText(info.union.count + "/100");

            // System Info
            tvUnionId.setText(String.valueOf(info.union.id));
            tvTotalExp.setText(String.valueOf(info.union.experience));
            tvTotalContribution.setText(String.valueOf(info.union.contribution));
            tvDissolveDate.setText(info.union.dissolveDate != null ? info.union.dissolveDate : "N/A");

            if (info.union.detail != null) {
                tvAnnouncement.setText(info.union.detail.notice);
                tvUnionPower.setText(String.valueOf(info.union.detail.dps));

                tvTotalMoney.setText(String.valueOf(info.union.detail.money));
                tvTotalDps.setText(String.valueOf(info.union.detail.dps));
                tvBattleScore.setText(String.valueOf(info.union.detail.bs));
                tvKingName.setText(info.union.detail.kingName);

                tvVipLimit.setText(String.valueOf(info.union.detail.vipLimit));
                tvDpsLimit.setText(String.valueOf(info.union.detail.dpsLimit));
                tvUrl.setText(info.union.detail.url);
            } else {
                tvAnnouncement.setText("暂无公告");
                tvUnionPower.setText("--");
                tvTotalMoney.setText("--");
                tvTotalDps.setText("--");
                tvBattleScore.setText("--");
                tvKingName.setText("--");
                tvVipLimit.setText("--");
                tvDpsLimit.setText("--");
                tvUrl.setText("--");
            }
        }
    }
}
