package com.example.fly.componentbased.okhttp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

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

    interface Chain {
        okhttp3.Request request();

        Response proceed(Request request) throws IOException;

        /**
         * Returns the connection the request will be executed on. This is only available in the chains
         * of network interceptors; for application interceptors this is always null.
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
