package com.example.demo.api;

import com.example.demo.thrift.HttpTransport;
import com.example.demo.thrift.MemberApi;
import com.example.demo.thrift.Normal;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

/**
 * Member API 客户端
 * 对应 path: union/MemberApi
 */
public class MemberApiClient extends BaseThriftClient implements AutoCloseable {

    private HttpTransport transport;
    private MemberApi.Client client;

    public MemberApiClient() {
        super(ApiConstants.HOST, ApiConstants.PORT, ApiConstants.PATH_MEMBER_API);
        init();
    }

    public MemberApiClient(String cookies) {
        super(ApiConstants.HOST, ApiConstants.PORT, ApiConstants.PATH_MEMBER_API, cookies);
        init();
    }

    private void init() {
        transport = createTransport();
        TProtocol protocol = createProtocol(transport);
        client = new MemberApi.Client(protocol);
    }

    public MemberApi.Client getClient() {
        return client;
    }

    /**
     * 构建��求参数
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
