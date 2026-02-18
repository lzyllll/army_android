package com.example.demo.api;

import com.example.demo.thrift.HttpTransport;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

/**
 * Thrift API 客户端基类
 */
public abstract class BaseThriftClient {

    protected final String host;
    protected final int port;
    protected final String path;
    protected String cookies;

    public BaseThriftClient(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = path;
    }

    public BaseThriftClient(String host, int port, String path, String cookies) {
        this.host = host;
        this.port = port;
        this.path = path;
        this.cookies = cookies;
    }

    /**
     * 设置 Cookie
     */
    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    /**
     * 获取 Cookie
     */
    public String getCookies() {
        return cookies;
    }

    /**
     * 创建传输层
     */
    protected HttpTransport createTransport() {
        if (cookies != null && !cookies.isEmpty()) {
            return new HttpTransport(host, port, path, cookies);
        }
        return new HttpTransport(host, port, path);
    }

    /**
     * 创建协议
     */
    protected TProtocol createProtocol(HttpTransport transport) {
        return new TBinaryProtocol(transport);
    }
}
