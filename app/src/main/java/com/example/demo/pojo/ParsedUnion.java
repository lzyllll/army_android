package com.example.demo.pojo;

import com.example.demo.thrift.MemberUnion;

/**
 * 解析后的公会信息，将 MemberUnion.extra 替换为 UnionDetail
 */
public class ParsedUnion {

    public int id;
    public int gameId;
    public String uid;
    public String username;
    public int index;
    public String nickname;
    public String title;
    public int level;
    public int experience;
    public int contribution;
    public UnionDetail detail;
    public String extra2;
    public String dissolveDate;
    public int count;
    public String transfer;

    public static ParsedUnion fromMemberUnion(MemberUnion union) {
        ParsedUnion p = new ParsedUnion();
        p.id = union.id;
        p.gameId = union.game_id;
        p.uid = union.uid;
        p.username = union.username;
        p.index = toInt(union.index);
        p.nickname = union.nickname;
        p.title = union.title;
        p.level = union.level;
        p.experience = union.experience;
        p.contribution = union.contribution;
        p.detail = UnionDetail.fromExtra(union.extra);
        p.extra2 = union.extra2;
        p.dissolveDate = "0".equals(union.dissolve_date) ? null : union.dissolve_date;
        p.count = toInt(union.members_num);
        p.transfer = union.transfer;
        return p;
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
        return "ParsedUnion{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", level=" + level +
                ", count=" + count +
                ", detail=" + detail +
                '}';
    }
}
