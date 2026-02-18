package com.example.demo.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.R;
import com.example.demo.ui.AccountManager;
import com.example.demo.ui.adapter.AccountAdapter;
import com.example.demo.ui.model.UserAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class AccountFragment extends Fragment implements AccountManager.OnDataChangeListener {

    private RecyclerView recyclerView;
    private AccountAdapter adapter;
    private AccountManager accountManager;
    private ImageView ivAddAccount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountManager = AccountManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    private android.widget.TextView tvGlobalSelectedAccount;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        ivAddAccount = view.findViewById(R.id.ivAddAccount);
        tvGlobalSelectedAccount = view.findViewById(R.id.tvGlobalSelectedAccount);
        view.findViewById(R.id.cardGlobalSelector).setOnClickListener(v -> showGlobalSelector());

        adapter = new AccountAdapter(accountManager.getAccounts());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        updateGlobalUI();

        adapter.setOnAccountActionListener(new AccountAdapter.OnAccountActionListener() {
            @Override
            public void onDeleteAccount(UserAccount account) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("删除账号")
                        .setMessage("确定要删除账号 " + account.username + " 吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            accountManager.removeAccount(account);
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }

            @Override
            public void onAddArchive(UserAccount account) {
                if (account == null)
                    return;
                int accountPosition = accountManager.getAccounts().indexOf(account);
                Toast.makeText(requireContext(), "正在刷新存档列表...", Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    accountManager.fetchAccountList(account);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            adapter.setAccounts(accountManager.getAccounts());
                            // 展开该账号卡片
                            if (accountPosition >= 0) {
                                adapter.expandPosition(accountPosition);
                            }
                            Toast.makeText(requireContext(), "存档列表已更新", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }

            @Override
            public void onArchiveSelectedChanged(UserAccount account, UserAccount.GameUserData gameUser,
                    boolean selected) {
                gameUser.selected = selected;
                accountManager.updateGameUserSelection(account);
            }
        });

        ivAddAccount.setOnClickListener(v -> showLoginDialog());
    }

    private void updateGlobalUI() {
        if (tvGlobalSelectedAccount == null)
            return;
        UserAccount.GameUserData current = accountManager.getCurrentAccount();
        if (current != null) {
            tvGlobalSelectedAccount.setText(current.title + " (S" + current.archIndex + ")");
        } else {
            tvGlobalSelectedAccount.setText("请选择账号");
        }
    }

    private void showGlobalSelector() {
        java.util.List<UserAccount.GameUserData> selectedUsers = accountManager.getSelectedGameUsers();
        if (selectedUsers.isEmpty()) {
            Toast.makeText(requireContext(), "没有选中的账号，请先勾选账号", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[selectedUsers.size()];
        for (int i = 0; i < selectedUsers.size(); i++) {
            UserAccount.GameUserData gu = selectedUsers.get(i);
            items[i] = gu.title + " (S" + gu.archIndex + ")";
        }

        int selectedIndex = selectedUsers.indexOf(accountManager.getCurrentAccount());

        new AlertDialog.Builder(requireContext())
                .setTitle("切换账号")
                .setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
                    accountManager.setCurrentAccount(selectedUsers.get(which));
                    dialog.dismiss();
                })
                .show();
    }

    private void showLoginDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_login, null);
        TextInputEditText etUsername = dialogView.findViewById(R.id.etUsername);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);
        TextInputEditText etCaptcha = dialogView.findViewById(R.id.etCaptcha);
        ImageView ivCaptcha = dialogView.findViewById(R.id.ivCaptcha);
        View btnLogin = dialogView.findViewById(R.id.btnLogin);
        View tvCancel = dialogView.findViewById(R.id.tvCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 存储会话状态
        final String[] currentSessionId = { null };
        final String[] currentCookies = { null };

        // 点击验证码图片刷新
        ivCaptcha.setOnClickListener(v -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            if (username.isEmpty()) {
                Toast.makeText(requireContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(requireContext(), "正在刷新验证码...", Toast.LENGTH_SHORT).show();
            accountManager.refreshCaptcha(username, new AccountManager.LoginCallback() {
                @Override
                public void onSuccess(UserAccount account) {
                    // refreshCaptcha 不应发生
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
                public void onCaptchaRequired(byte[] captchaImage, String sessionId, String cookies) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (captchaImage != null && captchaImage.length > 0) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(captchaImage, 0, captchaImage.length);
                                ivCaptcha.setImageBitmap(bitmap);
                            }

                            // 更新会话数据
                            currentSessionId[0] = sessionId;
                            currentCookies[0] = cookies;

                            // 清除之前的验证码输入
                            etCaptcha.setText("");

                            Toast.makeText(requireContext(), "验证码已刷新", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        });

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String captcha = etCaptcha.getText() != null ? etCaptcha.getText().toString().trim() : "";

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }

            accountManager.login(username, password, captcha, currentSessionId[0], currentCookies[0],
                    new AccountManager.LoginCallback() {
                        @Override
                        public void onSuccess(UserAccount account) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    adapter.setAccounts(accountManager.getAccounts());
                                    // 展开新添加的账号
                                    int newPosition = accountManager.getAccounts().size() - 1;
                                    if (newPosition >= 0) {
                                        adapter.expandPosition(newPosition);
                                    }
                                    Toast.makeText(requireContext(), "登录成功", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                            }
                        }

                        @Override
                        public void onError(String message) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                // 可选：如果假设会话无效，则在错误时清除会话
                                // currentSessionId[0] = null;
                                // currentCookies[0] = null;
                            });
                        }

                        @Override
                        public void onCaptchaRequired(byte[] captchaImage, String sessionId, String cookies) {
                            requireActivity().runOnUiThread(() -> {
                                if (captchaImage != null && captchaImage.length > 0) {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(captchaImage, 0, captchaImage.length);
                                    ivCaptcha.setImageBitmap(bitmap);
                                }

                                // 存储会话数据
                                currentSessionId[0] = sessionId;
                                currentCookies[0] = cookies;

                                // 显示验证码布局
                                View layoutCaptcha = dialogView.findViewById(R.id.layoutCaptcha); // Make sure id
                                                                                                  // matches
                                if (layoutCaptcha != null) {
                                    layoutCaptcha.setVisibility(View.VISIBLE);
                                }

                                Toast.makeText(requireContext(), "请输入验证码", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });

        tvCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
                adapter.setAccounts(accountManager.getAccounts());

                // 自动选择逻辑（从 MainActivity 迁移）
                java.util.List<UserAccount.GameUserData> selectedUsers = accountManager.getSelectedGameUsers();
                UserAccount.GameUserData current = accountManager.getCurrentAccount();

                boolean currentValid = false;
                if (current != null) {
                    if (selectedUsers.contains(current)) {
                        currentValid = true;
                    } else {
                        for (UserAccount.GameUserData gu : selectedUsers) {
                            if (gu == current) {
                                currentValid = true;
                                break;
                            }
                        }
                    }
                }

                if (!currentValid) {
                    if (!selectedUsers.isEmpty()) {
                        accountManager.setCurrentAccount(selectedUsers.get(0));
                    } else {
                        accountManager.setCurrentAccount(null);
                    }
                }
                updateGlobalUI();
            });
        }
    }

    @Override
    public void onGameUserChanged(UserAccount.GameUserData gameUser) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateGlobalUI);
        }
    }

    @Override
    public void onCurrentAccountChanged(UserAccount.GameUserData gameUser) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateGlobalUI);
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
