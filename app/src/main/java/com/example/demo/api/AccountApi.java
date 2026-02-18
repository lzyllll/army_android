package com.example.demo.api;

import com.example.demo.pojo.AccountInfo;
import com.example.demo.pojo.GameUser;
import com.example.demo.utils.VerifyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 4399 存档账号 API
 *
 * 用于获取游戏存档列表
 */
public class AccountApi {

    private final OkHttpClient client;
    private final String uid;
    private final Map<String, String> cookies;

    public AccountApi(String uid, Map<String, String> cookies) {
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        this.uid = uid;
        this.cookies = cookies;
    }

    public AccountApi(String uid, String cookieString) {
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        this.uid = uid;
        this.cookies = parseCookieString(cookieString);
    }

    /**
     * 获取存档列表
     *
     * @param token 令牌（可选，可为空字符串）
     * @return AccountInfo.List
     * @throws IOException 网络错误或解析失败
     */
    public AccountInfo.List getAccountList(String token) throws IOException {
        String gameId = ApiConstants.GAME_ID;
        String gameKey = VerifyUtil.saveKey(gameId);
        String verify = VerifyUtil.userListVerify(gameKey, uid, gameId, token);

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("uid", uid)
                .add("gameid", gameId)
                .add("verify", verify)
                .add("gamekey", gameKey)
                .add("token", token != null ? token : "");

        String url = ApiConstants.SAVE_API_URL + "?ac=get_list";

        Request request = new Request.Builder()
                .url(url)
                .headers(buildHeaders(ApiConstants.GAME_API_HEADERS))
                .header("Cookie", buildCookieString(cookies))
                .post(formBuilder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();

            try {
                // 解析为 JSON 数组
                JSONArray jsonArray = new JSONArray(body);
                return AccountInfo.List.read(jsonArray, uid);
            } catch (JSONException e) {
                throw new IOException("解析存档列表失败: " + body);
            }
        }
    }

    /**
     * 获取存档列表（使用默认 token）
     */
    public AccountInfo.List getAccountList() throws IOException {
        return getAccountList("");
    }

    /**
     * 生成指定存档索引的 GameUser
     *
     * @param archIndex 存档索引 (0-7)
     * @return GameUser 实例
     */
    public GameUser createGameUser(int archIndex) {
        return new GameUser(uid, archIndex, buildCookieString(cookies));
    }

    /**
     * 生成 AccountInfo 对应的 GameUser
     *
     * @param accountInfo 存档信息
     * @return GameUser 实例
     */
    public GameUser createGameUser(AccountInfo accountInfo) {
        return new GameUser(uid, accountInfo.index, buildCookieString(cookies));
    }

    /**
     * 构建 Cookie 字符串
     */
    private String buildCookieString(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * 解析 Cookie 字符串
     */
    private Map<String, String> parseCookieString(String cookieString) {
        Map<String, String> map = new HashMap<>();
        if (cookieString == null || cookieString.isEmpty()) {
            return map;
        }
        String[] parts = cookieString.split(";");
        for (String part : parts) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
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
