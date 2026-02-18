package com.example.demo.pojo;

import com.example.demo.thrift.GeneralMember;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析后的申请成员，将 GeneralMember.extra 替换为 MemberDetail
 */
public class ParsedGeneralMember {

    public int gameId;
    public int unionId;
    public String uid;
    public String username;
    public int index;
    public String nickname;
    public MemberDetail detail;

    public static ParsedGeneralMember fromGeneralMember(GeneralMember gm) {
        ParsedGeneralMember p = new ParsedGeneralMember();
        p.gameId = gm.game_id;
        p.unionId = gm.union_id;
        p.uid = gm.uid;
        p.username = gm.username;
        p.index = toInt(gm.index);
        p.nickname = gm.nickname;
        p.detail = MemberDetail.fromExtra(gm.extra);
        return p;
    }

    public static List<ParsedGeneralMember> fromGeneralMembers(List<GeneralMember> members) {
        List<ParsedGeneralMember> result = new ArrayList<>();
        if (members != null) {
            for (GeneralMember gm : members) {
                result.add(fromGeneralMember(gm));
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

    @Override
    public String toString() {
        return "ParsedGeneralMember{" +
                "uid='" + uid + '\'' +
                ", nickname='" + nickname + '\'' +
                ", index=" + index +
                ", detail=" + detail +
                '}';
    }
}
