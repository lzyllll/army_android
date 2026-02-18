package com.example.demo.api;

import com.example.demo.utils.VerifyUtil;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 游戏会话 API
 */
public class GameSessionApi {

    private final OkHttpClient client;
    private final String cookies;

    public GameSessionApi(String cookies) {
        this.cookies = cookies;
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)
                .build();
    }

    /**
     * 获取游戏会话字符串
     */
    public String getGameSession(String uid) throws IOException {
        String url = ApiConstants.SAVE_API_URL;
        String gameKey = VerifyUtil.saveKey(ApiConstants.GAME_ID);
        String verify = VerifyUtil.userListVerify(gameKey, uid, ApiConstants.GAME_ID, "");

        RequestBody formBody = new FormBody.Builder()
                .add("uid", uid)
                .add("gameid", ApiConstants.GAME_ID)
                .add("refer", "https://sbai.4399.com/4399swf/upload_swf/ftp15/linxy/20150324/gun/tv3451xf.htm")
                .add("verify", verify)
                .add("gamekey", gameKey)
                .build();

        String queryParams = "ac=get_session&ran=" + (new Random().nextDouble() * 10000);

        Request request = new Request.Builder()
                .url(url + "?" + queryParams)
                .headers(buildHeaders(ApiConstants.GAME_API_HEADERS))
                .header("Cookie", cookies)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String result = response.body().string();
            if (result.startsWith("Error")) {
                throw new IOException("获取 game_session 失败: " + result);
            }
            return result;
        }
    }

    /**
     * 检查游戏会话是否有效
     */
    public boolean checkGameSession(String uid, String gameSession) throws IOException {
        String url = ApiConstants.SAVE_API_URL;

        Map<String, String> params = VerifyUtil.generateSessionCheckBody(ApiConstants.GAME_ID, uid, gameSession);

        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }

        Request request = new Request.Builder()
                .url(url + "?ac=check_session")
                .headers(buildHeaders(ApiConstants.GAME_API_HEADERS))
                .header("Cookie", cookies)
                .post(formBuilder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            String result = response.body().string();
            return "1".equals(result.trim());
        }
    }

    /**
     * 检查或更新游戏会话
     */
    public String checkOrUpdate(String uid, String gameSession) throws IOException {
        if (gameSession != null && checkGameSession(uid, gameSession)) {
            return gameSession;
        }
        return getGameSession(uid);
    }

    /**
     * 构建 Headers
     */
    private okhttp3.Headers buildHeaders(String[][] headers) {
        okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder();
        for (String[] header : headers) {
            builder.add(header[0], header[1]);
        }
        return builder.build();
    }
}
