package com.example.demo.utils;

import java.util.Base64;
import com.example.demo.amf3.AMFDecoder;
import com.example.demo.amf3.AMFEncoder;
import com.example.demo.amf3.AMFSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * extra 字段解析工具
 * extra 是 base64 编码的 AMF3 二进制数据
 */
public class ExtraParser {

    /**
     * 解析 extra 字符串为 Map
     * 流程: base64 decode → AMF3 decode → Map
     *
     * @param extra base64编码的AMF3数据
     * @return 解析后的键值对，解析失败返回空 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String extra) {
        if (extra == null || extra.isEmpty()) {
            return new HashMap<>();
        }
        try {
            byte[] data = Base64.getDecoder().decode(extra);
            AMFDecoder decoder = new AMFDecoder(data);
            Object result = decoder.decode();
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 将 AMFSerializable 对象编码为 extra 字符串
     * 流程: AMFSerializable → AMF3 encode → base64 encode
     *
     * @param obj AMF3 可序列化对象
     * @return base64编码的字符串
     */
    public static String encode(AMFSerializable obj) throws IOException {
        AMFEncoder encoder = new AMFEncoder();
        encoder.writeObject(obj);
        byte[] data = encoder.getBuffer();
        return Base64.getEncoder().encodeToString(data);
    }
}
