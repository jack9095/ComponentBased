package com.example.fly.componentbased.okhttp.connect;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.List;

import okhttp3.Address;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.ConnectionPool;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Route;
import okhttp3.internal.Internal;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.RouteDatabase;
import okhttp3.internal.connection.RouteException;
import okhttp3.internal.connection.RouteSelector;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.http2.ConnectionShutdownException;
import okhttp3.internal.http2.ErrorCode;
import okhttp3.internal.http2.StreamResetException;

import static okhttp3.internal.Util.closeQuietly;

/**
 * This class coordinates the relationship between three entities:
 *
 * <ul>
 * <li><strong>Connections:</strong> physical socket connections to remote servers. These are
 * potentially slow to establish so it is necessary to be able to cancel a connection
 * currently being connected.
 * <li><strong>Streams:</strong> logical HTTP request/response pairs that are layered on
 * connections. Each connection has its own allocation limit, which defines how many
 * concurrent streams that connection can carry. HTTP/1.x connections can carry 1 stream
 * at a time, HTTP/2 typically carry multiple.
 * <li><strong>Calls:</strong> a logical sequence of streams, typically an initial request and
 * its follow up requests. We prefer to keep all streams of a single call on the same
 * connection for better behavior and locality.
 * </ul>
 *
 * <p>Instances of this class act on behalf of the call, using one or more streams over one or more
 * connections. This class has APIs to release each of the above resources:
 *
 * <ul>
 * <li>{@link #noNewStreams()} prevents the connection from being used for new streams in the
 * future. Use this after a {@code Connection: close} header, or when the connection may be
 * inconsistent.
 * <li>{@link #streamFinished streamFinished()} releases the active stream from this allocation.
 * Note that only one stream may be active at a given time, so it is necessary to call
 * {@link #streamFinished streamFinished()} before creating a subsequent stream with {@link
 * #newStream newStream()}.
 * <li>{@link #release()} removes the call's hold on the connection. Note that this won't
 * immediately free the connection if there is a stream still lingering. That happens when a
 * call is complete but its response body has yet to be fully consumed.
 * </ul>
 *
 * <p>This class supports {@linkplain #cancel asynchronous canceling}. This is intended to have the
 * smallest blast radius possible. If an HTTP/2 stream is active, canceling will cancel that stream
 * but not the other streams sharing its connection. But if the TLS handshake is still in progress
 * then canceling may break the entire connection.
 * <p>
 * StreamAllocation相当于是个管理类，维护了Connections、Streams和Calls之间的管理，该类初始化一个Socket连接对象，获取输入/输出流对象
 */
public final class StreamAllocation {
    public final Address address;
    private RouteSelector.Selection routeSelection;
    private Route route;
    private final ConnectionPool connectionPool;
    public final Call call;
    public final EventListener eventListener;
    private final Object callStackTrace;

    // State guarded by connectionPool.  由ConnectionPool保护的状态
    private final RouteSelector routeSelector;
    private int refusedStreamCount;
    private RealConnection connection;
    private boolean reportedAcquired;
    private boolean released;
    private boolean canceled;
    private HttpCodec codec;

    public StreamAllocation(ConnectionPool connectionPool, Address address, Call call,
                            EventListener eventListener, Object callStackTrace) {
        this.connectionPool = connectionPool;
        this.address = address;
        this.call = call;
        this.eventListener = eventListener;
        this.routeSelector = new RouteSelector(address, routeDatabase(), call, eventListener);
        this.callStackTrace = callStackTrace;
    }

    // 创建HttpCodec
    public HttpCodec newStream(OkHttpClient client, Interceptor.Chain chain, boolean doExtensiveHealthChecks) {

        int connectTimeout = chain.connectTimeoutMillis(); // 设置的连接超时时间
        int readTimeout = chain.readTimeoutMillis();  // 读取超时
        int writeTimeout = chain.writeTimeoutMillis(); // 写入超时
        int pingIntervalMillis = client.pingIntervalMillis(); // Web socket ping 间隔 (毫秒) 定时通知服务器，为心跳连接做准备，如果pingIntervalMillis 设置为0的时候 心跳executor是不会执行的
        boolean connectionRetryEnabled = client.retryOnConnectionFailure();  // 连接失败是否重试

        try {
            // 生成实际的网络连接类 ，RealConnection利用Socket建立连接
            RealConnection resultConnection = findHealthyConnection(connectTimeout, readTimeout,
                    writeTimeout, pingIntervalMillis, connectionRetryEnabled, doExtensiveHealthChecks);
            // 通过网络连接的实际类生成网络请求和网络响应的编码类
            HttpCodec resultCodec = resultConnection.newCodec(client, chain, this);

            synchronized (connectionPool) {
                codec = resultCodec;
                return resultCodec;
            }
        } catch (IOException e) {
            throw new RouteException(e);
        }
    }

    /**
     * 找到一个连接，如果它是健康的，则返回它.
     * 如果不正常(健康)，则重复该过程，直到找到正常连接为止
     */
    private RealConnection findHealthyConnection(int connectTimeout, int readTimeout,
                                                 int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled,
                                                 boolean doExtensiveHealthChecks) throws IOException {
        while (true) {
            RealConnection candidate = findConnection(connectTimeout, readTimeout, writeTimeout,
                    pingIntervalMillis, connectionRetryEnabled);

            // If this is a brand new connection, we can skip the extensive health checks.
            synchronized (connectionPool) {
                if (candidate.successCount == 0) { // 等于0的时候表示整个网络请求已经结束了
                    return candidate;
                }
            }

            // Do a (potentially slow) check to confirm that the pooled connection is still good. If it
            // isn't, take it out of the pool and start again.
            // 不健康，网络链接没及时关闭，输入输出流没有及时关闭，这时候就认为不健康
            if (!candidate.isHealthy(doExtensiveHealthChecks)) { // 当这个网络连接类不健康
                noNewStreams(); // 回收网络请求资源
                continue; // 跳出这次循环，接着下一次循环
            }

            return candidate;
        }
    }

    /**
     * Returns a connection to host a new stream. This prefers the existing connection if it exists,
     * then the pool, finally building a new connection.
     * 返回一个连接来托管一个新的流。 可以复用现有的连接（如果存在的话），然后是池，最后建立一个新的连接
     */
    private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout,
                                          int pingIntervalMillis, boolean connectionRetryEnabled) throws IOException {
        boolean foundPooledConnection = false;
        RealConnection result = null;
        Route selectedRoute = null;
        Connection releasedConnection;
        Socket toClose;
        synchronized (connectionPool) {
            if (released) throw new IllegalStateException("released");
            if (codec != null) throw new IllegalStateException("codec != null");
            if (canceled) throw new IOException("Canceled");

            // Attempt to use an already-allocated connection. We need to be careful here because our
            // already-allocated connection may have been restricted from creating new streams.
            // 翻译上面的注释：尝试使用已分配的连接。 我们在这里需要小心，因为我们已经分配的连接可能已经被限制在创建新的流中
            releasedConnection = this.connection; // 直接复用
            toClose = releaseIfNoNewStreams();
            // 查看是否有完好的连接
            if (this.connection != null) {
                // We had an already-allocated connection and it's good.
                result = this.connection;
                releasedConnection = null;
            }
            if (!reportedAcquired) {
                // If the connection was never reported acquired, don't report it as released!
                releasedConnection = null;
            }
            // 连接池中是否用可用的连接，有则使用
            if (result == null) {
                // Attempt to get a connection from the pool. 从连接池中返回一个RealConnection
                Internal.instance.get(connectionPool, address, this, null);
                if (connection != null) {
                    foundPooledConnection = true;
                    result = connection;
                } else {
                    selectedRoute = route;
                }
            }
        }
        closeQuietly(toClose);

        if (releasedConnection != null) {
            eventListener.connectionReleased(call, releasedConnection);
        }
        if (foundPooledConnection) {
            eventListener.connectionAcquired(call, result);
        }
        if (result != null) {
            // If we found an already-allocated or pooled connection, we're done.
            // 如果我们找到了已经分配或者连接的连接，我们就完成了，直接返回
            return result;
        }

        // If we need a route selection, make one. This is a blocking operation.
        // 如果我们需要路线选择，请选择一个。 这是一项阻止操作。
        // 线程的选择，多IP操作
        boolean newRouteSelection = false;
        if (selectedRoute == null && (routeSelection == null || !routeSelection.hasNext())) {
            newRouteSelection = true;
            routeSelection = routeSelector.next();
        }

        // 如果没有可用连接，则自己创建一个
        synchronized (connectionPool) {
            if (canceled) throw new IOException("Canceled");

            if (newRouteSelection) {
                // Now that we have a set of IP addresses, make another attempt at getting a connection from
                // the pool. This could match due to connection coalescing.
                // 现在我们有一组IP地址，再次尝试从池中获取连接。 这可能由于连接合并而匹配
                List<Route> routes = routeSelection.getAll();
                for (int i = 0, size = routes.size(); i < size; i++) {
                    Route route = routes.get(i);
                    Internal.instance.get(connectionPool, address, this, route);
                    if (connection != null) {
                        foundPooledConnection = true;
                        result = connection;
                        this.route = route;
                        break;
                    }
                }
            }

            if (!foundPooledConnection) {
                if (selectedRoute == null) {
                    selectedRoute = routeSelection.next();
                }

                // Create a connection and assign it to this allocation immediately. This makes it possible
                // for an asynchronous cancel() to interrupt the handshake we're about to do.
                // 创建一个连接并立即将其分配给该分配。 这使得异步cancel（）可以中断我们即将进行的握手
                route = selectedRoute;
                refusedStreamCount = 0;
                result = new RealConnection(connectionPool, selectedRoute); // 创建连接
                acquire(result, false);
            }
        }

        // If we found a pooled connection on the 2nd time around, we're done.
        // 如果我们第二次发现一个连接池，我们就完成了
        if (foundPooledConnection) {
            eventListener.connectionAcquired(call, result);
            return result;
        }

        // Do TCP + TLS handshakes. This is a blocking operation. 进行实际的网络连接
        // 开始TCP以及TLS握手操作,这是阻塞操作
        result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis,
                connectionRetryEnabled, call, eventListener);
        routeDatabase().connected(result.route());

        // 将新创建的连接，放在连接池中
        Socket socket = null;
        synchronized (connectionPool) {
            reportedAcquired = true;

            // Pool the connection. 紧接着把这个RealConnection放入连接池中
            Internal.instance.put(connectionPool, result);

            // If another multiplexed connection to the same address was created concurrently, then
            // release this connection and acquire that one.
            // 如果同时创建了到同一地址的另一个多路复用连接，则释放此连接并获取该连接
            if (result.isMultiplexed()) {
                socket = Internal.instance.deduplicate(connectionPool, address, this);
                result = connection;
            }
        }
        closeQuietly(socket);

        eventListener.connectionAcquired(call, result);
        return result;
    }

    /**
     * Releases the currently held connection and returns a socket to close if the held connection
     * restricts new streams from being created. With HTTP/2 multiple requests share the same
     * connection so it's possible that our connection is restricted from creating new streams during
     * a follow-up request.
     */
    private Socket releaseIfNoNewStreams() {
        assert (Thread.holdsLock(connectionPool));
        RealConnection allocatedConnection = this.connection;
        if (allocatedConnection != null && allocatedConnection.noNewStreams) {
            return deallocate(false, false, true);
        }
        return null;
    }

    public void streamFinished(boolean noNewStreams, HttpCodec codec, long bytesRead, IOException e) {
        eventListener.responseBodyEnd(call, bytesRead);

        Socket socket;
        Connection releasedConnection;
        boolean callEnd;
        synchronized (connectionPool) {
            if (codec == null || codec != this.codec) {
                throw new IllegalStateException("expected " + this.codec + " but was " + codec);
            }
            if (!noNewStreams) {
                connection.successCount++;
            }
            releasedConnection = connection;
            socket = deallocate(noNewStreams, false, true);
            if (connection != null) releasedConnection = null;
            callEnd = this.released;
        }
        closeQuietly(socket);
        if (releasedConnection != null) {
            eventListener.connectionReleased(call, releasedConnection);
        }

        if (e != null) {
            e = Internal.instance.timeoutExit(call, e);
            eventListener.callFailed(call, e);
        } else if (callEnd) {
            Internal.instance.timeoutExit(call, null);
            eventListener.callEnd(call);
        }
    }

    public HttpCodec codec() {
        synchronized (connectionPool) {
            return codec;
        }
    }

    private RouteDatabase routeDatabase() {
        return Internal.instance.routeDatabase(connectionPool);
    }

    public Route route() {
        return route;
    }

    public synchronized RealConnection connection() {
        return connection;
    }

    public void release() {
        Socket socket;
        Connection releasedConnection;
        synchronized (connectionPool) {
            releasedConnection = connection;
            socket = deallocate(false, true, false);
            if (connection != null) releasedConnection = null;
        }
        closeQuietly(socket);
        if (releasedConnection != null) {
            Internal.instance.timeoutExit(call, null);
            eventListener.connectionReleased(call, releasedConnection);
            eventListener.callEnd(call);
        }
    }

    /**
     * Forbid new streams from being created on the connection that hosts this allocation.
     * 禁止新的流创建
     */
    public void noNewStreams() {
        Socket socket;
        Connection releasedConnection;
        synchronized (connectionPool) {
            releasedConnection = connection;
            socket = deallocate(true, false, false);
            if (connection != null) releasedConnection = null;
        }
        closeQuietly(socket);
        if (releasedConnection != null) {
            eventListener.connectionReleased(call, releasedConnection);
        }
    }

    /**
     * Releases resources held by this allocation. If sufficient resources are allocated, the
     * connection will be detached or closed. Callers must be synchronized on the connection pool.
     *
     * <p>Returns a closeable that the caller should pass to {@link Util#closeQuietly} upon completion
     * of the synchronized block. (We don't do I/O while synchronized on the connection pool.)
     */
    private Socket deallocate(boolean noNewStreams, boolean released, boolean streamFinished) {
        assert (Thread.holdsLock(connectionPool));

        if (streamFinished) {
            this.codec = null;
        }
        if (released) {
            this.released = true;
        }
        Socket socket = null;
        if (connection != null) {
            if (noNewStreams) {
                connection.noNewStreams = true;
            }
            if (this.codec == null && (this.released || connection.noNewStreams)) {
                release(connection);
                if (connection.allocations.isEmpty()) {
                    connection.idleAtNanos = System.nanoTime();
                    if (Internal.instance.connectionBecameIdle(connectionPool, connection)) {
                        socket = connection.socket();
                    }
                }
                connection = null;
            }
        }
        return socket;
    }

    public void cancel() {
        HttpCodec codecToCancel;
        RealConnection connectionToCancel;
        synchronized (connectionPool) {
            canceled = true;
            codecToCancel = codec;
            connectionToCancel = connection;
        }
        if (codecToCancel != null) {
            codecToCancel.cancel();
        } else if (connectionToCancel != null) {
            connectionToCancel.cancel();
        }
    }

    public void streamFailed(IOException e) {
        Socket socket;
        Connection releasedConnection;
        boolean noNewStreams = false;

        synchronized (connectionPool) {
            if (e instanceof StreamResetException) {
                ErrorCode errorCode = ((StreamResetException) e).errorCode;
                if (errorCode == ErrorCode.REFUSED_STREAM) {
                    // Retry REFUSED_STREAM errors once on the same connection.
                    refusedStreamCount++;
                    if (refusedStreamCount > 1) {
                        noNewStreams = true;
                        route = null;
                    }
                } else if (errorCode != ErrorCode.CANCEL) {
                    // Keep the connection for CANCEL errors. Everything else wants a fresh connection.
                    noNewStreams = true;
                    route = null;
                }
            } else if (connection != null
                    && (!connection.isMultiplexed() || e instanceof ConnectionShutdownException)) {
                noNewStreams = true;

                // If this route hasn't completed a call, avoid it for new connections.
                if (connection.successCount == 0) {
                    if (route != null && e != null) {
                        routeSelector.connectFailed(route, e);
                    }
                    route = null;
                }
            }
            releasedConnection = connection;
            socket = deallocate(noNewStreams, false, true);
            if (connection != null || !reportedAcquired) releasedConnection = null;
        }

        closeQuietly(socket);
        if (releasedConnection != null) {
            eventListener.connectionReleased(call, releasedConnection);
        }
    }

    /**
     * Use this allocation to hold {@code connection}. Each call to this must be paired with a call to
     * {@link #release} on the same connection.
     */
    public void acquire(RealConnection connection, boolean reportedAcquired) {
        assert (Thread.holdsLock(connectionPool));
        if (this.connection != null) throw new IllegalStateException();

        this.connection = connection; // 把连接池中获取到的可用连接赋值给成员变量
        this.reportedAcquired = reportedAcquired;
        // 根据allocations集合判断当前连接对象持有的StreamAllocation的数目，通过集合的大小来判定一个网络连接它的负载量是否超过最大值
        connection.allocations.add(new StreamAllocationReference(this, callStackTrace));
    }

    /**
     * Remove this allocation from the connection's list of allocations.
     */
    private void release(RealConnection connection) {
        for (int i = 0, size = connection.allocations.size(); i < size; i++) {
            Reference<StreamAllocation> reference = connection.allocations.get(i);
            if (reference.get() == this) {
                connection.allocations.remove(i);
                return;
            }
        }
        throw new IllegalStateException();
    }

    /**
     * Release the connection held by this connection and acquire {@code newConnection} instead. It is
     * only safe to call this if the held connection is newly connected but duplicated by {@code
     * newConnection}. Typically this occurs when concurrently connecting to an HTTP/2 webserver.
     *
     * <p>Returns a closeable that the caller should pass to {@link Util#closeQuietly} upon completion
     * of the synchronized block. (We don't do I/O while synchronized on the connection pool.)
     */
    public Socket releaseAndAcquire(RealConnection newConnection) {
        assert (Thread.holdsLock(connectionPool));
        if (codec != null || connection.allocations.size() != 1) throw new IllegalStateException();

        // Release the old connection.
        Reference<StreamAllocation> onlyAllocation = connection.allocations.get(0);
        Socket socket = deallocate(true, false, false);

        // Acquire the new connection.
        this.connection = newConnection;
        newConnection.allocations.add(onlyAllocation);

        return socket;
    }

    public boolean hasMoreRoutes() {
        return route != null
                || (routeSelection != null && routeSelection.hasNext())
                || routeSelector.hasNext();
    }

    @Override
    public String toString() {
        RealConnection connection = connection();
        return connection != null ? connection.toString() : address.toString();
    }

    public static final class StreamAllocationReference extends WeakReference<StreamAllocation> {
        /**
         * Captures the stack trace at the time the Call is executed or enqueued. This is helpful for
         * identifying the origin of connection leaks.
         */
        public final Object callStackTrace;

        StreamAllocationReference(StreamAllocation referent, Object callStackTrace) {
            super(referent);
            this.callStackTrace = callStackTrace;
        }
    }
}
