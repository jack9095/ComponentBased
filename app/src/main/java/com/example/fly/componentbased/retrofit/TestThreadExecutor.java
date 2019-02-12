package com.example.fly.componentbased.retrofit;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class TestThreadExecutor implements Executor {
    private final Handler handler = new Handler(Looper.getMainLooper());  // 创建主线程的handler


    @Override
    public void execute(Runnable r) {
//        Looper.prepare();
//        Handler handler = new Handler();
//        Looper.loop();
        handler.post(r);
    }

}
