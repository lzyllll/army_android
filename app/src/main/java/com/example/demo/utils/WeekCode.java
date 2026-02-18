package com.example.demo.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 周码计算工具
 * 基准日期: 2018-11-19，计算距今的完整周数
 */
public class WeekCode {

    private static final LocalDate ORIGINAL_DATE = LocalDate.of(2018, 11, 19);

    /**
     * 计算当前周码
     */
    public static int calculate() {
        return calculate(LocalDate.now());
    }

    /**
     * 计算指定日期的周码
     */
    public static int calculate(LocalDate date) {
        long days = ChronoUnit.DAYS.between(ORIGINAL_DATE, date);
        return (int) (days / 7);
    }

    /**
     * 获取本周、上周、上上周的周码字符串
     *
     * @return [thisWeek, lastWeek, beforeLastWeek]
     */
    public static String[] getWeekCodes() {
        return getWeekCodes(LocalDate.now());
    }

    /**
     * 获取指定日期对应的本周、上周、上上周的周码字符串
     *
     * @return [thisWeek, lastWeek, beforeLastWeek]
     */
    public static String[] getWeekCodes(LocalDate date) {
        int code = calculate(date);
        return new String[]{
                String.valueOf(code),
                String.valueOf(code - 1),
                String.valueOf(code - 2)
        };
    }
}
