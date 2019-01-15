package com.base.commonlib.utils;

/**
 * 防止多次点击
 */
public class SingleClick {

    private static long lastTime;

    public static boolean isSingle(long defalutTime) {
        long currentTime = System.currentTimeMillis();
        boolean isSign = currentTime - lastTime <= defalutTime ? false : true;
        lastTime = currentTime;

        return isSign;
    }

    public static long getLastTime() {
        return lastTime;
    }

    public static void setLastTime(long lastTime) {
        SingleClick.lastTime = lastTime;
    }

}
