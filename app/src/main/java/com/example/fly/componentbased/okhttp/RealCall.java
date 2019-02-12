///*
// * Copyright (C) 2014 Square, Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.example.fly.componentbased.okhttp;
//
//import android.support.annotation.Nullable;
//
//import com.example.fly.componentbased.okhttp.connect.StreamAllocation;
//
//import java.io.IOException;
//import java.io.InterruptedIOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.RejectedExecutionException;
////import javax.annotation.Nullable;
//
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.EventListener;
//import okhttp3.Interceptor;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//import okhttp3.internal.NamedRunnable;
//import okhttp3.internal.cache.CacheInterceptor;
//import okhttp3.internal.connection.ConnectInterceptor;
//import okhttp3.internal.http.BridgeInterceptor;
//import okhttp3.internal.http.CallServerInterceptor;
//import okhttp3.internal.http.RealInterceptorChain;
//import okhttp3.internal.http.RetryAndFollowUpInterceptor;
//import okhttp3.internal.platform.Platform;
//import okio.AsyncTimeout;
//import okio.Timeout;
//
//import static java.util.concurrent.TimeUnit.MILLISECONDS;
//import static okhttp3.internal.platform.Platform.INFO;
//
//final class RealCall implements okhttp3.Call {
//    final OkHttpClient client;
//    final RetryAndFollowUpInterceptor retryAndFollowUpInterceptor;
//    final AsyncTimeout timeout;
//
//    /**
//     * There is a cycle between the {@link Call} and {@link EventListener} that makes this awkward.
//     * This will be set after we create the call instance then create the event listener instance.
//     */
//    private @Nullable
//    EventListener eventListener;
//
//    /** The application's original request unadulterated by redirects or auth headers. */
//    final okhttp3.Request originalRequest;
//    final boolean forWebSocket;
//
//    // Guarded by this.
//    private boolean executed;
//
//    private RealCall(OkHttpClient client, okhttp3.Request originalRequest, boolean forWebSocket) {
//        this.client = client;  // 初始化客户端请求
//        this.originalRequest = originalRequest; // 初始化好request对象
//        this.forWebSocket = forWebSocket;
//        // 创建重定向拦截器
//        this.retryAndFollowUpInterceptor = new RetryAndFollowUpInterceptor(client, forWebSocket);
//        this.timeout = new AsyncTimeout() {  // 创建异步超时对象
//            @Override protected void timedOut() {
//                cancel();
//            }
//        };
//        this.timeout.timeout(client.callTimeoutMillis(), MILLISECONDS); // 初始化超时时间
//    }
//
//    static RealCall newRealCall(OkHttpClient client, okhttp3.Request originalRequest, boolean forWebSocket) {
//        // Safely publish the Call instance to the EventListener.
//        RealCall call = new RealCall(client, originalRequest, forWebSocket);
//        call.eventListener = client.eventListenerFactory().create(call);
//        return call;
//    }
//
//    @Override public okhttp3.Request request() {
//        return originalRequest;
//    }
//
//    @Override public Response execute() throws IOException {
//        synchronized (this) { // 加同步锁
//            // 同一个http请求只能执行一次，执行完就设置为你true
//            if (executed) throw new IllegalStateException("Already Executed");
//            executed = true;
//        }
//        captureCallStackTrace();
//        timeout.enter();
//        eventListener.callStart(this);
//        try {
//            client.dispatcher().executed(this);
//            Response result = getResponseWithInterceptorChain();
//            if (result == null) throw new IOException("Canceled");
//            return result;
//        } catch (IOException e) {
//            e = timeoutExit(e);
//            eventListener.callFailed(this, e);
//            throw e;
//        } finally {
//            client.dispatcher().finished(this);
//        }
//    }
//
//    @Nullable IOException timeoutExit(@Nullable IOException cause) {
//        if (!timeout.exit()) return cause;
//
//        InterruptedIOException e = new InterruptedIOException("timeout");
//        if (cause != null) {
//            e.initCause(cause);
//        }
//        return e;
//    }
//
//    private void captureCallStackTrace() {
//        Object callStackTrace = Platform.get().getStackTraceForCloseable("response.body().close()");
//        retryAndFollowUpInterceptor.setCallStackTrace(callStackTrace);
//    }
//
//    @Override public void enqueue(Callback responseCallback) {
//        synchronized (this) {
//            // 和同步一样判断是否请求过这个链接，已经请求过就直接抛出异常
//            if (executed) throw new IllegalStateException("Already Executed");
//            executed = true; // 请求过就设置为true
//        }
//        captureCallStackTrace();
//        eventListener.callStart(this);
//        client.dispatcher().enqueue(new AsyncCall(responseCallback)); // 关键代码
//    }
//
//    @Override public void cancel() {
//        retryAndFollowUpInterceptor.cancel();
//    }
//
//    @Override public Timeout timeout() {
//        return timeout;
//    }
//
//    @Override public synchronized boolean isExecuted() {
//        return executed;
//    }
//
//    @Override public boolean isCanceled() {
//        return retryAndFollowUpInterceptor.isCanceled();
//    }
//
//    @SuppressWarnings("CloneDoesntCallSuperClone") // We are a final type & this saves clearing state.
//    @Override public RealCall clone() {
//        return RealCall.newRealCall(client, originalRequest, forWebSocket);
//    }
//
//    StreamAllocation streamAllocation() {
//        return retryAndFollowUpInterceptor.streamAllocation();
//    }
//
//    final class AsyncCall extends NamedRunnable {
//        private final Callback responseCallback;
//
//        AsyncCall(Callback responseCallback) {
//            super("OkHttp %s", redactedUrl());
//            this.responseCallback = responseCallback;
//        }
//
//        String host() {
//            return originalRequest.url().host();
//        }
//
//        Request request() {
//            return originalRequest;
//        }
//
//        RealCall get() {
//            return RealCall.this;
//        }
//
//        /**
//         * Attempt to enqueue this async call on {@code executorService}. This will attempt to clean up
//         * if the executor has been shut down by reporting the call as failed.
//         */
//        void executeOn(ExecutorService executorService) {
//            assert (!Thread.holdsLock(client.dispatcher()));
//            boolean success = false;
//            try {
//                executorService.execute(this); // 在这里才真正开始执行线程
//                success = true;
//            } catch (RejectedExecutionException e) {
//                InterruptedIOException ioException = new InterruptedIOException("executor rejected");
//                ioException.initCause(e);
//                eventListener.callFailed(RealCall.this, ioException);
//                responseCallback.onFailure(RealCall.this, ioException);
//            } finally {
//                if (!success) {
//                    client.dispatcher().finished(this); // This call is no longer running!
//                }
//            }
//        }
//
//        // 这个方法才是线程真正实现的方法，在父类NamedRunnable中是一个抽象的方法，在线程的run方法中实现的
//        @Override protected void execute() { // 这个方法是在子线程中操作的
//            boolean signalledCallback = false;
//            timeout.enter();
//            try {
//                Response response = getResponseWithInterceptorChain(); // 拦截器链
//                if (retryAndFollowUpInterceptor.isCanceled()) { // 重定向和重试的拦截器是否取消了
//                    signalledCallback = true;
//                    // 如果取消了，就回掉返回失败给用户
//                    responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
//                } else {
//                    signalledCallback = true;
//                    // 没取消重定向拦截器就正常返回数据
//                    responseCallback.onResponse(RealCall.this, response);
//                }
//            } catch (IOException e) {
//                e = timeoutExit(e);
//                if (signalledCallback) {
//                    // Do not signal the callback twice!
//                    Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
//                } else {
//                    eventListener.callFailed(RealCall.this, e);
//                    responseCallback.onFailure(RealCall.this, e);
//                }
//            } finally {
//                /**
//                 * 这个finish做的事情
//                 * 1.把这个请求从正在请求的队列中删除
//                 * 2.调整我们整个异步请求的队列，因为这个队列是非线程安全的
//                 * 3.重新调整异步请求的数量
//                 */
//                client.dispatcher().finished(this);
//            }
//        }
//    }
//
//    /**
//     * Returns a string that describes this call. Doesn't include a full URL as that might contain
//     * sensitive information.
//     */
//    String toLoggableString() {
//        return (isCanceled() ? "canceled " : "")
//                + (forWebSocket ? "web socket" : "call")
//                + " to " + redactedUrl();
//    }
//
//    String redactedUrl() {
//        return originalRequest.url().redact();
//    }
//
//    // 所使用的拦截器链 在官网上分为两个拦截器 Application 应用拦截器和NetWork网络拦截器
//    Response getResponseWithInterceptorChain() throws IOException {
//        // Build a full stack of interceptors.
//        List<Interceptor> interceptors = new ArrayList<>();
//        interceptors.addAll(client.interceptors()); // 添加一个用户自定义的拦截器
//        interceptors.add(retryAndFollowUpInterceptor);
//        interceptors.add(new BridgeInterceptor(client.cookieJar()));
//        interceptors.add(new CacheInterceptor(client.internalCache()));
//        interceptors.add(new ConnectInterceptor(client));
//        if (!forWebSocket) {
//            interceptors.addAll(client.networkInterceptors());
//        }
//        interceptors.add(new CallServerInterceptor(forWebSocket));
//
//        Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
//                originalRequest, this, eventListener, client.connectTimeoutMillis(),
//                client.readTimeoutMillis(), client.writeTimeoutMillis());
//
//        return chain.proceed(originalRequest); // 拦截后调用proceed方法来执行相应请求
//    }
//}
