package com.example.demo.pojo;

import com.example.demo.thrift.GeneralUnion;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析后的公会列表项，将 GeneralUnion.extra 替换为 UnionDetail
 */
public class ParsedGeneralUnion {

    public int id;
    public String name;
    public String creator;
    public int level;
    public int membersNum;
    public UnionDetail detail;
    public int contribution;

    public static ParsedGeneralUnion fromGeneralUnion(GeneralUnion gu) {
        ParsedGeneralUnion p = new ParsedGeneralUnion();
        p.id = gu.id;
        p.name = gu.name;
        p.creator = gu.creator;
        p.level = gu.level;
        p.membersNum = toInt(gu.members_num);
        p.detail = UnionDetail.fromExtra(gu.extra);
        p.contribution = gu.contribution;
        return p;
    }

    public static List<ParsedGeneralUnion> fromGeneralUnions(List<GeneralUnion> unions) {
        List<ParsedGeneralUnion> result = new ArrayList<>();
        if (unions != null) {
            for (GeneralUnion gu : unions) {
                result.add(fromGeneralUnion(gu));
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
        return "ParsedGeneralUnion{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", level=" + level +
                ", membersNum=" + membersNum +
                ", detail=" + detail +
                '}';
    }
}
