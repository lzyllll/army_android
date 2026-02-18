package com.example.demo.amf3;

import java.util.ArrayList;
import java.util.List;

/**
 * AMF3 对象特征类
 *
 * 用于描述 AMF3 对象的元数据信息
 * 包括类名、是否为动态对象、是否可外部化、以及静态字段列表
 *
 * 在 AMF3 中，对象特征可以被引用以减少数据大小
 * 当多个相同类型的对象被序列化时，只需要序列化一次特征信息
 */
public class AMFTrait {

    /** 对象的类名（可以为空表示匿名对象） */
    public String name;

    /** 是否为动态对象 */
    public boolean dynamic;

    /** 是否可外部化 */
    public boolean externalizable;

    /** 静态字段名列表 */
    public List<String> staticFields;

    public AMFTrait(String name, boolean dynamic, boolean externalizable) {
        this.name = name;
        this.dynamic = dynamic;
        this.externalizable = externalizable;
        this.staticFields = new ArrayList<>();
    }
}
