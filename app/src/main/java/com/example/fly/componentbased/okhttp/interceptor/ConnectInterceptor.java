package com.example.fly.componentbased.okhttp.interceptor;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.http.RealInterceptorChain;

/** Opens a connection to the target server and proceeds to the next interceptor.
 *  OkHttp当中的真正的网络请求都是通过网络连接器来实现的
 * */
public final class ConnectInterceptor implements Interceptor {
    public final OkHttpClient client;

    public ConnectInterceptor(OkHttpClient client) {
        this.client = client;
    }

    @Override public Response intercept(Chain chain) throws IOException {
        RealInterceptorChain realChain = (RealInterceptorChain) chain;
        Request request = realChain.request();
        // 建立Http网络请求所有需要的网络组件 ,在RetryAndFollowUpInterceptor创建了StreamAllocation，在这里使用
        com.kuanquan.testdemo.okhttp.connect.StreamAllocation streamAllocation = realChain.streamAllocation();

        // We need the network to satisfy this request. Possibly for validating a conditional GET.
        boolean doExtensiveHealthChecks = !request.method().equals("GET");
        // HttpCodec用来编码Request,解码Response
        HttpCodec httpCodec = streamAllocation.newStream(client, chain, doExtensiveHealthChecks);
        // RealConnection用来进行实际的网络io传输的
        RealConnection connection = streamAllocation.connection(); // 很关键的

        return realChain.proceed(request, streamAllocation, httpCodec, connection);
    }
}
