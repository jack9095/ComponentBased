package com.example.fly.componentbased.okhttp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RealCall;
import okhttp3.RealCall.AsyncCall;
import okhttp3.Response;
import okhttp3.internal.Util;

/**
 * Policy on when async requests are executed.
 */
public final class Dispatcher {
    private int maxRequests = 64;  // 异步请求的最大个数为64
    private int maxRequestsPerHost = 5; // 相同主机最大请求数
    private @Nullable Runnable idleCallback;

    /** Executes calls. Created lazily. */
    private @Nullable ExecutorService executorService; // 线程池

    /** Ready async calls in the order they'll be run.当请求条件不满足我们的条件时，异步请求就会进入这个队列当中等待执行
     * 这里的条件就是大于异步请求的最大个数为64 和 请求的主机数量大于5 */
    private final Deque<AsyncCall> readyAsyncCalls = new ArrayDeque<>(); // 异步就绪队列，缓存等待的请求队列

    /** Running asynchronous calls. Includes canceled calls that haven't finished yet.包含了已经取消但没有执行的异步请求 */
    private final Deque<AsyncCall> runningAsyncCalls = new ArrayDeque<>(); // 正在执行的异步 队列,判断并发请求的数量

    /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
    private final Deque<okhttp3.RealCall> runningSyncCalls = new ArrayDeque<>(); // 同步的执行队列

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Dispatcher() {
    }

    // 返回一个线程池对象 在这里保证线程池是一个单例
    public synchronized ExecutorService executorService() {
        if (executorService == null) {
            // 第一个参数表示设置核心线程池的数量，这里设置为0，表示线程空闲，超过60秒后会把所有的线程全部销毁
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
        }
        return executorService;
    }

    /**
     * Set the maximum number of requests to execute concurrently. Above this requests queue in
     * memory, waiting for the running calls to complete.
     *
     * <p>If more than {@code maxRequests} requests are in flight when this is invoked, those requests
     * will remain in flight.
     */
    public void setMaxRequests(int maxRequests) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequests);
        }
        synchronized (this) {
            this.maxRequests = maxRequests;
        }
        promoteAndExecute();
    }

    public synchronized int getMaxRequests() {
        return maxRequests;
    }

    /**
     * Set the maximum number of requests for each host to execute concurrently. This limits requests
     * by the URL's host name. Note that concurrent requests to a single IP address may still exceed
     * this limit: multiple hostnames may share an IP address or be routed through the same HTTP
     * proxy.
     *
     * <p>If more than {@code maxRequestsPerHost} requests are in flight when this is invoked, those
     * requests will remain in flight.
     *
     * <p>WebSocket connections to hosts <b>do not</b> count against this limit.
     */
    public void setMaxRequestsPerHost(int maxRequestsPerHost) {
        if (maxRequestsPerHost < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequestsPerHost);
        }
        synchronized (this) {
            this.maxRequestsPerHost = maxRequestsPerHost;
        }
        promoteAndExecute();
    }

    public synchronized int getMaxRequestsPerHost() {
        return maxRequestsPerHost;
    }

    /**
     * Set a callback to be invoked each time the dispatcher becomes idle (when the number of running
     * calls returns to zero).
     *
     * <p>Note: The time at which a {@linkplain okhttp3.Call call} is considered idle is different depending
     * on whether it was run {@linkplain okhttp3.Call#enqueue(Callback) asynchronously} or
     * {@linkplain okhttp3.Call#execute() synchronously}. Asynchronous calls become idle after the
     * {@link Callback#onResponse onResponse} or {@link Callback#onFailure onFailure} callback has
     * returned. Synchronous calls become idle once {@link okhttp3.Call#execute() execute()} returns. This
     * means that if you are doing synchronous calls the network layer will not truly be idle until
     * every returned {@link Response} has been closed.
     */
    public synchronized void setIdleCallback(@Nullable Runnable idleCallback) {
        this.idleCallback = idleCallback;
    }

    void enqueue(AsyncCall call) {
        synchronized (this) {
            readyAsyncCalls.add(call); // 加入等待队列当中
        }
        promoteAndExecute();
    }

    /**
     * Cancel all calls currently enqueued or executing. Includes calls executed both {@linkplain
     * okhttp3.Call#execute() synchronously} and {@linkplain okhttp3.Call#enqueue asynchronously}.
     */
    public synchronized void cancelAll() {
        for (AsyncCall call : readyAsyncCalls) {
            call.get().cancel();
        }

        for (AsyncCall call : runningAsyncCalls) {
            call.get().cancel();
        }

        for (okhttp3.RealCall call : runningSyncCalls) {
            call.cancel();
        }
    }

    /**
     * Promotes eligible calls from {@link #readyAsyncCalls} to {@link #runningAsyncCalls} and runs
     * them on the executor service. Must not be called with synchronization because executing calls
     * can call into user code.
     *
     * @return true if the dispatcher is currently running calls.
     */
    private boolean promoteAndExecute() {
        assert (!Thread.holdsLock(this));

        List<AsyncCall> executableCalls = new ArrayList<>();
        boolean isRunning;
        synchronized (this) {
            // 循环缓存等待的异步请求队列
            for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
                AsyncCall asyncCall = i.next(); // 把循环的队列中的元素取出
                // 当前异步请求的个数大于最大请求数的时候就打断  break 是跳出整个循环体
                if (runningAsyncCalls.size() >= maxRequests) break; // Max capacity.
                // 当前网络请求的Host大于等于5个请求的时候  continue 结束单次循环并继续
                if (runningCallsForHost(asyncCall) >= maxRequestsPerHost) continue; // Host max capacity.

                i.remove(); // 把上面取出的元素从队列中删除
                executableCalls.add(asyncCall);
                runningAsyncCalls.add(asyncCall); // 添加到正在执行的异步请求队列中,重新计算正在执行异步请求的数量
            }
            isRunning = runningCallsCount() > 0;
        }

        for (int i = 0, size = executableCalls.size(); i < size; i++) {
            AsyncCall asyncCall = executableCalls.get(i);
            asyncCall.executeOn(executorService()); // 通过线程池执行网络请求
        }

        return isRunning;
    }

    /** Returns the number of running calls that share a host with {@code call}. */
    private int runningCallsForHost(AsyncCall call) {
        int result = 0;
        for (AsyncCall c : runningAsyncCalls) {
            if (c.get().forWebSocket) continue;
            if (c.host().equals(call.host())) result++;
        }
        return result;
    }

    /** Used by {@code Call#execute} to signal it is in-flight. */
    synchronized void executed(okhttp3.RealCall call) {
        runningSyncCalls.add(call); // 把请求添加到同步请求队列当中队列当中
    }

    /** Used by {@code AsyncCall#run} to signal completion. */
    void finished(AsyncCall call) {
        finished(runningAsyncCalls, call);
    }

    /** Used by {@code Call#execute} to signal completion. */
    void finished(okhttp3.RealCall call) {
        finished(runningSyncCalls, call);
    }

    private <T> void finished(Deque<T> calls, T call) {
        Runnable idleCallback;
        synchronized (this) {
            // 移除网络请求从队列中，不能移除就抛出异常
            if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
            idleCallback = this.idleCallback;
        }

        boolean isRunning = promoteAndExecute();

        if (!isRunning && idleCallback != null) {
            idleCallback.run();
        }
    }

    /** Returns a snapshot of the calls currently awaiting execution. */
    public synchronized List<okhttp3.Call> queuedCalls() {
        List<okhttp3.Call> result = new ArrayList<>();
        for (AsyncCall asyncCall : readyAsyncCalls) {
            result.add(asyncCall.get());
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns a snapshot of the calls currently being executed. */
    public synchronized List<okhttp3.Call> runningCalls() {
        List<Call> result = new ArrayList<>();
        result.addAll(runningSyncCalls);
        for (AsyncCall asyncCall : runningAsyncCalls) {
            result.add(asyncCall.get());
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized int queuedCallsCount() {
        return readyAsyncCalls.size();
    }

    // 返回执行异步请求和同步请求的数量总和
    public synchronized int runningCallsCount() {
        return runningAsyncCalls.size() + runningSyncCalls.size();
    }
}
