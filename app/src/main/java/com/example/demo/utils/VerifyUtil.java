package com.example.demo.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * 验证工具类
 * 用于生成各种 API 请求的 verify 参数
 */
public class VerifyUtil {

    private static final String SALT_PREFIX = "SDALPlsldlnSLWPElsdslSE";
    private static final String SALT_SUFFIX = "PKslsO";
    private static final String GAME_KEY_SALT = "LPislKLodlLKKOSNlSDOAADLKADJAOADALAklsd";
    private static final String DEFAULT_GAME_ID = "100027788";

    /**
     * 获取游戏密钥
     */
    public static String saveKey(String gameId) {
        if (DEFAULT_GAME_ID.equals(gameId)) {
            return "34008a2844a1a569";
        }

        String inputString = gameId + GAME_KEY_SALT + gameId;
        String firstHash = md5(inputString);
        String secondHash = md5(firstHash);
        return secondHash.substring(4, 20);
    }

    /**
     * 获取默认游戏密钥
     */
    public static String saveKey() {
        return saveKey(DEFAULT_GAME_ID);
    }

    /**
     * 生成用户列表验证参数
     */
    public static String userListVerify(String gameKey, String uid, String gameId, String token) {
        String rawStr = SALT_PREFIX + gameKey + uid + gameId + token + SALT_SUFFIX;
        return tripleMd5(rawStr);
    }

    /**
     * 生成用户索引验证参数
     */
    public static String userIndexVerify(String gameKey, String uid, String gameId, String token, String index) {
        String rawStr = SALT_PREFIX + index + gameKey + uid + gameId + token + SALT_SUFFIX;
        return tripleMd5(rawStr);
    }

    /**
     * 生成会话检查请求体
     */
    public static Map<String, String> generateSessionCheckBody(String gameId, String uid, String sessionStr) {
        Map<String, String> params = new HashMap<>();
        params.put("gameid", gameId);
        params.put("uid", uid);
        params.put("session", sessionStr);

        String rawStr = SALT_PREFIX + sessionStr + uid + gameId + SALT_SUFFIX;
        params.put("verify", tripleMd5(rawStr));

        return params;
    }

    /**
     * 三重 MD5
     */
    private static String tripleMd5(String input) {
        String hash1 = md5(input);
        String hash2 = md5(hash1);
        return md5(hash2);
    }

    /**
     * MD5 哈希
     */
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
