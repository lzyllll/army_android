package com.example.demo.pojo;

import java.util.Base64;

import com.example.demo.amf3.AMFSerializable;
import com.example.demo.utils.ExtraParser;

import java.util.HashMap;
import java.util.Map;

/**
 * union.extra 解析后的公会详情
 *
 * AMF3 静态字段顺序:
 * notice, enemyUnionId, money, dps, weUnionId, bs, kingName, url, vipLimit, dpsLimit, life
 *
 * 注意: notice 和 url 在原始数据中是 base64 编码的字符串
 */
public class UnionDetail {

    /** 公告 (已解码) */
    public String notice = "";
    /** 敌对公会ID */
    public int enemyUnionId = 0;
    /** 公会资金 */
    public int money = 0;
    /** 战力 */
    public int dps = 0;
    /** 友好公会ID */
    public int weUnionId = 0;
    /** bs */
    public int bs = 0;
    /** 会长名称 */
    public String kingName = "";
    /** 公会链接 (已解码) */
    public String url = "";
    /** VIP限制 */
    public int vipLimit = 0;
    /** 战力限制 */
    public int dpsLimit = 0;
    /** 生命 */
    public int life = 0;

    /** 原始解析数据，还原时用于补全未定义的字段 */
    private Map<String, Object> rawArgs = new HashMap<>();

    /**
     * 从 union.extra 字符串解析
     */
    public static UnionDetail fromExtra(String extra) {
        Map<String, Object> args = ExtraParser.parse(extra);
        UnionDetail detail = new UnionDetail();
        detail.rawArgs = new HashMap<>(args);

        // dps 处理 NaN
        Object rawDps = args.get("dps");
        if (rawDps instanceof Number) {
            double d = ((Number) rawDps).doubleValue();
            detail.dps = Double.isNaN(d) ? 0 : (int) d;
        }

        detail.enemyUnionId = toInt(args.get("enemyUnionId"));
        detail.money = toInt(args.get("money"));
        detail.weUnionId = toInt(args.get("weUnionId"));
        detail.bs = toInt(args.get("bs"));
        detail.kingName = toStr(args.get("kingName"));
        detail.vipLimit = toInt(args.get("vipLimit"));
        detail.dpsLimit = toInt(args.get("dpsLimit"));
        detail.life = toInt(args.get("life"));

        // notice 和 url 是 base64 编码的字符串，需要再解码一层
        detail.notice = decodeBase64Field(toStr(args.get("notice")));
        detail.url = decodeBase64Field(toStr(args.get("url")));

        return detail;
    }

    /**
     * 转为 AMFSerializable 用于编码上传
     * 先以 rawArgs 为底，再覆盖已知字段，保证未定义的字段不丢失
     * notice 和 url 需要 base64 编码回去
     */
    public AMFSerializable toAMFSerializable() {
        AMFSerializable obj = new AMFSerializable("", false);
        obj.__dynamic = false;
        // 先放入原始数据作为底
        for (Map.Entry<String, Object> entry : rawArgs.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("__")) {
                obj.put(key, entry.getValue());
            }
        }
        // 再覆盖已知字段
        obj.put("notice", encodeBase64Field(notice));
        obj.put("enemyUnionId", enemyUnionId);
        obj.put("money", money);
        obj.put("dps", dps);
        obj.put("weUnionId", weUnionId);
        obj.put("bs", bs);
        obj.put("kingName", kingName);
        obj.put("url", encodeBase64Field(url));
        obj.put("vipLimit", vipLimit);
        obj.put("dpsLimit", dpsLimit);
        obj.put("life", life);
        return obj;
    }

    /**
     * 编码回 extra 字符串
     */
    public String toExtra() {
        try {
            return ExtraParser.encode(toAMFSerializable());
        } catch (Exception e) {
            return "";
        }
    }

    // === 工具方法 ===

    private static String decodeBase64Field(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return new String(decoded, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static String encodeBase64Field(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            return Base64.getEncoder().encodeToString(value.getBytes());
        } catch (Exception e) {
            return "";
        }
    }

    private static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    private static String toStr(Object value) {
        if (value == null) return "";
        return value.toString();
    }

    @Override
    public String toString() {
        return "UnionDetail{" +
                "notice='" + notice + '\'' +
                ", enemyUnionId=" + enemyUnionId +
                ", money=" + money +
                ", dps=" + dps +
                ", weUnionId=" + weUnionId +
                ", bs=" + bs +
                ", kingName='" + kingName + '\'' +
                ", url='" + url + '\'' +
                ", vipLimit=" + vipLimit +
                ", dpsLimit=" + dpsLimit +
                ", life=" + life +
                '}';
    }
}
