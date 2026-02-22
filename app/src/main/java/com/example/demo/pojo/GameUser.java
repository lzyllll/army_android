package com.example.demo.pojo;

import com.example.demo.api.ApiConstants;
import com.example.demo.api.GameSessionApi;
import com.example.demo.api.GrowApiClient;
import com.example.demo.api.MasterApiClient;
import com.example.demo.api.MemberApiClient;
import com.example.demo.api.VisitorApiClient;
import com.example.demo.thrift.ApplyList;
import com.example.demo.thrift.LogInfo;
import com.example.demo.thrift.Normal;
import com.example.demo.thrift.TasksInfo;
import com.example.demo.thrift.UnionInfo;
import com.example.demo.thrift.UnionList;
import com.example.demo.thrift.UnionMembers;
import com.example.demo.thrift.UnionOfMe;

import com.example.demo.thrift.Task;

import org.apache.thrift.TException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏用户统一 API 类
 *
 * 使用流程:
 * 1. 使用 LoginApi 登录获取 cookies
 * 2. 创建 GameUser 实例
 * 3. 调用各种 API 方法
 */
public class GameUser {

    private String uid;
    private int archIndex;
    private String cookies;
    private String gameSession;
    private GameSessionApi sessionApi;

    /**
     * 创建 GameUser
     *
     * @param uid       用户ID
     * @param archIndex 存档索引 (0-7)
     * @param cookies   登录后的 cookies
     */
    public GameUser(String uid, int archIndex, String cookies) {
        this.uid = uid;
        this.archIndex = Math.max(0, Math.min(7, archIndex));
        this.cookies = cookies;
        this.sessionApi = new GameSessionApi(cookies);
    }

    /**
     * 检查或更新游戏会话
     */
    public String updateGameSession() throws IOException {
        this.gameSession = sessionApi.checkOrUpdate(uid, gameSession);
        return gameSession;
    }

    // ==================== Visitor API ====================

    /**
     * 获取军队列表
     */
    public List<ParsedGeneralUnion> getUnionList(int pageNum, int pageSize) throws TException {
        try (VisitorApiClient client = new VisitorApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            UnionList result = client.getClient().unionList(normal, pageNum, pageSize);
            return ParsedGeneralUnion.fromGeneralUnions(result.getGeneral_unions());
        }
    }

    /**
     * 获取当前用户军队信息
     */
    public ParsedUnionAndMe getUnionAndMe() throws TException {
        try (VisitorApiClient client = new VisitorApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            UnionOfMe raw = client.getClient().unionOfMe(normal);
            return ParsedUnionAndMe.fromUnionOfMe(raw);
        }
    }

    /**
     * 申请加入军队
     */
    public void applyUnion(int unionId, String extra) throws TException {
        try (VisitorApiClient client = new VisitorApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().unionApply(normal, unionId, extra);
        }
    }

    /**
     * 创建军队
     */
    public void createUnion(String title, String extra) throws TException {
        try (VisitorApiClient client = new VisitorApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().unionCreate(normal, title, extra);
        }
    }

    // ==================== Member API ====================

    /**
     * 获取军队成员列表
     */
    public List<ParsedMember> getMembers(int unionId) throws TException {
        try (MemberApiClient client = new MemberApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            UnionMembers result = client.getClient().unionMembers(normal, unionId);
            return ParsedMember.fromMembers(result.getMembers());
        }
    }

    /**
     * 获取军队详细信息
     */
    public ParsedUnion getUnionInfo(int unionId) throws TException {
        try (MemberApiClient client = new MemberApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            UnionInfo result = client.getClient().unionInfo(normal, unionId);
            return ParsedUnion.fromMemberUnion(result.getMember_union());
        }
    }

    /**
     * 获取军队日志
     */
    public LogInfo getUnionLog(int pageNum, int pageSize) throws TException {
        try (MemberApiClient client = new MemberApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            return client.getClient().unionLog(normal, pageNum, pageSize);
        }
    }

    /**
     * 设置个人扩展信息
     */
    public void setSelfExtra(String extra) throws TException {
        try (MemberApiClient client = new MemberApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().setMemberExtra(normal, ApiConstants.SELF_TYPE, extra,
                    ApiConstants.NO_TYPE, ApiConstants.NO_TYPE, ApiConstants.NO_TYPE);
        }
    }

    /**
     * 使用个人贡献
     */
    public void useSelfContribution(int contribution) throws TException, IOException {
        updateGameSession();
        try (MemberApiClient client = new MemberApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().deleteContributionPersonal(normal, contribution);
        }
    }

    /**
     * 退出军队
     */
    public void quitUnion() throws TException, IOException {
        updateGameSession();
        try (MemberApiClient client = new MemberApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().unionQuit(normal);
        }
    }

    // ==================== Master API ====================

    /**
     * 获取申请列表
     */
    public List<ParsedGeneralMember> getApplyList(int pageNum, int pageSize) throws TException {
        try (MasterApiClient client = new MasterApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            ApplyList result = client.getClient().applyList(normal, pageNum, pageSize);
            return ParsedGeneralMember.fromGeneralMembers(result.getGeneral_members());
        }
    }

    /**
     * 审核申请成员
     */
    public void auditApplyMember(String targetUid, int targetIndex, boolean approve) throws TException {
        try (MasterApiClient client = new MasterApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().applyAudit(normal, (int) Long.parseLong(targetUid), String.valueOf(targetIndex),
                    approve ? 1 : 0);
        }
    }

    /**
     * 审核目标
     */
    public static class AuditTarget {
        public final String uid;
        public final int index;

        public AuditTarget(String uid, int index) {
            this.uid = uid;
            this.index = index;
        }
    }

    /**
     * 批量审核申请成员（复用单个连接）
     *
     * @param targets 待审核目标列表（uid + index）
     * @param approve true=通过, false=拒绝
     * @return 成功审核的数量
     */
    public int auditApplyMembers(List<AuditTarget> targets, boolean approve) throws TException {
        int successCount = 0;
        TException lastError = null;
        try (MasterApiClient client = new MasterApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            for (AuditTarget target : targets) {
                try {
                    int uid = (int) Long.parseLong(target.uid);
                    client.getClient().applyAudit(normal, uid, String.valueOf(target.index), approve ? 1 : 0);
                    successCount++;
                } catch (TException e) {
                    lastError = e;
                }
            }
        }
        if (successCount == 0 && lastError != null) {
            throw lastError;
        }
        return successCount;
    }

    /**
     * 移除成员
     */
    public void removeMember(int targetUid, int targetIndex) throws TException, IOException {
        updateGameSession();
        try (MasterApiClient client = new MasterApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().memberRemove(normal, targetUid, String.valueOf(targetIndex));
        }
    }

    /**
     * 设置成员角色 uid 可以为无符号的，待修改
     */
    public void setMemberRole(int targetUid, int targetIndex, int roleId) throws TException {
        try (MemberApiClient client = new MemberApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().setRole(normal, targetUid, targetIndex, roleId);
        }
    }

    // ==================== Grow API ====================

    /**
     * 获取任务状态
     */
    public TasksInfo getTasksStatus() throws TException {
        try (GrowApiClient client = new GrowApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            return client.getClient().getTaskValue(normal);
        }
    }

    /**
     * 执行任务
     */
    public void doTask(String taskId) throws TException, IOException {
        updateGameSession();
        try (GrowApiClient client = new GrowApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            client.getClient().doTask(normal, taskId);
        }
    }

    /**
     * 批量执行多个任务（只更新一次 session）
     */
    public void doTasks(String... taskIds) throws TException, IOException {
        updateGameSession();
        try (GrowApiClient client = new GrowApiClient(cookies)) {
            Normal normal = client.buildNormal(archIndex);
            for (String taskId : taskIds) {
                client.getClient().doTask(normal, taskId);
            }
        }
    }

    // ==================== Getters/Setters ====================

    public String getUid() {
        return uid;
    }

    public int getArchIndex() {
        return archIndex;
    }

    public void setArchIndex(int archIndex) {
        this.archIndex = Math.max(0, Math.min(7, archIndex));
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
        this.sessionApi = new GameSessionApi(cookies);
    }

    public String getGameSession() {
        return gameSession;
    }

    public void setGameSession(String gameSession) {
        this.gameSession = gameSession;
    }
    // ==================== Auto Tasks ====================

    public static class TaskRequirement {
        public String id;
        public String name;
        public int target; // executed times (not used in calculation, we use max/cost)
        public int max;
        public int cost;

        public TaskRequirement(String id, String name, int target, int max, int cost) {
            this.id = id;
            this.name = name;
            this.target = target;
            this.max = max;
            this.cost = cost;
        }
    }

    private static final List<TaskRequirement> REQUIRED_TASKS = new ArrayList<>();
    static {
        REQUIRED_TASKS.add(new TaskRequirement("41", "金币任务", 4, 280, 70));
        REQUIRED_TASKS.add(new TaskRequirement("42", "签到任务", 1, 100, 100));
        REQUIRED_TASKS.add(new TaskRequirement("43", "竞技场任务", 1, 100, 100));
        REQUIRED_TASKS.add(new TaskRequirement("44", "升级任务", 1, 100, 100));
        REQUIRED_TASKS.add(new TaskRequirement("92", "地下城任务", 1, 100, 100));
        REQUIRED_TASKS.add(new TaskRequirement("93", "仙境任务", 1, 60, 60));
        REQUIRED_TASKS.add(new TaskRequirement("94", "礼包任务", 1, 60, 60));
        REQUIRED_TASKS.add(new TaskRequirement("106", "联盟任务", 1, 100, 100));
        REQUIRED_TASKS.add(new TaskRequirement("107", "熔炼任务", 1, 100, 100));
        REQUIRED_TASKS.add(new TaskRequirement("108", "活跃任务", 1, 100, 100));
    }

    public static List<TaskRequirement> getRequiredTasks() {
        return REQUIRED_TASKS;
    }

    /**
     * 一键执行所有未完成的任务，并设置conDay 为1100
     *
     * @return 执行结果摘要
     */
    public String oneClickDoTasks() throws TException, IOException {
        return oneClickDoTasks(0);
    }

    /**
     * 一键执行所有未完成的任务，并设置conDay 为1100，同时设置本周累计贡献
     *
     * @param weeklyAccumulatedContribution 本周累计贡献值
     * @return 执行结果摘要
     */
    public String oneClickDoTasks(int weeklyAccumulatedContribution) throws TException, IOException {
        // 1. 获取当前任务状态
        TasksInfo info = getTasksStatus();
        Map<String, Integer> currentValues = new HashMap<>();
        if (info.tasks != null) {
            for (Task t : info.tasks) {
                try {
                    currentValues.put(t.id, Integer.parseInt(t.value));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        // 2. 计算需要执行的任务
        List<String> toExecute = new ArrayList<>();
        StringBuilder log = new StringBuilder();

        for (TaskRequirement req : REQUIRED_TASKS) {
            int current = currentValues.getOrDefault(req.id, 0);
            if (current >= req.max) {
                continue;
            }

            // 计算还需要执行的次数
            int needed = (req.max - current) / req.cost;
            if (needed > 0) {
                for (int i = 0; i < needed; i++) {
                    toExecute.add(req.id);
                }
                log.append(req.name).append("x").append(needed).append(" ");
            }
        }

        if (toExecute.isEmpty()) {
            return "所有任务已达标，无需执行";
        }

        // 3. 批量执行
        doTasks(toExecute.toArray(new String[0]));
        // 日贡日志添加，日贡1100，并设置为1100
        ParsedMember me = getUnionAndMe().member;
        me.detail.conDay = 1100;
        me.detail.loginTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());

        // 设置本周累计贡献
        if (me.detail.conObj != null) {
            me.detail.conObj.thisWeek = weeklyAccumulatedContribution;
        }

        setSelfExtra(me.detail.toExtra());
        return "执行成功: " + log.toString();
    }
}
