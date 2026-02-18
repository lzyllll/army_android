package com.example.demo.amf3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AMF3 编码器
 *
 * 仅实现 AMF3，并使用类方法进行分发编码
 */
public class AMFEncoder extends AMFWriter {

    /** AMF3 对象引用表 */
    private final List<Object> objectReferences = new ArrayList<>();

    /** AMF3 字符串引用表 */
    private final List<String> stringReferences = new ArrayList<>();

    /** 忽略的字段前缀 */
    private static final Set<String> IGNORED_PREFIXES = new HashSet<>(Arrays.asList("__"));

    public AMFEncoder() {
        super();
    }

    /**
     * 编码一个 AMF3 值（会写入类型标记）
     */
    public void writeObject(Object value) throws IOException {
        encodeValue(value);
    }

    /**
     * 编码值
     * 遇见一个bug: serializable由于继承hashmap，导致生效不了，
     * 修改：提前了这个判断
     */
    private void encodeValue(Object value) throws IOException {
        if (value == null) {
            writeByte(0x01); // NULL
            return;
        }

        // 基本类型处理
        if (value instanceof Boolean) {
            writeByte((Boolean) value ? 0x03 : 0x02); // TRUE or FALSE
            return;
        }

        if (value instanceof Integer) {
            int intVal = (Integer) value;
            if (intVal >= -268435456 && intVal <= 536870911) {
                writeByte(0x04); // INTEGER
                writeInt29(intVal);
            } else {
                writeByte(0x05); // DOUBLE
                writeDoubleBE(intVal);
            }
            return;
        }

        if (value instanceof Long) {
            long longVal = (Long) value;
            if (longVal >= -268435456 && longVal <= 536870911) {
                writeByte(0x04); // INTEGER
                writeInt29((int) longVal);
            } else {
                writeByte(0x05); // DOUBLE
                writeDoubleBE(longVal);
            }
            return;
        }

        if (value instanceof Number) {
            writeByte(0x05); // DOUBLE
            writeDoubleBE(((Number) value).doubleValue());
            return;
        }

        if (value instanceof String) {
            encodeString((String) value);
            return;
        }

        if (value instanceof Date) {
            encodeDate((Date) value);
            return;
        }

        if (value instanceof byte[]) {
            encodeByteArray((byte[]) value);
            return;
        }

        if (value instanceof List) {
            encodeArray((List<?>) value);
            return;
        }

        if (value instanceof AMFExternalizable) {
            encodeExternalizable((AMFExternalizable) value);
            return;
        }

        if (value instanceof AMFSerializable) {
            encodeSerializable((AMFSerializable) value);
            return;
        }

        if (value instanceof Map) {
            encodeMap((Map<?, ?>) value);
            return;
        }

        throw new IOException("不支持的类型: " + value.getClass().getName());
    }

    /**
     * 编码字符串
     */
    private void encodeString(String value) throws IOException {
        // 检查引用
        int refIndex = stringReferences.indexOf(value);
        if (refIndex != -1 && !value.isEmpty()) {
            writeByte(0x06); // STRING
            writeInt29(refIndex << 1);
            return;
        }

        if (!value.isEmpty()) {
            stringReferences.add(value);
        }

        writeByte(0x06); // STRING
        writeInlineString(value);
    }

    /**
     * 写入 AMF3 内联字符串（无类型标记）
     */
    private void writeInlineString(String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeInt29((bytes.length << 1) | 1);
        writeBytes(bytes);
    }

    /**
     * 编码日期
     */
    private void encodeDate(Date value) throws IOException {
        writeByte(0x08); // DATE
        writeInt29(1); // 定义标记
        writeDoubleBE(value.getTime());
    }

    /**
     * 编码数组（密集数组）
     */
    private void encodeArray(List<?> value) throws IOException {
        // 检查引用
        int refIndex = objectReferences.indexOf(value);
        if (refIndex != -1) {
            writeByte(0x09); // ARRAY
            writeInt29(refIndex << 1);
            return;
        }

        objectReferences.add(value);
        writeByte(0x09); // ARRAY
        writeInt29((value.size() << 1) | 1);
        writeInlineString(""); // 空的关联部分

        for (Object item : value) {
            encodeValue(item);
        }
    }

    /**
     * 编码 Map（作为关联数组或对象）
     */
    private void encodeMap(Map<?, ?> value) throws IOException {
        // 检查引用
        int refIndex = objectReferences.indexOf(value);
        if (refIndex != -1) {
            writeByte(0x09); // ARRAY
            writeInt29(refIndex << 1);
            return;
        }

        objectReferences.add(value);

        // 检查是否是密集数组（全是数字索引）
        boolean isDense = true;
        int maxIndex = -1;
        for (Object key : value.keySet()) {
            if (key instanceof Integer) {
                int idx = (Integer) key;
                if (idx > maxIndex) maxIndex = idx;
            } else {
                isDense = false;
                break;
            }
        }

        writeByte(0x09); // ARRAY

        if (isDense && maxIndex == value.size() - 1) {
            // 密集数组
            writeInt29((value.size() << 1) | 1);
            writeInlineString("");
            for (int i = 0; i < value.size(); i++) {
                encodeValue(value.get(i));
            }
        } else {
            // 关联数组
            writeInt29(0x01);
            for (Map.Entry<?, ?> entry : value.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (key.startsWith("__")) continue;
                writeInlineString(key);
                encodeValue(entry.getValue());
            }
            writeInlineString("");
        }
    }

    /**
     * 编码 Serializable 对象
     */
    private void encodeSerializable(AMFSerializable value) throws IOException {
        // 检查引用
        int refIndex = objectReferences.indexOf(value);
        if (refIndex != -1) {
            writeByte(0x0A); // OBJECT
            writeInt29(refIndex << 1);
            return;
        }

        objectReferences.add(value);

        boolean isDynamic = value.__dynamic == null || value.__dynamic;
        String className = value.__class != null ? value.__class : "";

        if (isDynamic && className.isEmpty()) {
            // 动态对象
            writeByte(0x0A); // OBJECT
            writeInt29(0x0B); // 动态对象标记
            writeInt29(0x01);

            for (Map.Entry<String, Object> entry : value.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("__")) continue;
                writeInlineString(key);
                encodeValue(entry.getValue());
            }
            writeInlineString("");
        } else {
            // 静态对象
            String[] fields = value.getSerializableFields();
            List<String> keys;
            if (fields != null) {
                keys = Arrays.asList(fields);
            } else {
                keys = new ArrayList<>();
                for (String key : value.keySet()) {
                    if (!key.startsWith("__")) {
                        keys.add(key);
                    }
                }
            }

            int header = keys.size() << 4;
            header |= 0x03; // 动态 + 定义标记

            writeByte(0x0A); // OBJECT
            writeInt29(header);
            writeInlineString(className);

            // 写入字段名
            for (String key : keys) {
                writeInlineString(key);
            }

            // 写入字段值
            for (String key : keys) {
                encodeValue(value.get(key));
            }
        }
    }

    /**
     * 编码 Externalizable 对象
     */
    private void encodeExternalizable(AMFExternalizable value) throws IOException {
        // 检查引用
        int refIndex = objectReferences.indexOf(value);
        if (refIndex != -1) {
            writeByte(0x0A); // OBJECT
            writeInt29(refIndex << 1);
            return;
        }

        objectReferences.add(value);

        int header = 0x07; // externalizable + 动态 + 定义标记
        writeByte(0x0A); // OBJECT
        writeInt29(header);
        writeInlineString(value.getClassName());

        value.write(this);
    }

    /**
     * 编码 ByteArray
     */
    private void encodeByteArray(byte[] value) throws IOException {
        // 检查引用
        int refIndex = objectReferences.indexOf(value);
        if (refIndex != -1) {
            writeByte(0x0C); // BYTE_ARRAY
            writeInt29(refIndex << 1);
            return;
        }

        objectReferences.add(value);
        writeByte(0x0C); // BYTE_ARRAY
        writeInt29((value.length << 1) | 1);
        writeBytes(value);
    }
}
