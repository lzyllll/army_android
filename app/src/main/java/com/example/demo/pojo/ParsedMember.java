package com.example.demo.pojo;

import com.example.demo.thrift.Member;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 解析后的成员，将 Member.extra 替换为 MemberDetail
 */
public class ParsedMember {

    public int gameId;
    public int unionId;
    public String uid;
    public String username;
    public int index;
    public String nickname;
    public int contribution;
    public MemberDetail detail;
    public String extra2;
    public String activeTime;
    public int roleId;
    public String roleName;

    public static ParsedMember fromMember(Member member) {
        ParsedMember p = new ParsedMember();
        p.gameId = member.game_id;
        p.unionId = member.union_id;
        p.uid = member.uid;
        p.username = member.username;
        p.index = toInt(member.index);
        p.nickname = member.nickname;
        p.contribution = member.contribution;
        p.detail = MemberDetail.fromExtra(member.extra);
        p.extra2 = member.extra2;
        p.activeTime = formatTimestamp(member.active_time);
        p.roleId = toInt(member.role_id);
        p.roleName = member.role_name;
        return p;
    }

    public static List<ParsedMember> fromMembers(List<Member> members) {
        List<ParsedMember> result = new ArrayList<>();
        if (members != null) {
            for (Member m : members) {
                result.add(fromMember(m));
            }
        }
        return result;
    }

    private static int toInt(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";
        try {
            long millis = (long) (Double.parseDouble(timestamp) * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(new Date(millis));
        } catch (NumberFormatException e) {
            return timestamp;
        }
    }

    @Override
    public String toString() {
        return "ParsedMember{" +
                "uid='" + uid + '\'' +
                ", nickname='" + nickname + '\'' +
                ", index=" + index +
                ", contribution=" + contribution +
                ", roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", activeTime='" + activeTime + '\'' +
                ", detail=" + detail +
                '}';
    }
}
