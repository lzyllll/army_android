package com.example.demo.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.demo.pojo.LoginResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 4399 登录 API
 *
 * 使用流程:
 * 1. 调用 checkLogin() 检查是否需要验证码
 * 2. 如果需要验证码，获取验证码图片显示给用户
 * 3. 用户输入验证码后，调用 login() 完成登录
 */
public class LoginApi {

    private final OkHttpClient client;
    private String currentSessionId;
    private String currentCaptchaUrl;
    private Map<String, String> currentCookies;

    public LoginApi() {
        this(null);
    }

    /**
     * 带代理的构造函数
     *
     * @param proxyHost 代理地址，如 "127.0.0.1"，传 null 则不使用代理
     * @param proxyPort 代理端口，如 8888
     */
    public LoginApi(String proxyHost, int proxyPort) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true);

        if (proxyHost != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            builder.proxy(proxy);
        }

        this.client = builder.build();
        this.currentCookies = new HashMap<>();
    }

    /**
     * 使用 Proxy 对象的构造函数
     */
    public LoginApi(Proxy proxy) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true);

        if (proxy != null) {
            builder.proxy(proxy);
        }

        this.client = builder.build();
        this.currentCookies = new HashMap<>();
    }

    /**
     * 检查是否需要验证码
     *
     * @param username 用户名
     * @return LoginResult，如果需要验证码则包含验证码图片
     */
    public LoginResult checkLogin(String username) throws IOException {


        // 设置初始 cookies
        currentCookies.clear();
        currentCookies.put("USESSIONID", generateUUID());


        // 检查验证码
        String checkUrl = ApiConstants.LOGIN_VERIFY_URL +
                "?username=" + username +
                "&appId=kid_wdsj" +
                "&t=" + UUID.randomUUID() +
                "&inputWidth=iptw2&v=1";

        Request request = new Request.Builder()
                .url(checkUrl)
                .headers(buildHeaders(ApiConstants.LOGIN_HEADERS))
                .header("Cookie", buildCookieString(currentCookies))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();

            // 检查是否需要验证码
            Pattern pattern = Pattern.compile("/ptlogin/captcha\\.do\\?captchaId=[\\w\\d]+");
            Matcher matcher = pattern.matcher(body);

            if (matcher.find()) {
                currentSessionId = matcher.group(0).split("=")[1];
                currentCaptchaUrl = ApiConstants.LOGIN_BASE_URL + matcher.group(0);

                // 获取验证码图片
                String captchaBase64 = getCaptchaImage(currentCaptchaUrl);

                return LoginResult.needCaptcha(currentSessionId, currentCaptchaUrl, captchaBase64,
                        buildCookieString(currentCookies));
            }

            // 不需要验证码
            return LoginResult.success(null, null);
        }
    }

    /**
     * 获取验证码图片 (Base64)
     */
    private String getCaptchaImage(String captchaUrl) throws IOException {
        Request request = new Request.Builder()
                .url(captchaUrl)
                .headers(buildHeaders(ApiConstants.LOGIN_HEADERS))
                .header("Cookie", buildCookieString(currentCookies))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            byte[] bytes = response.body().bytes();
            return java.util.Base64.getEncoder().encodeToString(bytes);
        }
    }

    /**
     * 获取验证码图片 (Bitmap)
     */
    public Bitmap getCaptchaBitmap() throws IOException {
        if (currentCaptchaUrl == null) {
            return null;
        }

        Request request = new Request.Builder()
                .url(currentCaptchaUrl)
                .headers(buildHeaders(ApiConstants.LOGIN_HEADERS))
                .header("Cookie", buildCookieString(currentCookies))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            byte[] bytes = response.body().bytes();
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

    /**
     * 执行登录
     *
     * @param username 用户名
     * @param password 密码
     * @param captcha  验证码 (如果不需要验证码，传空字符串)
     * @return LoginResult
     */
    public LoginResult login(String username, String password, String captcha, String sessionId) throws IOException {
        // 初始化 cookies
        currentCookies.clear();
        currentCookies.put("USESSIONID", generateUUID());
        currentCookies.put("ptusertype", "kid_wdsj.4399_login");

        // 构建登录请求
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("postLoginHandler", "default")
                .add("externalLogin", "qq")
                .add("bizId", "2100001792")
                .add("appId", "kid_wdsj")
                .add("gameId", "wd")
                .add("sec", "1")
                .add("password", password)
                .add("username", username);

        // 如果有验证码
        if (sessionId != null && captcha != null && !sessionId.isEmpty() && !captcha.isEmpty()) {
            formBuilder.add("redirectUrl", "")
                    .add("sessionId", sessionId)
                    .add("inputCaptcha", captcha);
        }

        RequestBody formBody = formBuilder.build();

        Request request = new Request.Builder()
                .url(ApiConstants.LOGIN_DO_URL)
                .headers(buildHeaders(ApiConstants.LOGIN_HEADERS))
                .header("Cookie", buildCookieString(currentCookies))
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 更新 cookies
            updateCookies(response);

            // 检查登录是否成功
            String uauth = currentCookies.get("Uauth");
            String puser = currentCookies.get("Puser");

            if (uauth == null || puser == null) {
                return LoginResult.error("登录失败，请检查账号密码或验证码");
            }

            // 获取 UID
            String pauth = currentCookies.get("Pauth");
            String uid = null;
            if (pauth != null) {
                uid = pauth.split("\\|")[0];
            }

            // 验证登录
            if (!verifyLogin()) {
                return LoginResult.error("登录验证失败");
            }

            return LoginResult.success(buildCookieString(currentCookies), uid);
        }
    }

    /**
     * 验证登录状态
     */
    private boolean verifyLogin() throws IOException {
        // checkKid
        String uauth = currentCookies.get("Uauth");
        if (uauth == null)
            return false;

        String[] parts = uauth.split("\\|");
        String authTimestamp = parts.length > 4 ? parts[4] : String.valueOf(System.currentTimeMillis());

        String checkUrl = ApiConstants.LOGIN_CHECK_KID_URL +
                "?appId=kid_wdsj" +
                "&gameUrl=http://cdn.h5wan.4399sj.com/microterminal-h5-frame?game_id=500352" +
                "&rand_time=" + authTimestamp +
                "&nick=null" +
                "&onLineStart=false" +
                "&show=1" +
                "&isCrossDomain=1" +
                "&retUrl=http%253A%252F%252Fptlogin.4399.com%252Fresource%252Fucenter.html";

        Request request = new Request.Builder()
                .url(checkUrl)
                .headers(buildHeaders(ApiConstants.LOGIN_HEADERS))
                .header("Cookie", buildCookieString(currentCookies))
                .post(new FormBody.Builder().build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    /**
     * 从响应中更新 cookies
     */
    private void updateCookies(Response response) {
        for (String cookieStr : response.headers("Set-Cookie")) {
            String[] parts = cookieStr.split(";");
            if (parts.length > 0) {
                String[] kv = parts[0].split("=", 2);
                if (kv.length == 2) {
                    currentCookies.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
    }

    /**
     * 构建 Cookie 字符串
     */
    private String buildCookieString(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0)
                sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
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

    /**
     * 生成 UUID
     */
    private String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * 获取当前 cookies
     */
    public Map<String, String> getCurrentCookies() {
        return currentCookies;
    }

    /**
     * 设置 cookies (用于持久化登录)
     */
    public void setCookies(Map<String, String> cookies) {
        this.currentCookies = cookies;
    }

    /**
     * 设置 cookies (从字符串)
     */
    public void setCookies(String cookieString) {
        this.currentCookies.clear();
        String[] parts = cookieString.split(";");
        for (String part : parts) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                currentCookies.put(kv[0].trim(), kv[1].trim());
            }
        }
    }
}
