package com.example.fly.componentbased.app;


import android.content.Context;
import android.support.multidex.MultiDex;

import com.alibaba.android.arouter.launcher.ARouter;
import com.base.commonlib.base.mvp_no_dagger.BaseApplication;
import com.example.fly.componentbased.api.HttpHelper;
import com.example.fly.componentbased.api.URL;


public class App extends BaseApplication {

    private boolean debug = true;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new HttpHelper.Builder()
                .initOkHttp()
                .createRetrofit(URL.BASE_URL)
                .build();
        initARouter();
    }

    @Override
    protected String getBaseUrl() {
        return null;
    }

    private void initARouter() {
        if (isDebug()) {           // 这两行必须写在init之前，否则这些配置在init过程中将无效
            ARouter.openLog();     // 打印日志
            ARouter.openDebug();   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        }
        ARouter.init(this); // 尽可能早，推荐在Application中初始化
    }

    public boolean isDebug() {
        return debug;
    }
}
