package com.example.demo.api;

import com.example.demo.thrift.HttpTransport;
import com.example.demo.thrift.GrowApi;
import com.example.demo.thrift.Normal;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

/**
 * Grow API 客户端
 * 对应 path: union/GrowApi
 */
public class GrowApiClient extends BaseThriftClient implements AutoCloseable {

    private HttpTransport transport;
    private GrowApi.Client client;

    public GrowApiClient() {
        super(ApiConstants.HOST, ApiConstants.PORT, ApiConstants.PATH_GROW_API);
        init();
    }

    public GrowApiClient(String cookies) {
        super(ApiConstants.HOST, ApiConstants.PORT, ApiConstants.PATH_GROW_API, cookies);
        init();
    }

    private void init() {
        transport = createTransport();
        TProtocol protocol = createProtocol(transport);
        client = new GrowApi.Client(protocol);
    }

    public GrowApi.Client getClient() {
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
