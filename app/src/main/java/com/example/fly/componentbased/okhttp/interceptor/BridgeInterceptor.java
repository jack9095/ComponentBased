package com.example.fly.componentbased.okhttp.interceptor;

import java.io.IOException;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Version;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.Okio;

import static okhttp3.internal.Util.hostHeader;

/**
 * Bridges from application code to network code. First it builds a network request from a user
 * request. Then it proceeds to call the network. Finally it builds a user response from the network
 * response.
 * 桥接拦截器
 * 主要用于设置 内容长度、编码方式、压缩等等  主要添加头部的作用 成为一个可以发送网络请求的request
 */
public final class BridgeInterceptor implements Interceptor {
    private final CookieJar cookieJar;

    public BridgeInterceptor(CookieJar cookieJar) {
        this.cookieJar = cookieJar;
    }

    @Override public Response intercept(Chain chain) throws IOException {
        Request userRequest = chain.request();
        Request.Builder requestBuilder = userRequest.newBuilder();

        RequestBody body = userRequest.body();
        if (body != null) {
            // 进行Header的包装
            MediaType contentType = body.contentType();
            if (contentType != null) {
                requestBuilder.header("Content-Type", contentType.toString());
            }

            long contentLength = body.contentLength();
            if (contentLength != -1) {
                requestBuilder.header("Content-Length", Long.toString(contentLength));
                requestBuilder.removeHeader("Transfer-Encoding");
            } else {
                requestBuilder.header("Transfer-Encoding", "chunked");
                requestBuilder.removeHeader("Content-Length");
            }
        }

        if (userRequest.header("Host") == null) {
            requestBuilder.header("Host", hostHeader(userRequest.url(), false));
        }

        if (userRequest.header("Connection") == null) {
            requestBuilder.header("Connection", "Keep-Alive");
        }

        // If we add an "Accept-Encoding: gzip" header field we're responsible for also decompressing
        // the transfer stream.
        // 如果我们添加一个“Accept-Encoding：gzip”头字段，我们也负责解压缩传输流。
        boolean transparentGzip = false;
        if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
            transparentGzip = true;
            requestBuilder.header("Accept-Encoding", "gzip");
        }

        // 创建OkhttpClient配置的cookieJar
        List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
        if (!cookies.isEmpty()) {
            requestBuilder.header("Cookie", cookieHeader(cookies));
        }

        if (userRequest.header("User-Agent") == null) {
            requestBuilder.header("User-Agent", Version.userAgent());
        }

        // 执行拦截器链的操作，相当于发起一个网络请求
        Response networkResponse = chain.proceed(requestBuilder.build());

        // 解析服务器返回的Header，如果没有这个cookie，则不进行解析
        HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());

        Response.Builder responseBuilder = networkResponse.newBuilder()
                .request(userRequest);

        // 第一个条件为true告诉服务端表示支持gzip压缩 第二个条件 判断服务器返回的相应体内容是否经过"gzip"压缩
        if (transparentGzip
                && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
                && HttpHeaders.hasBody(networkResponse)) {
            // 将输入流转换成GzipSource实体解压的数据流
            GzipSource responseBody = new GzipSource(networkResponse.body().source());
            Headers strippedHeaders = networkResponse.headers().newBuilder()
                    .removeAll("Content-Encoding")
                    .removeAll("Content-Length")
                    .build();
            responseBuilder.headers(strippedHeaders);
            String contentType = networkResponse.header("Content-Type");
            // 这里可以看到压缩解压的操作是由Okio库来处理的
            responseBuilder.body(new RealResponseBody(contentType, -1L, Okio.buffer(responseBody)));
        }

        return responseBuilder.build();
    }

    /** Returns a 'Cookie' HTTP request header with all cookies, like {@code a=b; c=d}. */
    private String cookieHeader(List<Cookie> cookies) {
        StringBuilder cookieHeader = new StringBuilder();
        for (int i = 0, size = cookies.size(); i < size; i++) {
            if (i > 0) {
                cookieHeader.append("; ");
            }
            Cookie cookie = cookies.get(i);
            cookieHeader.append(cookie.name()).append('=').append(cookie.value());
        }
        return cookieHeader.toString();
    }
}
