package com.example.demo.amf3;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AMF3 二进制数据读取器
 *
 * 提供从字节数组中读取各种二进制数据类型的方法
 * 所有多字节数据都使用大端序（Big Endian）读取，这是 AMF 协议的要求
 */
public class AMFReader {

    protected DataInputStream dis;
    protected int position;

    public AMFReader(byte[] data) {
        this.dis = new DataInputStream(new ByteArrayInputStream(data));
        this.position = 0;
    }

    public int getPosition() {
        return position;
    }

    public int getBytesAvailable() throws IOException {
        return dis.available();
    }

    public int readByte() throws IOException {
        position++;
        return dis.readByte() & 0xFF;
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        position += length;
        return bytes;
    }

    public int readUInt8() throws IOException {
        return readByte();
    }

    public int readInt8() throws IOException {
        position++;
        return dis.readByte();
    }

    public int readUInt16BE() throws IOException {
        position += 2;
        return dis.readUnsignedShort();
    }

    public int readInt16BE() throws IOException {
        position += 2;
        return dis.readShort();
    }

    public long readUInt32BE() throws IOException {
        position += 4;
        return dis.readInt() & 0xFFFFFFFFL;
    }

    public int readInt32BE() throws IOException {
        position += 4;
        return dis.readInt();
    }

    public double readDoubleBE() throws IOException {
        position += 8;
        return dis.readDouble();
    }

    /**
     * 读取 AMF0 格式的字符串
     * AMF0 字符串格式：2字节长度 + UTF-8 编码的字符串数据
     */
    public String readString() throws IOException {
        int length = readUInt16BE();
        if (length == 0) {
            return "";
        }
        return readUTF8String(length);
    }

    /**
     * 读取指定长度的 UTF-8 字符串
     */
    public String readUTF8String(int length) throws IOException {
        byte[] bytes = readBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 读取 AMF3 的变长整数（Int29）
     *
     * AMF3 使用一种可变长度编码来表示 29 位有符号整数
     * 每个字节的最高位表示是否还有更多字节：
     * - 0xxxxxxx: 单字节，值 0-127
     * - 1xxxxxxx 0xxxxxxx: 双字节
     * - 1xxxxxxx 1xxxxxxx 0xxxxxxx: 三字节
     * - 1xxxxxxx 1xxxxxxx 1xxxxxxx xxxxxxxx: 四字节（最后一个字节使用全部 8 位）
     *
     * @return 29 位有符号整数，范围 -268435456 到 536870911
     */
    public int readInt29() throws IOException {
        int total = 0;
        int b = readByte();

        // 第一个字节
        if (b < 128) {
            return b;
        }

        // 第二个字节
        total = (b & 0x7F) << 7;
        b = readByte();

        if (b < 128) {
            total |= b;
        } else {
            // 第三个字节
            total = (total | (b & 0x7F)) << 7;
            b = readByte();

            if (b < 128) {
                total |= b;
            } else {
                // 第四个字节（使用全部 8 位）
                total = (total | (b & 0x7F)) << 8;
                total |= readByte();
            }
        }

        // 处理符号位（29 位有符号整数）
        // 如果第 29 位是 1，则是负数
        return -(total & (1 << 28)) | total;
    }

    /**
     * 读取 AMF3 头部信息
     *
     * AMF3 中很多类型使用一个头部来区分是定义还是引用：
     * - 最低位为 0: 这是一个引用，高位存储引用索引
     * - 最低位为 1: 这是一个定义，高位存储数据长度或其他信息
     */
    public AMFHeader readAMFHeader() throws IOException {
        int handle = readInt29();
        boolean isDef = (handle & 1) != 0;
        int value = handle >> 1;

        return new AMFHeader(isDef, value);
    }

    /**
     * AMF 头部信息类
     */
    public static class AMFHeader {
        public final boolean isDef;
        public final int value;

        public AMFHeader(boolean isDef, int value) {
            this.isDef = isDef;
            this.value = value;
        }
    }
}
