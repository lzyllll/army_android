package com.example.demo.ui.model;

import com.example.demo.api.AccountApi;
import com.example.demo.pojo.AccountInfo;
import com.example.demo.pojo.GameUser;
import com.example.demo.pojo.ParsedMember;
import com.example.demo.pojo.ParsedUnionAndMe;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户账号数据模型
 */
public class UserAccount {
    public String uid;
    public String username;
    public String cookies;
    public List<GameUserData> gameUsers = new ArrayList<>();

    public UserAccount(String uid, String username, String cookies) {
        this.uid = uid;
        this.username = username;
        this.cookies = cookies;
    }

    /**
     * 游戏用户数据
     */
    public static class GameUserData {
        public int archIndex;
        public String title;
        public String datetime;
        public int status;
        public boolean enabled = true;
        public boolean selected = false;

        public GameUser gameUser;
        public ParsedUnionAndMe unionAndMe;

        // 贡献相关
        public int dailyContribution;
        public int weeklyContribution;
        public int lastWeekContribution;
        public int beforeLastWeekContribution;
        public int totalContribution;
        public String lastActionLog;
        public long lastDonateTime;

        // Caches for persistence
        public java.util.List<ParsedMember> cachedMembers;
        public com.example.demo.thrift.TasksInfo cachedTasks;
        public java.util.List<ApplyUserData> cachedApplyList;

        public GameUserData(AccountInfo info, GameUser gameUser) {
            this.archIndex = info.index;
            this.title = info.title;
            this.datetime = info.datetime;
            this.status = info.status;
            this.gameUser = gameUser;
        }

        public boolean isCheated() {
            return status != 0;
        }

        public void updateContribution(ParsedMember member) {
            if (member != null && member.detail != null) {
                this.dailyContribution = member.detail.conDay;
                this.totalContribution = member.contribution;
                if (member.detail.conObj != null) {
                    this.weeklyContribution = member.detail.conObj.thisWeek;
                    this.lastWeekContribution = member.detail.conObj.lastWeek;
                    this.beforeLastWeekContribution = member.detail.conObj.beforeLastWeek;
                }
            }
        }

        public void updateContribution(com.example.demo.thrift.TasksInfo info) {
            if (info != null && info.con_day != null) {
                try {
                    this.dailyContribution = Integer.parseInt(info.con_day);
                } catch (NumberFormatException e) {
                    this.dailyContribution = 0;
                }
            }
        }
    }
}
