package com.example.demo.ui.model;

import com.example.demo.pojo.ParsedGeneralMember;

/**
 * 申请用户数据模型
 */
public class ApplyUserData {
    public ParsedGeneralMember member;
    public boolean selected;

    public ApplyUserData(ParsedGeneralMember member) {
        this.member = member;
        this.selected = false;
    }

    public String getPlayerName() {
        return member != null && member.detail != null ? member.detail.playerName : member.nickname;
    }

    public int getLevel() {
        return member != null && member.detail != null ? member.detail.lv : 0;
    }

    public double getDps() {
        return member != null && member.detail != null ? member.detail.dps : 0;
    }

    public String getVip() {
        return member != null && member.detail != null ? member.detail.vip : "";
    }

    public int getTargetUid() {
        if (member == null || member.uid == null) return 0;
        try {
            return Integer.parseInt(member.uid);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getTargetIndex() {
        return member != null ? member.index : 0;
    }

    public String getLoginTime(){return member != null ? member.detail.loginTime : ""; }
}
