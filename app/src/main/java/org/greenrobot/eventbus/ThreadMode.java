package org.greenrobot.eventbus;

/**
 * Each subscriber method has a thread mode, which determines in which thread the method is to be called by EventBus.
 * EventBus takes care of threading independently from the posting thread.
 *
 * 每个订阅服务器方法都有一个线程模式，该模式决定EventBus在哪个线程中调用该方法。EventBus独立于发布线程来处理线程。
 * 
 * @see EventBus#register(Object)
 * @author Markus
 */
public enum ThreadMode {
    /**
     * Subscriber will be called directly in the same thread, which is posting the event. This is the default. Event delivery
     * implies the least overhead because it avoids thread switching completely. Thus this is the recommended mode for
     * simple tasks that are known to complete in a very short time without requiring the main thread. Event handlers
     * using this mode must return quickly to avoid blocking the posting thread, which may be the main thread.
     *
     * 订阅服务器将直接在发布事件的同一线程中调用。这是默认设置。事件传递意味着开销最小，因为它完全避免了线程切换。
     * 因此，对于已知在非常短的时间内完成而不需要主线程的简单任务，建议使用这种模式。
     * 使用此模式的事件处理程序必须快速返回，以避免阻塞可能是主线程的发布线程。
     */
    POSTING,  // EventBus 默认的线程模式 就是订阅的线程和发送事件的线程为同一线程

    /**
     * On Android, subscriber will be called in Android's main thread (UI thread). If the posting thread is
     * the main thread, subscriber methods will be called directly, blocking the posting thread. Otherwise the event
     * is queued for delivery (non-blocking). Subscribers using this mode must return quickly to avoid blocking the main thread.
     * If not on Android, behaves the same as {@link #POSTING}.
     *
     *
     * 在Android上，用户将在Android的主线程（UI线程）中被调用。
     * 如果发布线程是主线程，则将直接调用订阅方法，从而阻塞发布线程。否则，事件将排队等待传递（非阻塞）。
     * 使用此模式的订阅必须快速返回，以避免阻塞主线程。如果不在Android上，其行为与 POSTING 发布相同。
     * 因为安卓主线程阻塞会发生 ANR 异常
     */
    MAIN,  // 主线程

    /**
     * On Android, subscriber will be called in Android's main thread (UI thread). Different from {@link #MAIN},
     * the event will always be queued for delivery. This ensures that the post call is non-blocking.
     *
     * 在Android上，用户将在Android的主线程（UI线程）中被调用。与 MAIN 不同，事件将始终排队等待传递。这样可以确保后调用不阻塞
     */
    MAIN_ORDERED, // 主线程

    /**
     * On Android, subscriber will be called in a background thread. If posting thread is not the main thread, subscriber methods
     * will be called directly in the posting thread. If the posting thread is the main thread, EventBus uses a single
     * background thread, that will deliver all its events sequentially. Subscribers using this mode should try to
     * return quickly to avoid blocking the background thread. If not on Android, always uses a background thread.
     *
     * 在Android上，订阅将在后台线程中被调用。如果发布线程不是主线程，则将直接在发布线程中调用订阅方法。
     * 如果发布线程是主线程，那么 eventBus 使用单个后台线程，该线程将按顺序传递其所有事件。
     * 使用此模式的订阅应尝试快速返回，以避免阻塞后台线程。如果不在Android上，则始终使用后台线程。
     *
     */
    BACKGROUND,  // 后台线程

    /**
     * Subscriber will be called in a separate thread. This is always independent from the posting thread and the
     * main thread. Posting events never wait for subscriber methods using this mode. Subscriber methods should
     * use this mode if their execution might take some time, e.g. for network access. Avoid triggering a large number
     * of long running asynchronous subscriber methods at the same time to limit the number of concurrent threads. EventBus
     * uses a thread pool to efficiently reuse threads from completed asynchronous subscriber notifications.
     *
     * 将在单独的线程中调用订阅服务器。这始终独立于发布线程和主线程。
     * 发布事件从不等待使用此模式的订阅方法。
     * 如果订阅服务器方法的执行可能需要一些时间，例如用于网络访问，则应使用此模式。
     * 避免同时触发大量长时间运行的异步订阅服务器方法，以限制并发线程的数量。
     * EventBus使用线程池来有效地重用已完成的异步订阅服务器通知中的线程。
     *
     */
    ASYNC // 异步线程
}