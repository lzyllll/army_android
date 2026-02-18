package com.example.demo.amf3;

import java.util.HashMap;
import java.util.Map;

/**
 * 可序列化对象基类
 *
 * Java 对象如果想要被序列化为"命名对象"（带有类名的对象），
 * 应该继承这个类。
 *
 * 编码器会序列化对象的所有字段，除非：
 * 1. 字段名以双下划线（__）开头
 * 2. 对象定义了 getSerializableFields() 方法来指定要序列化的字段
 */
public class AMFSerializable extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    /** 对象的类名 */
    public String __class;

    /** 是否为动态对象 */
    public Boolean __dynamic;

    public AMFSerializable() {
        this.__class = "";
        this.__dynamic = true;
    }

    public AMFSerializable(String className) {
        this.__class = className;
        this.__dynamic = true;
    }

    public AMFSerializable(String className, boolean dynamic) {
        this.__class = className;
        this.__dynamic = dynamic;
    }

    /**
     * 获取应该被序列化的字段列表
     * 子类可以覆盖此方法来控制哪些字段会被序列化
     */
    public String[] getSerializableFields() {
        return null; // null 表示序列化所有字段
    }
}
