package com.example.demo.pojo;

import com.example.demo.utils.WeekCode;

import java.util.HashMap;
import java.util.Map;

/**
 * 周贡献数据
 * 从 member.extra 中的 conObj 字段解析
 * conObj 的 key 是周码字符串（如 "341"），value 是该周的贡献值
 */
public class ConObj {

    /** 本周贡献 */
    public int thisWeek;
    /** 上周贡献 */
    public int lastWeek;
    /** 上上周贡献 */
    public int beforeLastWeek;

    public ConObj() {
        this(0, 0, 0);
    }

    public ConObj(int thisWeek, int lastWeek, int beforeLastWeek) {
        this.thisWeek = thisWeek;
        this.lastWeek = lastWeek;
        this.beforeLastWeek = beforeLastWeek;
    }

    /**
     * 从 AMF3 解码后的原始 conObj Map 构建
     * key 是周码字符串，value 是贡献值
     */
    @SuppressWarnings("unchecked")
    public static ConObj fromRawObj(Object rawObj) {
        if (rawObj == null) {
            return new ConObj();
        }
        Map<String, Object> map;
        if (rawObj instanceof Map) {
            map = (Map<String, Object>) rawObj;
        } else {
            return new ConObj();
        }

        String[] codes = WeekCode.getWeekCodes();
        return new ConObj(
                toInt(map.get(codes[0])),
                toInt(map.get(codes[1])),
                toInt(map.get(codes[2]))
        );
    }

    /**
     * 转回原始格式用于上传
     * key 是周码字符串，value 是贡献值
     */
    public Map<String, Object> toRawObj() {
        String[] codes = WeekCode.getWeekCodes();
        Map<String, Object> map = new HashMap<>();
        map.put(codes[0], thisWeek);
        map.put(codes[1], lastWeek);
        map.put(codes[2], beforeLastWeek);
        return map;
    }

    private static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    @Override
    public String toString() {
        return "ConObj{thisWeek=" + thisWeek +
                ", lastWeek=" + lastWeek +
                ", beforeLastWeek=" + beforeLastWeek + "}";
    }
}
