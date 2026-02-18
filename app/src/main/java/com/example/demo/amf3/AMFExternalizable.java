package com.example.demo.amf3;

import java.io.IOException;

/**
 * 可外部化对象接口
 *
 * 实现此接口的对象可以完全控制自己的序列化和反序列化过程。
 * 必须实现 write() 方法。
 *
 * 可外部化对象需要通过 AMFDecoder.register() 注册，
 * 以便解码器���道如何反序列化它们。
 */
public interface AMFExternalizable {

    /**
     * 获取类名
     */
    String getClassName();

    /**
     * 将对象写入编码器
     *
     * @param encoder AMF 编码器实例
     */
    void write(AMFEncoder encoder) throws IOException;

    /**
     * 从解码器读取并创建对象实例
     *
     * @param decoder AMF 解码器实例
     * @return 反序列化后的对象实例
     */
    AMFExternalizable read(AMFDecoder decoder) throws IOException;
}
