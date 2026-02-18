package com.example.demo.pojo;

import com.example.demo.thrift.Compose;
import com.example.demo.thrift.UnionOfMe;

/**
 * 解析后的"我的公会"信息
 * 将 UnionOfMe 中 Compose 的 member.extra 和 member_union.extra 都解析
 */
public class ParsedUnionAndMe {

    public ParsedMember member;
    public ParsedUnion union;

    public static ParsedUnionAndMe fromUnionOfMe(UnionOfMe unionOfMe) {
        ParsedUnionAndMe result = new ParsedUnionAndMe();
        Compose compose = unionOfMe.compose;
        if (compose != null) {
            if (compose.member != null) {
                result.member = ParsedMember.fromMember(compose.member);
            }
            if (compose.member_union != null) {
                result.union = ParsedUnion.fromMemberUnion(compose.member_union);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "ParsedUnionAndMe{" +
                "member=" + member +
                ", union=" + union +
                '}';
    }
}
