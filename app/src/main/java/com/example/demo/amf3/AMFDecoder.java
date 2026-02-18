package com.example.demo.amf3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AMF3 解码器
 *
 * 仅实现 AMF3，并使用类方法进行分发解码
 */
public class AMFDecoder extends AMFReader {

    /** AMF3 外部化对象注册表 */
    private static final Map<String, AMFExternalizable> externalizables = new HashMap<>();

    /** AMF3 字符串引用表 */
    private final List<String> stringReferences = new ArrayList<>();

    /** AMF3 对象引用表 */
    private final List<Object> objectReferences = new ArrayList<>();

    /** AMF3 Trait 引用表 */
    private final List<AMFTrait> traitReferences = new ArrayList<>();

    /**
     * 注册外部化类型
     */
    public static void register(String className, AMFExternalizable externalizable) {
        externalizables.put(className, externalizable);
    }

    public AMFDecoder(byte[] data) {
        super(data);
    }

    /**
     * 解码一个 AMF3 值
     */
    public Object decode() throws IOException {
        int typeId = readByte();
        return readByTypeId(typeId);
    }

    /**
     * 按类型 ID 分发解码
     */
    private Object readByTypeId(int typeId) throws IOException {
        switch (typeId) {
            case 0x00: // UNDEFINED
                return null;
            case 0x01: // NULL
                return null;
            case 0x02: // FALSE
                return false;
            case 0x03: // TRUE
                return true;
            case 0x04: // INTEGER
                return readInt29();
            case 0x05: // DOUBLE
                return readDoubleBE();
            case 0x06: // STRING
                return readAMF3String();
            case 0x07: // XML_DOC
                return readAMF3XmlDoc();
            case 0x08: // DATE
                return readAMF3Date();
            case 0x09: // ARRAY
                return readAMF3Array();
            case 0x0A: // OBJECT
                return readAMF3Object();
            case 0x0B: // XML
                return readAMF3Xml();
            case 0x0C: // BYTE_ARRAY
                return readAMF3ByteArray();
            case 0x0D: // VECTOR_INT
                return readAMF3VectorInt();
            case 0x0E: // VECTOR_UINT
                return readAMF3VectorUInt();
            case 0x0F: // VECTOR_DOUBLE
                return readAMF3VectorDouble();
            case 0x10: // VECTOR_OBJECT
                return readAMF3VectorObject();
            case 0x11: // DICTIONARY
                return readAMF3Dictionary();
            default:
                throw new IOException("当前 AMF3 解码器暂不支持类型 ID: 0x" + Integer.toHexString(typeId));
        }
    }

    /**
     * 读取 AMF3 字符串
     */
    private String readAMF3String() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= stringReferences.size()) {
                throw new IOException("无效的 AMF3 字符串引用: " + refIndex);
            }
            return stringReferences.get(refIndex);
        }

        if (header.value == 0) {
            return "";
        }

        String value = readUTF8String(header.value);
        stringReferences.add(value);
        return value;
    }

    /**
     * 读取 AMF3 日期
     */
    private Date readAMF3Date() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 日期引用: " + refIndex);
            }
            Object ref = objectReferences.get(refIndex);
            if (!(ref instanceof Date)) {
                throw new IOException("无效的 AMF3 日期引用");
            }
            return (Date) ref;
        }

        double timestamp = readDoubleBE();
        Date value = new Date((long) timestamp);
        objectReferences.add(value);
        return value;
    }

    /**
     * 读取 AMF3 数组
     */
    private Object readAMF3Array() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 数组引用: " + refIndex);
            }
            return objectReferences.get(refIndex);
        }

        // 先读取关联部分（key-value）
        Map<String, Object> named = new HashMap<>();
        objectReferences.add(named);
        int idx = objectReferences.size() - 1;

        while (true) {
            String key = readAMF3String();
            if (key == null || key.isEmpty()) {
                break;
            }
            named.put(key, decode());
        }

        // 如果有关联部分，直接返回 Map
        if (!named.isEmpty()) {
            return named;
        }

        // 否则读取密集数组
        List<Object> dense = new ArrayList<>();
        objectReferences.set(idx, dense);
        for (int i = 0; i < header.value; i++) {
            dense.add(decode());
        }
        return dense;
    }

    /**
     * 读取 AMF3 对象 Trait
     */
    private AMFTrait readAMF3ObjectTrait(int flags) throws IOException {
        if ((flags & 1) == 0) {
            int traitIndex = flags >> 1;
            if (traitIndex < 0 || traitIndex >= traitReferences.size()) {
                throw new IOException("无效的 AMF3 Trait 引用: " + traitIndex);
            }
            return traitReferences.get(traitIndex);
        }

        String name = readAMF3String();
        boolean isExternalizable = ((flags >> 1) & 1) == 1;
        boolean isDynamic = ((flags >> 2) & 1) == 1;
        int staticKeyLen = flags >> 3;

        AMFTrait trait = new AMFTrait(name, isDynamic, isExternalizable);
        for (int i = 0; i < staticKeyLen; i++) {
            trait.staticFields.add(readAMF3String());
        }

        traitReferences.add(trait);
        return trait;
    }

    /**
     * 读取 AMF3 对象
     */
    private Object readAMF3Object() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 对象引用: " + refIndex);
            }
            return objectReferences.get(refIndex);
        }

        AMFTrait trait = readAMF3ObjectTrait(header.value);

        // 处理外部化对象
        if (trait.externalizable) {
            if ("flex.messaging.io.ArrayCollection".equals(trait.name)) {
                Object arr = decode();
                objectReferences.add(arr);
                return arr;
            }

            AMFExternalizable ext = externalizables.get(trait.name);
            if (ext == null) {
                throw new IOException("未注册 AMF3 外部化类型: " + trait.name);
            }

            AMFExternalizable extObj = ext.read(this);
            objectReferences.add(extObj);
            return extObj;
        }

        // 处理普通对象
        AMFSerializable result = new AMFSerializable(trait.name, trait.dynamic);
        objectReferences.add(result);

        // 读取静态字段
        for (String field : trait.staticFields) {
            result.put(field, decode());
        }

        // 读取动态字段
        if (trait.dynamic) {
            while (true) {
                String key = readAMF3String();
                if (key == null || key.isEmpty()) {
                    break;
                }
                result.put(key, decode());
            }
        }

        return result;
    }

    /**
     * 读取 AMF3 ByteArray
     */
    private byte[] readAMF3ByteArray() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 ByteArray 引用: " + refIndex);
            }
            Object ref = objectReferences.get(refIndex);
            if (!(ref instanceof byte[])) {
                throw new IOException("无效的 AMF3 ByteArray 引用");
            }
            return (byte[]) ref;
        }

        byte[] bytes = readBytes(header.value);
        objectReferences.add(bytes);
        return bytes;
    }

    /**
     * 读取 AMF3 XML Doc
     */
    private String readAMF3XmlDoc() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 XML 引用: " + refIndex);
            }
            return (String) objectReferences.get(refIndex);
        }

        String xml = readUTF8String(header.value);
        objectReferences.add(xml);
        return xml;
    }

    /**
     * 读取 AMF3 XML
     */
    private String readAMF3Xml() throws IOException {
        return readAMF3XmlDoc();
    }

    /**
     * 读取 VECTOR_INT
     */
    private List<Integer> readAMF3VectorInt() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 向量引用: " + refIndex);
            }
            @SuppressWarnings("unchecked")
            List<Integer> ref = (List<Integer>) objectReferences.get(refIndex);
            return ref;
        }

        readByte(); // fixed 标记
        List<Integer> result = new ArrayList<>();
        objectReferences.add(result);
        for (int i = 0; i < header.value; i++) {
            result.add(readInt32BE());
        }
        return result;
    }

    /**
     * 读取 VECTOR_UINT
     */
    private List<Long> readAMF3VectorUInt() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 向量引用: " + refIndex);
            }
            @SuppressWarnings("unchecked")
            List<Long> ref = (List<Long>) objectReferences.get(refIndex);
            return ref;
        }

        readByte(); // fixed 标记
        List<Long> result = new ArrayList<>();
        objectReferences.add(result);
        for (int i = 0; i < header.value; i++) {
            result.add(readUInt32BE());
        }
        return result;
    }

    /**
     * 读取 VECTOR_DOUBLE
     */
    private List<Double> readAMF3VectorDouble() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 向量引用: " + refIndex);
            }
            @SuppressWarnings("unchecked")
            List<Double> ref = (List<Double>) objectReferences.get(refIndex);
            return ref;
        }

        readByte(); // fixed 标记
        List<Double> result = new ArrayList<>();
        objectReferences.add(result);
        for (int i = 0; i < header.value; i++) {
            result.add(readDoubleBE());
        }
        return result;
    }

    /**
     * 读取 VECTOR_OBJECT
     */
    private List<Object> readAMF3VectorObject() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 向量引用: " + refIndex);
            }
            @SuppressWarnings("unchecked")
            List<Object> ref = (List<Object>) objectReferences.get(refIndex);
            return ref;
        }

        readByte(); // fixed 标记
        List<Object> result = new ArrayList<>();
        objectReferences.add(result);
        for (int i = 0; i < header.value; i++) {
            result.add(decode());
        }
        return result;
    }

    /**
     * 读取 DICTIONARY
     */
    private Map<String, Object> readAMF3Dictionary() throws IOException {
        AMFHeader header = readAMFHeader();
        if (!header.isDef) {
            int refIndex = header.value;
            if (refIndex < 0 || refIndex >= objectReferences.size()) {
                throw new IOException("无效的 AMF3 字典引用: " + refIndex);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> ref = (Map<String, Object>) objectReferences.get(refIndex);
            return ref;
        }

        readByte(); // weakKeys 标记
        Map<String, Object> result = new HashMap<>();
        objectReferences.add(result);

        for (int i = 0; i < header.value; i++) {
            Object key = decode();
            Object value = decode();
            result.put(String.valueOf(key), value);
        }
        return result;
    }
}
