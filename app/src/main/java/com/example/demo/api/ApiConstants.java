package com.example.demo.api;

/**
 * API 常量定义
 */
public class ApiConstants {

    /** 主机地址 */
    public static final String HOST = "save.api.4399.com";
    public static final int PORT = 80;

    /** API 路径 */
    public static final String PATH_VISITOR_API = "union/VisitorApi";
    public static final String PATH_MEMBER_API = "union/MemberApi";
    public static final String PATH_MASTER_API = "union/MasterApi";
    public static final String PATH_GROW_API = "union/GrowApi";

    /** 登录相关 URL */
    public static final String LOGIN_BASE_URL = "http://ptlogin.4399.com";
    public static final String LOGIN_VERIFY_URL = LOGIN_BASE_URL + "/ptlogin/verify.do";
    public static final String LOGIN_DO_URL = LOGIN_BASE_URL + "/ptlogin/login.do";
    public static final String LOGIN_CHECK_KID_URL = LOGIN_BASE_URL + "/ptlogin/checkKidLoginUserCookie.do";
    public static final String USER_INFO_URL = "https://microgame.5054399.net/v2/service/sdk/info";

    /** 存档相关 URL */
    public static final String SAVE_API_URL = "https://save.api.4399.com/";
    public static final String GET_UID_URL = "http://cz.4399.com/get_role_info.php";

    /** 游戏ID */
    public static final String GAME_ID = "100027788";

    /** 类型常量 */
    public static final int SELF_TYPE = 1;
    public static final int NO_TYPE = 0;

    /** 通用 Headers */
    public static final String[][] COMMON_HEADERS = {
            {"User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36"},
            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"}
    };

    /** 登录 Headers */
    public static final String[][] LOGIN_HEADERS = {
            {"User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36"},
            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
            {"Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"},
            {"Cache-Control", "max-age=0"},
            {"Origin", "http://ptlogin.4399.com"},
            {"Referer", "http://ptlogin.4399.com/ptlogin/loginFrame.do"}
    };

    /** 游戏 API Headers */
    public static final String[][] GAME_API_HEADERS = {
            {"User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36"},
            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
            {"Accept", "application/json, text/plain, */*"},
            {"Origin", "https://sda.4399.com"}
    };
}
