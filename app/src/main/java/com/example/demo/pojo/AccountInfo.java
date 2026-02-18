package com.example.demo.pojo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 存档信息
 */
public class AccountInfo {
    /** 存档索引 (0-7) */
    public final int index;
    /** 是否为作弊存档 (0=正常, 其他=作弊) */
    public final int status;
    /** 最后登录时间 */
    public final String datetime;
    /** 存档标题 (玩家名 + 等级) */
    public final String title;

    public AccountInfo(int index, int status, String datetime, String title) {
        this.index = index;
        this.status = status;
        this.datetime = datetime;
        this.title = title;
    }

    /**
     * 是否为作弊存档
     */
    public boolean isCheated() {
        return status != 0;
    }

    /**
     * 从 JSON 对象解析
     */
    public static AccountInfo fromJSONObject(JSONObject obj) throws JSONException {
        int index = obj.optInt("index", -1);
        int status = obj.optInt("status", 0);
        String datetime = obj.optString("datetime", "");
        String title = obj.optString("title", "");

        return new AccountInfo(index, status, datetime, title);
    }

    @Override
    public String toString() {
        return "AccountInfo{" +
                "index=" + index +
                ", status=" + status +
                ", datetime='" + datetime + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    /**
     * 存档信息列表
     */
    public static class List {
        public final String uid;
        public final java.util.List<AccountInfo> accounts;
        public final int count;

        public List(String uid, java.util.List<AccountInfo> accounts) {
            this.uid = uid;
            this.accounts = accounts;
            this.count = accounts.size();
        }

        /**
         * 从 JSON 数组解析
         */
        public static List read(JSONArray jsonArray, String uid) throws JSONException {
            java.util.List<AccountInfo> accounts = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                accounts.add(AccountInfo.fromJSONObject(obj));
            }
            return new List(uid, accounts);
        }
    }
}
