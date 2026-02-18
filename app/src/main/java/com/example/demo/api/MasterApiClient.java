package com.example.demo.api;

import com.example.demo.thrift.HttpTransport;
import com.example.demo.thrift.MasterApi;
import com.example.demo.thrift.Normal;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

/**
 * Master API 客户端
 * 对应 path: union/MasterApi
 */
public class MasterApiClient extends BaseThriftClient implements AutoCloseable {

    private HttpTransport transport;
    private MasterApi.Client client;

    public MasterApiClient() {
        super(ApiConstants.HOST, ApiConstants.PORT, ApiConstants.PATH_MASTER_API);
        init();
    }

    public MasterApiClient(String cookies) {
        super(ApiConstants.HOST, ApiConstants.PORT, ApiConstants.PATH_MASTER_API, cookies);
        init();
    }

    private void init() {
        transport = createTransport();
        TProtocol protocol = createProtocol(transport);
        client = new MasterApi.Client(protocol);
    }

    public MasterApi.Client getClient() {
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
