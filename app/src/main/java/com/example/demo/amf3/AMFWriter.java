package com.example.demo.amf3;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * AMF3 二进制数据写入器
 *
 * 提供向动态增长的缓冲区写入各种二进制数据类型的方法
 * 所有多字节数据都使用大端序（Big Endian）写入，这是 AMF 协议的要求
 */
public class AMFWriter {

    private final ByteArrayOutputStream baos;
    private final DataOutputStream dos;

    public AMFWriter() {
        this.baos = new ByteArrayOutputStream();
        this.dos = new DataOutputStream(baos);
    }

    public int getLength() {
        return baos.size();
    }

    public byte[] getBuffer() {
        return baos.toByteArray();
    }

    public void clear() {
        baos.reset();
    }

    public void writeByte(int value) throws IOException {
        dos.writeByte(value & 0xFF);
    }

    public void writeBytes(byte[] bytes) throws IOException {
        dos.write(bytes);
    }

    public void writeBytes(List<Byte> bytes) throws IOException {
        for (byte b : bytes) {
            dos.writeByte(b);
        }
    }

    public void writeUInt16BE(int value) throws IOException {
        dos.writeShort(value & 0xFFFF);
    }

    public void writeInt16BE(int value) throws IOException {
        dos.writeShort(value);
    }

    public void writeUInt32BE(long value) throws IOException {
        dos.writeInt((int) (value & 0xFFFFFFFFL));
    }

    public void writeInt32BE(int value) throws IOException {
        dos.writeInt(value);
    }

    public void writeDoubleBE(double value) throws IOException {
        dos.writeDouble(value);
    }

    /**
     * 写入 AMF0 格式的字符串
     * AMF0 字符串格式：2字节长度（大端序）+ UTF-8 编码的字符串数据
     */
    public void writeString(String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeUInt16BE(bytes.length);
        dos.write(bytes);
    }

    /**
     * 写入 AMF3 的变长整数（Int29）
     *
     * AMF3 使用一种可变长度编码来表示 29 位有符号整数
     * 每个字节的最高位表示是否还有更多字节：
     * - 值 0-127: 单字节 (0xxxxxxx)
     * - 值 128-16383: 双字节 (1xxxxxxx 0xxxxxxx)
     * - 值 16384-2097151: 三字节 (1xxxxxxx 1xxxxxxx 0xxxxxxx)
     * - 值 2097152-536870911: 四字节 (1xxxxxxx 1xxxxxxx 1xxxxxxx xxxxxxxx)
     *
     * @param value 29 位有符号整数，范围 -268435456 到 536870911
     * @throws IOException 如果值超出范围
     */
    public void writeInt29(int value) throws IOException {
        // 检查范围
        if (value > 536870911 || value < -268435456) {
            throw new IllegalArgumentException("Int29 值超出范围: " + value);
        }

        // 处理负数：转换为无符号形式
        value &= 0x1FFFFFFF;

        if (value < 0x80) {
            // 单字节
            writeByte(value);
        } else if (value < 0x4000) {
            // 双字节
            writeByte((value >> 7 & 0x7F) | 0x80);
            writeByte(value & 0x7F);
        } else if (value < 0x200000) {
            // 三字节
            writeByte((value >> 14 & 0x7F) | 0x80);
            writeByte((value >> 7 & 0x7F) | 0x80);
            writeByte(value & 0x7F);
        } else {
            // 四字节
            writeByte((value >> 22 & 0x7F) | 0x80);
            writeByte((value >> 14 & 0x7F) | 0x80);
            writeByte((value >> 7 & 0x7F) | 0x80);
            writeByte(value & 0xFF);
        }
    }
}
