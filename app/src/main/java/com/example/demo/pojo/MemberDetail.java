package com.example.demo.pojo;

import com.example.demo.amf3.AMFSerializable;
import com.example.demo.utils.ExtraParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * member.extra 解析后的成员详情
 *
 * AMF3 静态字段顺序:
 * conDay, money, dps, bt, lt, vip, militaryRank,
 * lv, loginTime, playerName, life, mp, conObj, pkS, pkW
 */
public class MemberDetail {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public String loginTime = "";
    /** 每秒伤害 */
    public double dps = 0.0;
    /** vip等级，如 VIP8 */
    public String vip = "";
    /** 金钱 */
    public int money = 0;
    /** 玩家名字 */
    public String playerName = "";
    /** 军衔 */
    public String militaryRank = "";
    /** 贡献 */
    public ConObj conObj = new ConObj();
    /** 今日贡献 */
    public int conDay = 0;
    /** 等级 */
    public int lv = 0;
    /** 争霸时间 */
    public String bt = "";
    /** 生命值 */
    public int life = 0;
    /** 争霸地图 */
    public String mp = "";
    /** 争霸耗时 */
    public double lt = 0.0;
    /** 计算后的争霸得分 (4~48, 0表示未参与) */
    public int score = 0;
    /** 不知是什么 */
    public int pkS = 0;
    public int pkW = 0;

    /** 原始解析数据，还原时用于补全未定义的字段 */
    private Map<String, Object> rawArgs = new HashMap<>();

    /**
     * 从 member.extra 字符串解析
     */
    public static MemberDetail fromExtra(String extra) {
        Map<String, Object> args = ExtraParser.parse(extra);
        MemberDetail detail = new MemberDetail();
        detail.rawArgs = new HashMap<>(args);

        detail.loginTime = toStr(args.get("loginTime"));
        detail.dps = toDouble(args.get("dps"));
        detail.vip = toStr(args.get("vip"));
        detail.money = toInt(args.get("money"));
        detail.playerName = toStr(args.get("playerName"));
        detail.militaryRank = toStr(args.get("militaryRank"));
        detail.conObj = ConObj.fromRawObj(args.get("conObj"));
        detail.lv = toInt(args.get("lv"));
        detail.bt = toStr(args.get("bt"));
        detail.life = toInt(args.get("life"));
        detail.mp = toStr(args.get("mp"));
        detail.lt = toDouble(args.get("lt"));
        detail.pkS = toInt(args.get("pkS"));
        detail.pkW = toInt(args.get("pkW"));

        // === 业务逻辑计算 ===
        Date now = new Date();

        // 本日贡献 (每天 0 点刷新)
        Date loginDate = parseDate(detail.loginTime);
        if (loginDate != null && isSameDay(loginDate, now)) {
            detail.conDay = toInt(args.get("conDay"));
        } else {
            detail.conDay = 0;
        }

        // 争霸分数/地图 (每周六 0 点刷新)
        if (detail.bt != null && !detail.bt.isEmpty()) {
            Date btTime = parseDate(detail.bt);
            if (btTime != null) {
                Date lastSaturdayMidnight = getLastSaturdayMidnight(now);
                if (btTime.before(lastSaturdayMidnight)) {
                    detail.lt = 0.0;
                    detail.mp = "";
                }
            } else {
                detail.lt = 0.0;
                detail.mp = "";
            }
        } else {
            detail.lt = 0.0;
            detail.mp = "";
        }

        // 计算 score: 最低4, 最高48, 没记录为0
        if (detail.mp == null || detail.mp.isEmpty()) {
            detail.score = 0;
        } else if (detail.lt == 0) {
            detail.score = 4;
        } else {
            detail.score = Math.min(48, Math.max(4, (int) Math.round((100 - detail.lt) / 2)));
        }

        return detail;
    }

    /**
     * 转为 AMFSerializable 用于编码上传
     * 先以 rawArgs 为底，再覆盖已知字段，保证未定义的字段不丢失
     */
    public AMFSerializable toAMFSerializable() {
        AMFSerializable obj = new AMFSerializable("", false);
        obj.__dynamic = false;
        // 先放入原始数据作为底
        for (Map.Entry<String, Object> entry : rawArgs.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("__")) {
                obj.put(key, entry.getValue());
            }
        }
        // 再覆盖已知字段
        obj.put("conDay", conDay);
        obj.put("money", money);
        obj.put("dps", dps);
        obj.put("bt", bt);
        obj.put("lt", lt);
        obj.put("vip", vip);
        obj.put("militaryRank", militaryRank);
        obj.put("lv", lv);
        obj.put("loginTime", loginTime);
        obj.put("playerName", playerName);
        obj.put("life", life);
        obj.put("mp", mp);
        // conObj 转回原始 Map 格式
        AMFSerializable conObjMap = new AMFSerializable("", true);
        for (Map.Entry<String, Object> entry : conObj.toRawObj().entrySet()) {
            conObjMap.put(entry.getKey(), entry.getValue());
        }
        obj.put("conObj", conObjMap);
        obj.put("pkS", pkS);
        obj.put("pkW", pkW);
        return obj;
    }


    public String toExtra() {
        try {
            return ExtraParser.encode(toAMFSerializable());
        } catch (Exception e) {
            return "";
        }
    }

    // === 工具方法 ===

    private static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty())
            return null;
        try {
            return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private static boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 获取距离 now 最近的上一个周六 00:00:00
     */
    private static Date getLastSaturdayMidnight(Date now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        // Calendar.SATURDAY = 7, weekday: 1=Sun,...,7=Sat
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        // 距离上个周六过了多少天
        int daysSinceSaturday = (dayOfWeek % 7); // Sun=0, Mon=1, ..., Sat=6 → 不对
        // Calendar: Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7
        // 我们要算距上个周六过了多少天:
        // Sat=7 → 0天, Sun=1 → 1天, Mon=2 → 2天, ..., Fri=6 → 6天
        daysSinceSaturday = (dayOfWeek == Calendar.SATURDAY) ? 0 : dayOfWeek; // Sun=1,Mon=2,...Fri=6
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceSaturday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static int toInt(Object value) {
        if (value == null)
            return 0;
        if (value instanceof Number)
            return ((Number) value).intValue();
        return 0;
    }

    private static double toDouble(Object value) {
        if (value == null)
            return 0.0;
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return Double.isNaN(d) ? Double.MAX_VALUE : d;
        }
        return 0.0;
    }

    private static String toStr(Object value) {
        if (value == null)
            return "";
        return value.toString();
    }

    private static final Map<String, String> MAP_NAMES = new HashMap<>();
    static {
        MAP_NAMES.put("XiFeng", "西峰");
        MAP_NAMES.put("FangZhouTop", "方舟顶层");
        MAP_NAMES.put("XianQu", "先驱号");
        MAP_NAMES.put("HanGuang5", "寒光末路");
        MAP_NAMES.put("Hospital2", "实验室中层");
        MAP_NAMES.put("ShaMo", "沙漠中心");
        MAP_NAMES.put("FuMu", "腐木岗");
        MAP_NAMES.put("JungleDeep", "丛林深处");
        MAP_NAMES.put("LvSen", "绿森堡");
        MAP_NAMES.put("Hospital3", "研究中心");
        MAP_NAMES.put("DongShan", "东山澳");
        MAP_NAMES.put("BuDong", "不冻湖");
        MAP_NAMES.put("HanGuang2", "寒光西城");
        MAP_NAMES.put("CavesDeep", "洞窟深处");
        MAP_NAMES.put("Hospital1", "实验室入口");
        MAP_NAMES.put("DiXia", "地下城");
        MAP_NAMES.put("DuKu", "毒窟");
        MAP_NAMES.put("BaiSha", "白沙村");
        MAP_NAMES.put("NanTang", "南唐城");
        MAP_NAMES.put("PrisonDeep", "监狱深处");
        MAP_NAMES.put("BeiDou", "北斗城");
        MAP_NAMES.put("GuiWang", "鬼王墓");
        MAP_NAMES.put("ZhongXin", "实验室底层");
        MAP_NAMES.put("PingChuan", "平川");
    }

    public static String getMapName(String mp) {
        if (mp == null || mp.isEmpty())
            return "";
        String name = MAP_NAMES.get(mp);
        return name != null ? name : mp;
    }

    @Override
    public String toString() {
        return "MemberDetail{" +
                "playerName='" + playerName + '\'' +
                ", lv=" + lv +
                ", vip='" + vip + '\'' +
                ", dps=" + dps +
                ", life=" + life +
                ", money=" + money +
                ", militaryRank='" + militaryRank + '\'' +
                ", conObj=" + conObj +
                ", conDay=" + conDay +
                ", score=" + score +
                ", loginTime='" + loginTime + '\'' +
                ", bt='" + bt + '\'' +
                ", mp='" + mp + '\'' +
                ", lt=" + lt +
                ", pkS=" + pkS +
                ", pkW=" + pkW +
                '}';
    }
}
