package com.example.demo.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.demo.api.AccountApi;
import com.example.demo.api.LoginApi;
import com.example.demo.pojo.AccountInfo;
import com.example.demo.pojo.GameUser;
import com.example.demo.pojo.LoginResult;
import com.example.demo.pojo.ParsedGeneralMember;
import com.example.demo.pojo.ParsedUnionAndMe;
import com.example.demo.ui.model.ApplyUserData;
import com.example.demo.ui.model.UserAccount;

import org.apache.thrift.TException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 账号管理器 - 单例模式
 */
public class AccountManager {
    private static final String TAG = "AccountManager";
    private static AccountManager instance;
    private final Context context;
    private final SharedPreferences prefs;

    private final List<UserAccount> accounts = new CopyOnWriteArrayList<>();
    private final List<OnDataChangeListener> listeners = new ArrayList<>();

    public interface OnDataChangeListener {
        void onAccountsChanged();

        void onGameUserChanged(UserAccount.GameUserData gameUser);

        void onError(String message);

        default void onCurrentAccountChanged(UserAccount.GameUserData gameUser) {
        }
    }

    private UserAccount.GameUserData currentAccount;

    private AccountManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("account_manager", Context.MODE_PRIVATE);
        loadAccounts();
    }

    public static synchronized AccountManager getInstance(Context context) {
        if (instance == null) {
            instance = new AccountManager(context);
        }
        return instance;
    }

    public void addListener(OnDataChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnDataChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyAccountsChanged() {
        for (OnDataChangeListener listener : listeners) {
            listener.onAccountsChanged();
        }
    }

    private void notifyGameUserChanged(UserAccount.GameUserData gameUser) {
        for (OnDataChangeListener listener : listeners) {
            listener.onGameUserChanged(gameUser);
        }
    }

    private void notifyError(String message) {
        for (OnDataChangeListener listener : listeners) {
            listener.onError(message);
        }
    }

    private void notifyCurrentAccountChanged(UserAccount.GameUserData gameUser) {
        for (OnDataChangeListener listener : listeners) {
            listener.onCurrentAccountChanged(gameUser);
        }
    }

    // ==================== 账号管理 ====================

    public UserAccount.GameUserData getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(UserAccount.GameUserData gameUser) {
        this.currentAccount = gameUser;
        saveAccounts(); // 保存选择
        notifyCurrentAccountChanged(gameUser);
    }

    public List<UserAccount> getAccounts() {
        return accounts;
    }

    public List<UserAccount.GameUserData> getAllGameUsers() {
        List<UserAccount.GameUserData> result = new ArrayList<>();
        for (UserAccount account : accounts) {
            result.addAll(account.gameUsers);
        }
        return result;
    }

    public List<UserAccount.GameUserData> getSelectedGameUsers() {
        List<UserAccount.GameUserData> result = new ArrayList<>();
        for (UserAccount account : accounts) {
            for (UserAccount.GameUserData gu : account.gameUsers) {
                if (gu.enabled && gu.selected) {
                    result.add(gu);
                }
            }
        }
        return result;
    }

    /**
     * 添加账号（登录）
     */
    public interface LoginCallback {
        void onSuccess(UserAccount account);

        void onError(String message);

        void onCaptchaRequired(byte[] captchaImage, String sessionId, String cookies);
    }

    public void login(String username, String password, String captcha, String sessionId, String cookies,
            LoginCallback callback) {
        new Thread(() -> {
            try {
                LoginApi loginApi = new LoginApi();

                // 如果提供了会话，恢复它并跳过检查
                if (sessionId != null && !sessionId.isEmpty() && cookies != null && !cookies.isEmpty()) {
                    loginApi.setCookies(cookies);
                } else {
                    // 检查是否需要验证码
                    LoginResult checkResult = loginApi.checkLogin(username);
                    if (checkResult.needCaptcha) {
                        // 需要验证码
                        callback.onCaptchaRequired(
                                android.util.Base64.decode(checkResult.captchaBase64, android.util.Base64.DEFAULT),
                                checkResult.sessionId,
                                checkResult.cookies);
                        return;
                    }
                }

                // 执行登录
                LoginResult result = loginApi.login(username, password, captcha, sessionId);

                if (result.success) {
                    String newCookies = result.cookies;
                    String uid = result.uid;

                    UserAccount account = null;
                    // 检查账号是否存在
                    for (UserAccount acc : accounts) {
                        if (acc.username.equals(username)) {
                            account = acc;
                            break;
                        }
                    }

                    if (account != null) {
                        // 更新现有账号
                        account.uid = uid;
                        account.cookies = newCookies;
                    } else {
                        // 创建新账号
                        account = new UserAccount(uid, username, newCookies);
                        accounts.add(account);
                    }

                    saveAccounts();

                    // 获取账号列表
                    fetchAccountList(account);

                    callback.onSuccess(account);
                    notifyAccountsChanged();
                } else if (result.needCaptcha) {
                    // 登录期间需要验证码（如果 checkLogin 正常工作不应发生，但为了严格安全）
                    callback.onCaptchaRequired(
                            android.util.Base64.decode(result.captchaBase64, android.util.Base64.DEFAULT),
                            result.sessionId,
                            result.cookies);
                } else {
                    callback.onError(result.errorMessage != null ? result.errorMessage : "登录失败");
                }
            } catch (Exception e) {
                callback.onError("登录失败: " + e.getMessage());
            }
        }).start();
    }

    public void refreshCaptcha(String username, LoginCallback callback) {
        new Thread(() -> {
            try {
                LoginApi loginApi = new LoginApi();
                LoginResult checkResult = loginApi.checkLogin(username);
                if (checkResult.needCaptcha) {
                    callback.onCaptchaRequired(
                            android.util.Base64.decode(checkResult.captchaBase64, android.util.Base64.DEFAULT),
                            checkResult.sessionId,
                            checkResult.cookies);
                } else {
                    callback.onError("当前无需验证码");
                }
            } catch (Exception e) {
                callback.onError("刷新验证码失败: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 获取账号存档列表
     */
    public void fetchAccountList(UserAccount account) {
        try {
            AccountApi accountApi = new AccountApi(account.uid, account.cookies);
            AccountInfo.List list = accountApi.getAccountList();

            List<UserAccount.GameUserData> newGameUsers = new ArrayList<>();

            for (AccountInfo info : list.accounts) {
                if (!info.isCheated()) {
                    // 尝试查找现有的 GameUserData 以保留本地标志
                    UserAccount.GameUserData existing = null;
                    for (UserAccount.GameUserData gu : account.gameUsers) {
                        if (gu.archIndex == info.index) {
                            existing = gu;
                            break;
                        }
                    }

                    GameUser gameUser = accountApi.createGameUser(info);
                    UserAccount.GameUserData userData;

                    if (existing != null) {
                        // 更新现有数据
                        userData = existing;
                        userData.title = info.title;
                        userData.datetime = info.datetime;
                        userData.status = info.status;
                        userData.gameUser = gameUser;
                        // enabled, selected, lastDonateTime 被保留
                    } else {
                        // 新建
                        userData = new UserAccount.GameUserData(info, gameUser);
                    }
                    newGameUsers.add(userData);
                }
            }
            account.gameUsers.clear();
            account.gameUsers.addAll(newGameUsers);
            saveAccounts();

            // 加载所有游戏用户的缓存
            for (UserAccount.GameUserData gu : account.gameUsers) {
                loadGameUserCache(gu, account.uid);
            }
        } catch (IOException e) {
            notifyError("获取存档失败: " + e.getMessage());
        }
    }

    /**
     * 刷新 GameUser 数据
     */
    public void refreshGameUserData(UserAccount.GameUserData gameUserData, Runnable onComplete) {
        new Thread(() -> {
            try {
                if (gameUserData.gameUser != null) {
                    ParsedUnionAndMe unionAndMe = gameUserData.gameUser.getUnionAndMe();
                    gameUserData.unionAndMe = unionAndMe;

                    if (unionAndMe != null && unionAndMe.member != null) {
                        gameUserData.updateContribution(unionAndMe.member);
                    }

                    // 获取任务状态更新日贡
                    com.example.demo.thrift.TasksInfo tasksInfo = gameUserData.gameUser.getTasksStatus();
                    gameUserData.updateContribution(tasksInfo);

                    notifyGameUserChanged(gameUserData);
                    saveCache(gameUserData);
                }
            } catch (TException e) {
                notifyError("刷新数据失败: " + e.getMessage());
            }

            if (onComplete != null) {
                onComplete.run();
            }
        }).start();
    }

    /**
     * 刷新所有 GameUser 数据
     */
    public void refreshAllGameUsers(Runnable onComplete) {
        new Thread(() -> {
            for (UserAccount account : accounts) {
                for (UserAccount.GameUserData gu : account.gameUsers) {
                    try {
                        if (gu.gameUser != null) {
                            ParsedUnionAndMe unionAndMe = gu.gameUser.getUnionAndMe();
                            gu.unionAndMe = unionAndMe;

                            if (unionAndMe != null && unionAndMe.member != null) {
                                gu.updateContribution(unionAndMe.member);
                            }

                            // 获取任务状态更新日贡
                            com.example.demo.thrift.TasksInfo tasksInfo = gu.gameUser.getTasksStatus();
                            gu.updateContribution(tasksInfo);
                            saveCache(gu);
                        }
                    } catch (TException e) {
                        // 忽略
                    }
                }
            }

            if (onComplete != null) {
                onComplete.run();
            }
        }).start();
    }

    /**
     * 一键贡献
     */
    public void donateAll(List<UserAccount.GameUserData> gameUsers, Runnable onComplete) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            long thisWeekStart = getThisWeekStartTime(now);

            for (UserAccount.GameUserData gu : gameUsers) {
                try {
                    // 检查是否需要重置本周累计（新的一周）
                    if (gu.weekStartTime == 0 || gu.weekStartTime < thisWeekStart) {
                        gu.weeklyAccumulatedContribution = 0;
                        gu.weekStartTime = thisWeekStart;
                    }

                    // 检查今天是否已经累加过贡献
                    boolean alreadyDonatedToday = gu.lastDonateTime > 0 && android.text.format.DateUtils.isToday(gu.lastDonateTime);

                    // 每天最多累加一次 1100
                    if (!alreadyDonatedToday) {
                        gu.weeklyAccumulatedContribution += 1100;
                        gu.lastDonateTime = now;
                    }

                    if (gu.gameUser != null) {
                        // 逻辑更改为本地记录
                        // 根据用户请求使用 oneClickDoTasks
                        // 这将执行所有必要的任务，包括贡献（如果配置了）
                        String result = gu.gameUser.oneClickDoTasks(gu.weeklyAccumulatedContribution);
                        gu.lastActionLog = result;
                    }
                } catch (Exception e) {
                    notifyError("贡献失败 [" + gu.title + "]: " + e.getMessage());
                }
            }

            // 保存更改
            saveAccounts();

            // 刷新 UI（通知监听器）
            // refreshAllGameUsers(onComplete); // 不再从 API 刷新
            if (onComplete != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
            }
        }).start();
    }

    /**
     * 获取申请列表
     */
    public void getApplyList(UserAccount.GameUserData gameUser, int page, int pageSize, ApplyListCallback callback) {
        new Thread(() -> {
            try {
                if (gameUser.gameUser != null) {
                    List<ParsedGeneralMember> members = gameUser.gameUser.getApplyList(page, pageSize);
                    List<ApplyUserData> applyList = new ArrayList<>();
                    for (ParsedGeneralMember m : members) {
                        applyList.add(new ApplyUserData(m));
                    }
                    callback.onSuccess(applyList);
                    gameUser.cachedApplyList = applyList;
                    saveCache(gameUser);
                } else {
                    callback.onError("GameUser 未初始化");
                }
            } catch (TException e) {
                callback.onError("获取申请列表失败: " + e.getMessage());
            }
        }).start();
    }

    public interface ApplyListCallback {
        void onSuccess(List<ApplyUserData> applyList);

        void onError(String message);
    }

    /**
     * 审核回调
     */
    public interface AuditCallback {
        void onComplete(int successCount, String errorMessage);
    }

    /**
     * 审核申请（使用批量方法，复用单个连接）
     */
    public void auditApply(UserAccount.GameUserData gameUser, List<ApplyUserData> applyList, boolean approve,
            AuditCallback callback) {
        new Thread(() -> {
            int successCount = 0;
            String errorMessage = null;
            try {
                if (gameUser == null || gameUser.gameUser == null) {
                    errorMessage = "当前账号未初始化";
                } else {
                    List<GameUser.AuditTarget> targets = new ArrayList<>();
                    for (ApplyUserData apply : applyList) {
                        String uid = apply.member != null ? apply.member.uid : null;
                        int index = apply.getTargetIndex();
                        android.util.Log.d(TAG, "auditApply: uid=" + uid + ", index=" + index
                                + ", name=" + apply.getPlayerName());
                        if (uid != null && !uid.isEmpty()) {
                            targets.add(new GameUser.AuditTarget(uid, index));
                        } else {
                            android.util.Log.e(TAG, "auditApply: 跳过空uid, name=" + apply.getPlayerName());
                        }
                    }
                    if (targets.isEmpty()) {
                        errorMessage = "没有有效的申请数据 (uid为空)";
                    } else {
                        successCount = gameUser.gameUser.auditApplyMembers(targets, approve);
                    }
                }
            } catch (com.example.demo.thrift.NormalException e) {
                android.util.Log.e(TAG, "auditApply NormalException: " + e.error, e);
                errorMessage = e.error;
            } catch (Exception e) {
                android.util.Log.e(TAG, "auditApply exception", e);
                errorMessage = e.getMessage();
            }
            final int finalCount = successCount;
            final String finalError = errorMessage;
            if (callback != null) {
                callback.onComplete(finalCount, finalError);
            }
        }).start();
    }

    /**
     * 删除账号
     */
    public void removeAccount(UserAccount account) {
        accounts.remove(account);
        saveAccounts();
        notifyAccountsChanged();
    }

    /**
     * 删除存档
     */
    public void removeGameUser(UserAccount account, UserAccount.GameUserData gameUser) {
        account.gameUsers.remove(gameUser);
        saveAccounts();
        notifyAccountsChanged();
    }

    public interface TaskCallback {
        void onResult(String result);

        void onError(String message);
    }

    /**
     * 执行一键任务 (单个)
     */
    public void oneClickTasks(UserAccount.GameUserData gameUserData, TaskCallback callback) {
        new Thread(() -> {
            try {
                String result = "GameUser 未初始化";
                if (gameUserData.gameUser != null) {
                    long now = System.currentTimeMillis();
                    long thisWeekStart = getThisWeekStartTime(now);

                    // 检查是否需要重置本周累计（新的一周）
                    if (gameUserData.weekStartTime == 0 || gameUserData.weekStartTime < thisWeekStart) {
                        gameUserData.weeklyAccumulatedContribution = 0;
                        gameUserData.weekStartTime = thisWeekStart;
                    }

                    // 检查今天是否已经累加过贡献
                    boolean alreadyDonatedToday = gameUserData.lastDonateTime > 0 && android.text.format.DateUtils.isToday(gameUserData.lastDonateTime);

                    // 每天最多累加一次 1100
                    if (!alreadyDonatedToday) {
                        gameUserData.weeklyAccumulatedContribution += 1100;
                        gameUserData.lastDonateTime = now;
                    }

                    result = gameUserData.gameUser.oneClickDoTasks(gameUserData.weeklyAccumulatedContribution);

                    // 保存账号数据
                    saveAccounts();

                    // 刷新数据
                    refreshGameUserData(gameUserData, null);
                }
                String finalResult = result;
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(() -> callback.onResult(finalResult));
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : e.toString()));
                }
            }
        }).start();
    }

    /**
     * 批量执行一键任务
     */
    public void oneClickTasksAll(List<UserAccount.GameUserData> gameUsers, Runnable onProgress, Runnable onComplete) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            long thisWeekStart = getThisWeekStartTime(now);

            for (UserAccount.GameUserData gu : gameUsers) {
                try {
                    if (gu.gameUser != null) {
                        gu.lastActionLog = "执行中...";
                        if (onProgress != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(onProgress);
                        }

                        // 检查是否需要重置本周累计（新的一周）
                        if (gu.weekStartTime == 0 || gu.weekStartTime < thisWeekStart) {
                            gu.weeklyAccumulatedContribution = 0;
                            gu.weekStartTime = thisWeekStart;
                        }

                        // 检查今天是否已经累加过贡献
                        boolean alreadyDonatedToday = gu.lastDonateTime > 0 && android.text.format.DateUtils.isToday(gu.lastDonateTime);

                        // 每天最多累加一次 1100
                        if (!alreadyDonatedToday) {
                            gu.weeklyAccumulatedContribution += 1100;
                            gu.lastDonateTime = now;
                        }

                        String result = gu.gameUser.oneClickDoTasks(gu.weeklyAccumulatedContribution);
                        gu.lastActionLog = result;

                        // 刷新单条数据
                        try {
                            ParsedUnionAndMe unionAndMe = gu.gameUser.getUnionAndMe();
                            gu.unionAndMe = unionAndMe;
                            if (unionAndMe != null && unionAndMe.member != null) {
                                gu.updateContribution(unionAndMe.member);
                            }

                            // 获取任务状态更新日贡
                            com.example.demo.thrift.TasksInfo tasksInfo = gu.gameUser.getTasksStatus();
                            gu.updateContribution(tasksInfo);
                        } catch (Exception e) {
                            gu.lastActionLog += " (刷新失败: " + e.getMessage() + ")";
                        }
                    } else {
                        gu.lastActionLog = "GameUser 未初始化";
                    }
                } catch (Exception e) {
                    gu.lastActionLog = "执行失败: " + e.getMessage();
                }

                if (onProgress != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(onProgress);
                }
            }

            // 保存更改
            saveAccounts();

            if (onComplete != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
            }
        }).start();
    }

    public void updateGameUserSelection(UserAccount account) {
        saveAccounts();
    }

    /**
     * 获取本周起始时间（周一 00:00:00）
     */
    private long getThisWeekStartTime(long now) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);

        int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
        // Calendar: Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7
        // 计算距离本周一过了多少天
        int daysSinceMonday;
        if (dayOfWeek == java.util.Calendar.SUNDAY) {
            daysSinceMonday = 6; // 周日是上周的最后一天，距离本周一6天
        } else {
            daysSinceMonday = dayOfWeek - java.util.Calendar.MONDAY;
        }

        cal.add(java.util.Calendar.DAY_OF_MONTH, -daysSinceMonday);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    // ==================== 持久化 ====================

    private void saveAccounts() {
        try {
            JSONArray array = new JSONArray();
            for (UserAccount account : accounts) {
                JSONObject obj = new JSONObject();
                obj.put("uid", account.uid);
                obj.put("username", account.username);
                obj.put("cookies", account.cookies);

                JSONArray gameUsersArray = new JSONArray();
                for (UserAccount.GameUserData gu : account.gameUsers) {
                    JSONObject guObj = new JSONObject();
                    guObj.put("archIndex", gu.archIndex);
                    guObj.put("title", gu.title);
                    guObj.put("datetime", gu.datetime);
                    guObj.put("status", gu.status);
                    guObj.put("enabled", gu.enabled);
                    guObj.put("selected", gu.selected);
                    guObj.put("lastDonateTime", gu.lastDonateTime);
                    guObj.put("weeklyAccumulatedContribution", gu.weeklyAccumulatedContribution);
                    guObj.put("weekStartTime", gu.weekStartTime);
                    gameUsersArray.put(guObj);
                }
                obj.put("gameUsers", gameUsersArray);
                array.put(obj);
            }
            prefs.edit().putString("accounts", array.toString()).apply();

            if (currentAccount != null) {
                String uid = null;
                for (UserAccount account : accounts) {
                    if (account.gameUsers.contains(currentAccount)) {
                        uid = account.uid;
                        break;
                    }
                }

                if (uid != null) {
                    prefs.edit()
                            .putString("current_uid", uid)
                            .putInt("current_arch_index", currentAccount.archIndex)
                            .apply();
                }
            }
        } catch (JSONException e) {
            // 忽略
        }
    }

    private void loadAccounts() {
        String json = prefs.getString("accounts", null);
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                accounts.clear();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    UserAccount account = new UserAccount(
                            obj.getString("uid"),
                            obj.getString("username"),
                            obj.getString("cookies"));

                    JSONArray gameUsersArray = obj.optJSONArray("gameUsers");
                    if (gameUsersArray != null) {
                        AccountApi accountApi = new AccountApi(account.uid, account.cookies);

                        for (int j = 0; j < gameUsersArray.length(); j++) {
                            JSONObject guObj = gameUsersArray.getJSONObject(j);
                            AccountInfo info = new AccountInfo(
                                    guObj.getInt("archIndex"),
                                    guObj.getInt("status"),
                                    guObj.getString("datetime"),
                                    guObj.getString("title"));

                            UserAccount.GameUserData gu = new UserAccount.GameUserData(info,
                                    accountApi.createGameUser(info));
                            gu.enabled = guObj.optBoolean("enabled", true);
                            gu.selected = guObj.optBoolean("selected", false);
                            gu.lastDonateTime = guObj.optLong("lastDonateTime", 0);
                            gu.weeklyAccumulatedContribution = guObj.optInt("weeklyAccumulatedContribution", 0);
                            gu.weekStartTime = guObj.optLong("weekStartTime", 0);
                            loadGameUserCache(gu, account.uid); // 加载缓存
                            account.gameUsers.add(gu);
                        }
                    }
                    accounts.add(account);
                }
            } catch (JSONException e) {
                // 忽略
            }
        }

        // 恢复当前账号
        String currentUid = prefs.getString("current_uid", null);
        int currentArchIndex = prefs.getInt("current_arch_index", -1);

        if (currentUid != null && currentArchIndex != -1) {
            for (UserAccount account : accounts) {
                if (account.uid.equals(currentUid)) {
                    for (UserAccount.GameUserData gu : account.gameUsers) {
                        if (gu.archIndex == currentArchIndex) {
                            this.currentAccount = gu;
                            break;
                        }
                    }
                }
                if (this.currentAccount != null)
                    break;
            }
        }

        // 如果未找到则默认
        if (this.currentAccount == null && !accounts.isEmpty() && !accounts.get(0).gameUsers.isEmpty()) {
            this.currentAccount = accounts.get(0).gameUsers.get(0);
        }
    }

    // ==================== 缓存持久化 ====================

    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    private java.io.File getCacheFile(String uid, int archIndex) {
        return new java.io.File(context.getCacheDir(), "user_cache_" + uid + "_" + archIndex + ".json");
    }

    public void saveCache(UserAccount.GameUserData gameUserData) {
        if (gameUserData == null)
            return;
        // 查找父账号 UID
        String uid = null;
        for (UserAccount acc : accounts) {
            if (acc.gameUsers.contains(gameUserData)) {
                uid = acc.uid;
                break;
            }
        }
        if (uid == null)
            return;

        final String finalUid = uid;
        new Thread(() -> {
            try {
                java.io.File file = getCacheFile(finalUid, gameUserData.archIndex);

                CacheData cache = new CacheData();
                cache.unionAndMe = gameUserData.unionAndMe;
                cache.cachedMembers = gameUserData.cachedMembers;
                cache.cachedTasks = gameUserData.cachedTasks;
                cache.cachedApplyList = gameUserData.cachedApplyList;

                String cacheJson = gson.toJson(cache);
                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(cacheJson);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadGameUserCache(UserAccount.GameUserData gameUserData, String uid) {
        if (gameUserData == null || uid == null)
            return;

        java.io.File file = getCacheFile(uid, gameUserData.archIndex);
        if (file.exists()) {
            try (java.io.FileReader reader = new java.io.FileReader(file)) {
                CacheData cache = gson.fromJson(reader, CacheData.class);
                if (cache != null) {
                    gameUserData.unionAndMe = cache.unionAndMe;
                    gameUserData.cachedMembers = cache.cachedMembers;
                    gameUserData.cachedTasks = cache.cachedTasks;
                    gameUserData.cachedApplyList = cache.cachedApplyList;

                    // 如果可用，还可以从缓存的 unionAndMe 更新贡献字段
                    if (cache.unionAndMe != null && cache.unionAndMe.member != null) {
                        gameUserData.updateContribution(cache.unionAndMe.member);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class CacheData {
        ParsedUnionAndMe unionAndMe;
        List<com.example.demo.pojo.ParsedMember> cachedMembers;
        com.example.demo.thrift.TasksInfo cachedTasks;
        List<ApplyUserData> cachedApplyList;
    }
}
