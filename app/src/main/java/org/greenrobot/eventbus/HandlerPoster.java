package org.greenrobot.eventbus;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

/**
 *
 */
public class HandlerPoster extends Handler implements Poster {

    private final PendingPostQueue queue; // 存放待执行的 Post Events 的事件队列
    private final int maxMillisInsideHandleMessage; // post 事件在 handlerMessage 中执行的最大的时间值，超过这个时间值会抛出异常
    private final EventBus eventBus;
    private boolean handlerActive;  // 标识 handler 是否被运行起来了

    protected HandlerPoster(EventBus eventBus, Looper looper, int maxMillisInsideHandleMessage) {
        super(looper);
        this.eventBus = eventBus;
        this.maxMillisInsideHandleMessage = maxMillisInsideHandleMessage;
        queue = new PendingPostQueue();
    }

    public void enqueue(Subscription subscription, Object event) {
        // 从 pendingPostPool 的 ArrayList 缓存池中获取 PendingPost 添加到 PendingPostQueue 队列中，并将该 PendingPost 事件发送到 Handler 中处理
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            queue.enqueue(pendingPost); // 把 pendingPost 添加到队列中
            if (!handlerActive) {  // 标记 Handler 为活跃状态
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    throw new EventBusException("Could not send handler message");
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        boolean rescheduled = false;
        try {
            long started = SystemClock.uptimeMillis();
            while (true) { // 死循环，不断从 PendingPost 队列中取出 post 事件执行
                PendingPost pendingPost = queue.poll();
                if (pendingPost == null) { // 如果为 null，表示队列中没有 post 事件，此时标记 Handler 关闭，并退出 while 循环
                    synchronized (this) {
                        // Check again, this time in synchronized
                        pendingPost = queue.poll();
                        if (pendingPost == null) {
                            handlerActive = false; // 标记 Handler 为非活跃状态
                            return;
                        }
                    }
                }
                // 获取到 post 之后由 eventBus 通过该 post 查找相应的 Subscriber 处理事件
                eventBus.invokeSubscriber(pendingPost);

                // 计算每个事件在 handleMessage 中执行的时间
                long timeInMethod = SystemClock.uptimeMillis() - started;
                if (timeInMethod >= maxMillisInsideHandleMessage) { // 超过最大时间了
                    if (!sendMessage(obtainMessage())) {  // 消息没有发出去
                        throw new EventBusException("Could not send handler message"); // 抛出异常
                    }
                    rescheduled = true;
                    return;
                }
            }
        } finally {
            handlerActive = rescheduled;
        }
    }
}