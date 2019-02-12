package com.example.fly.componentbased.okhttp;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
//import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Observes, modifies, and potentially short-circuits requests going out and the corresponding
 * responses coming back in. Typically interceptors add, remove, or transform headers on the request
 * or response.
 */
public interface Interceptor {
    Response intercept(Chain chain) throws IOException;

    // 也就是说我们可以通过实现Interceptor，定义一个拦截器对象，然后拿到请求和Response对象，对Request和Response进行修改
    interface Chain {
        // 实现chain接口对象的request方法可以拿到Request对象
        okhttp3.Request request();

        // 实现chain接口对象的proceed方法可以拿到Response对象
        Response proceed(Request request) throws IOException;

        /**
         * Returns the connection the request will be executed on. This is only available in the chains
         * of network interceptors; for application interceptors this is always null.
         *
         * 返回将在其上执行请求的连接。这只适用于链条对于网络拦截器；对于应用程序拦截器，此值始终为空
         */
        @Nullable
        Connection connection();

        Call call();

        int connectTimeoutMillis();

        Chain withConnectTimeout(int timeout, TimeUnit unit);

        int readTimeoutMillis();

        Chain withReadTimeout(int timeout, TimeUnit unit);

        int writeTimeoutMillis();

        Chain withWriteTimeout(int timeout, TimeUnit unit);
    }
}
