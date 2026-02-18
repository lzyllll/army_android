package com.example.demo.api;

import com.example.demo.thrift.HttpTransport;
import com.example.demo.thrift.Normal;
import com.example.demo.thrift.VisitorApi;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

/**
 * Visitor API 客户端
 * 对应 path: union/VisitorApi
 */
public class VisitorApiClient extends BaseThriftClient implements AutoCloseable {

    private HttpTransport transport;
    private VisitorApi.Client client;

    public VisitorApiClient() {
        super(ApiConstants.HOST, ApiConstants.PORT, ApiConstants.PATH_VISITOR_API);
        init();
    }

    public VisitorApiClient(String cookies) {
        super(ApiConstants.HOST, ApiConstants.PORT, ApiConstants.PATH_VISITOR_API, cookies);
        init();
    }

    private void init() {
        transport = createTransport();
        TProtocol protocol = createProtocol(transport);
        client = new VisitorApi.Client(protocol);
    }

    public VisitorApi.Client getClient() {
        return client;
    }

    /**
     * 构建请求参数
     */
    public Normal buildNormal(int archIndex) {
        Normal normal = new Normal();
        normal.setTime(String.valueOf(System.currentTimeMillis() / 1000));
        normal.setGame_id(ApiConstants.GAME_ID);
        normal.setArch_index(String.valueOf(archIndex));
        return normal;
    }

    @Override
    public void close() {
        if (transport != null) {
            transport.close();
        }
    }
}
