package com.example.fly.componentbased.okhttp.connect;

import android.support.annotation.Nullable;

import java.lang.ref.Reference;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
//import javax.annotation.Nullable;

import okhttp3.Address;
import okhttp3.Connection;
import okhttp3.Route;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.RouteDatabase;
import okhttp3.internal.connection.StreamAllocation;
import okhttp3.internal.platform.Platform;

import static okhttp3.internal.Util.closeQuietly;

/**
 * Manages reuse of HTTP and HTTP/2 connections for reduced network latency. HTTP requests that
 * share the same {@link Address} may share a {@link Connection}. This class implements the policy
 * of which connections to keep open for future use.
 * 在有限的时间内复用链接
 * 连接池中的连接是如何回收的
 * 连接池，用来管理和复用连接
 */
public final class ConnectionPool {
    /**
     * 后台线程用于清除过期的连接
     *
     * 每个连接池最多只能运行一个线程
     *
     * 线程池执行器允许对池本身进行垃圾收集
     */
    private static final Executor executor = new ThreadPoolExecutor(0 /* corePoolSize */,
            Integer.MAX_VALUE /* maximumPoolSize */, 60L /* keepAliveTime */, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp ConnectionPool", true));

    /** 每个地址的最大空闲连接数. */
    private final int maxIdleConnections; // 默认 5
    // 每个keep-alive时长为5分钟
    private final long keepAliveDurationNs; // 默认5分钟
    private final Runnable cleanupRunnable = new Runnable() {
        @Override public void run() {
            while (true) {
                long waitNanos = cleanup(System.nanoTime()); // cleanup方法里面就是具体的GC回收算法，类似于GC的标记清除算法
                if (waitNanos == -1) return;
                if (waitNanos > 0) {
                    long waitMillis = waitNanos / 1000000L;
                    waitNanos -= (waitMillis * 1000000L);
                    synchronized (ConnectionPool.this) {
                        try {
                            ConnectionPool.this.wait(waitMillis, (int) waitNanos);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        }
    };

    // 连接池中维护了一个双端队列Deque来存储连接
    private final Deque<RealConnection> connections = new ArrayDeque<>();
    final RouteDatabase routeDatabase = new RouteDatabase();
    boolean cleanupRunning;

    /**
     * Create a new connection pool with tuning parameters appropriate for a single-user application.
     * The tuning parameters in this pool are subject to change in future OkHttp releases. Currently
     * this pool holds up to 5 idle connections which will be evicted after 5 minutes of inactivity.
     * TODO 连接池最多保持5个地址的连接keep-alive，每个keep-alive时长为5分钟
     */
    public ConnectionPool() {
        this(5, 5, TimeUnit.MINUTES);
    }

    public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
        this.maxIdleConnections = maxIdleConnections;
        this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);

        // Put a floor on the keep alive duration, otherwise cleanup will spin loop.
        // 在保持活跃状态的持续时间内放置任务，否则将循环清理
        if (keepAliveDuration <= 0) {
            throw new IllegalArgumentException("keepAliveDuration <= 0: " + keepAliveDuration);
        }
    }

    // Returns the number of idle connections in the pool.
    // 返回连接池内空闲的连接数
    public synchronized int idleConnectionCount() {
        int total = 0;
        for (RealConnection connection : connections) {
            if (connection.allocations.isEmpty()) total++;
        }
        return total;
    }

    /**
     * Returns total number of connections in the pool. Note that prior to OkHttp 2.7 this included
     * only idle connections and HTTP/2 connections. Since OkHttp 2.7 this includes all connections,
     * both active and inactive. Use {@link #idleConnectionCount()} to count connections not currently
     * in use.
     */
    public synchronized int connectionCount() {
        return connections.size();
    }

    /**
     * Returns a recycled connection to {@code address}, or null if no such connection exists.
     * 返回连接，如果不存在此类连接，则返回空值
     * The route is null if the address has not yet been routed.
     * 如果地址尚未路由，则路由为空
     */
    @Nullable
    RealConnection get(Address address, StreamAllocation streamAllocation, Route route) {
        assert (Thread.holdsLock(this));
        for (RealConnection connection : connections) {
            if (connection.isEligible(address, route)) { // 判断连接是否可用
                streamAllocation.acquire(connection, true);
                return connection;
            }
        }
        return null;
    }

    /**
     * Replaces the connection held by {@code streamAllocation} with a shared connection if possible.
     * 如果可能，将@code streamallocation持有的连接替换为共享连接
     * This recovers when multiple multiplexed connections are created concurrently.
     * 当同时创建多个多路复用连接时，恢复
     */
    @Nullable Socket deduplicate(Address address, StreamAllocation streamAllocation) {
        assert (Thread.holdsLock(this));
        for (RealConnection connection : connections) {
            if (connection.isEligible(address, null)
                    && connection.isMultiplexed()
                    && connection != streamAllocation.connection()) {
                return streamAllocation.releaseAndAcquire(connection);
            }
        }
        return null;
    }

    void put(RealConnection connection) {
        assert (Thread.holdsLock(this));
        // 没有任何连接时,cleanupRunning = false;
        // 即没有任何链接时才会去执行executor.execute(cleanupRunnable);
        // 从而保证每个连接池最多只能运行一个线程。
        if (!cleanupRunning) {
            cleanupRunning = true;
            executor.execute(cleanupRunnable); // 做异步处理任务
        }
        connections.add(connection); // 将连接加入到双端队列
    }

    /**
     * Notify this pool that {@code connection} has become idle. Returns true if the connection has
     * been removed from the pool and should be closed.
     * 把连接变为闲置状态
     */
    boolean connectionBecameIdle(RealConnection connection) {
        assert (Thread.holdsLock(this));
        if (connection.noNewStreams || maxIdleConnections == 0) {
            connections.remove(connection);
            return true;
        } else {
            notifyAll(); // Awake the cleanup thread: we may have exceeded the idle connection limit.
            return false;
        }
    }

    /** Close and remove all idle connections in the pool. */
    // 关闭并删除池中的所有空闲连接
    public void evictAll() {
        List<RealConnection> evictedConnections = new ArrayList<>();
        synchronized (this) {
            for (Iterator<RealConnection> i = connections.iterator(); i.hasNext(); ) {
                RealConnection connection = i.next();
                if (connection.allocations.isEmpty()) {
                    connection.noNewStreams = true;
                    evictedConnections.add(connection);
                    i.remove();
                }
            }
        }

        for (RealConnection connection : evictedConnections) {
            closeQuietly(connection.socket());
        }
    }

    /**
     * Performs maintenance on this pool, evicting the connection that has been idle the longest if
     * either it has exceeded the keep alive limit or the idle connections limit.
     * 对该池执行维护，如果已超过保持活动状态限制或空闲连接限制，则清除空闲时间最长的连接
     *
     * <p>Returns the duration in nanos to sleep until the next scheduled call to this method.
     * 返回在下次计划调用此方法之前休眠的持续时间（纳秒）
     *
     * Returns -1 if no further cleanups are required.
     * 如果不需要进一步清理，则返回-1
     *
     */
    long cleanup(long now) {
        int inUseConnectionCount = 0;
        int idleConnectionCount = 0;
        RealConnection longestIdleConnection = null;
        long longestIdleDurationNs = Long.MIN_VALUE;

        // Find either a connection to evict, or the time that the next eviction is due.
        // 遍历队列当中所有的RealConnection集合，去标记泄露或者不活跃的连接
        synchronized (this) {
            for (Iterator<RealConnection> i = connections.iterator(); i.hasNext(); ) {
                RealConnection connection = i.next();

                // If the connection is in use, keep searching.
                // 如果连接正在使用中，请继续搜索
                if (pruneAndGetAllocationCount(connection, now) > 0) {
                    inUseConnectionCount++;
                    continue;
                }

                idleConnectionCount++;

                // If the connection is ready to be evicted, we're done.
                // 如果连接准备好被收回，标记为空闲连接
                long idleDurationNs = now - connection.idleAtNanos;
                if (idleDurationNs > longestIdleDurationNs) {
                    longestIdleDurationNs = idleDurationNs;
                    longestIdleConnection = connection;
                }
            }

            // 如果被标记的连接满足空闲的socekt连接超过5个
            if (longestIdleDurationNs >= this.keepAliveDurationNs
                    || idleConnectionCount > this.maxIdleConnections) {
                // We've found a connection to evict. Remove it from the list, then close it below (outside
                // of the synchronized block).
                // 如果空闲连接超过5个或者keepalive时间大于5分钟，则将该连接清理掉，然后在下面关闭它（同步块外部）
                connections.remove(longestIdleConnection); // 这时候就会把连接从集合中移除并关闭
            } else if (idleConnectionCount > 0) {
                // A connection will be ready to evict soon.
                return keepAliveDurationNs - longestIdleDurationNs; // 返回此连接的到期时间，供下次进行清理
            } else if (inUseConnectionCount > 0) {
                // All connections are in use. It'll be at least the keep alive duration 'til we run again.
                return keepAliveDurationNs; // 全部都是活跃连接，5分钟时候再进行清理
            } else { // 没有连接
                // No connections, idle or in use.
                cleanupRunning = false;
                return -1;  // 返回-1 跳出循环
            }
        }

        // 关闭连接，返回时间0，立即再次进行清理
        closeQuietly(longestIdleConnection.socket());

        // Cleanup again immediately.
        // 立即再次清理
        return 0;
    }

    /**
     * Prunes any leaked allocations and then returns the number of remaining live allocations on {@code connection}
     * 删除任何泄漏的分配，然后返回@code connection上剩余的活动分配数
     *
     * Allocations are leaked if the connection is tracking them but the application code has abandoned them.
     * 如果连接正在跟踪分配，但应用程序代码已放弃分配，则会泄漏分配
     *
     * Leak detection is imprecise and relies on garbage collection.
     * 泄漏检测不精确，依赖于垃圾收集。
     *
     * 如何找到最不活跃的链接呢
     */
    private int pruneAndGetAllocationCount(RealConnection connection, long now) {
        // 虚引用列表
        List<Reference<StreamAllocation>> references = connection.allocations;
        // 遍历虚引用列表
        for (int i = 0; i < references.size(); ) {
            Reference<StreamAllocation> reference = references.get(i);
            // 如果虚引用StreamAllocation正在被使用，则跳过进行下一次循环
            if (reference.get() != null) {
                i++; // 引用计数
                continue;
            }

            // We've discovered a leaked allocation. This is an application bug.
            // 我们发现了一个泄露的分配。这是一个应用程序bug
            StreamAllocation.StreamAllocationReference streamAllocRef =
                    (StreamAllocation.StreamAllocationReference) reference;
            String message = "A connection to " + connection.route().address().url()
                    + " was leaked. Did you forget to close a response body?";
            Platform.get().logCloseableLeak(message, streamAllocRef.callStackTrace);

            references.remove(i);
            connection.noNewStreams = true;

            // If this was the last allocation, the connection is eligible for immediate eviction.
            // 如果所有的StreamAllocation引用都没有了，返回引用计数0
            if (references.isEmpty()) {
                connection.idleAtNanos = now - keepAliveDurationNs;
                return 0; // 表示这个连接没有代码引用了
            }
        }

        return references.size(); // 返回剩余的活动分配数 (返回引用列表的大小，作为引用计数)
    }
}
