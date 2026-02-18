package com.example.demo.pojo;

/**
 * 登录结果
 */
public class LoginResult {

    /** 是否需要验证码 */
    public boolean needCaptcha;

    /** 验证码Session ID */
    public String sessionId;

    /** 验证码图片URL */
    public String captchaUrl;

    /** 验证码图片数据 (Base64) */
    public String captchaBase64;

    /** 登录成功后的 Cookies */
    public String cookies;

    /** 用户 UID */
    public String uid;

    /** 错误信息 */
    public String errorMessage;

    /** 是否成功 */
    public boolean success;

    public static LoginResult needCaptcha(String sessionId, String captchaUrl, String captchaBase64, String cookies) {
        LoginResult result = new LoginResult();
        result.needCaptcha = true;
        result.sessionId = sessionId;
        result.captchaUrl = captchaUrl;
        result.captchaBase64 = captchaBase64;
        result.cookies = cookies;
        result.success = false;
        return result;
    }

    public static LoginResult success(String cookies, String uid) {
        LoginResult result = new LoginResult();
        result.needCaptcha = false;
        result.cookies = cookies;
        result.uid = uid;
        result.success = true;
        return result;
    }

    public static LoginResult error(String message) {
        LoginResult result = new LoginResult();
        result.success = false;
        result.errorMessage = message;
        return result;
    }
}
