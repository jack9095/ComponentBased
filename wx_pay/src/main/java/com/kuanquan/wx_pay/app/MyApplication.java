package com.kuanquan.wx_pay.app;

import android.app.Application;
import android.content.Context;

import com.kuanquan.wx_pay.util.LogUtil;

public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.Builder fLog = new LogUtil.Builder(this)
                .isLog(true) //是否开启打印
                .isLogBorder(true) //是否开启边框
                .setLogType(LogUtil.TYPE.E) //设置默认打印级别
                .setTag("dx"); //设置默认打印Tag
        LogUtil.init(fLog);
    }
}
