package org.greenrobot.eventbus;

import java.util.logging.Level;

/**
 * Posts events in background.
 * 在后台发布事件
 * @author Markus
 */
final class BackgroundPoster implements Runnable, Poster {

    private final PendingPostQueue queue;
    private final EventBus eventBus;

    private volatile boolean executorRunning;

    BackgroundPoster(EventBus eventBus) {
        this.eventBus = eventBus;
        queue = new PendingPostQueue();
    }

    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            queue.enqueue(pendingPost); // 添加到队列
            if (!executorRunning) {
                executorRunning = true;

                // 在线程池中执行这个 pendingPost
                eventBus.getExecutorService().execute(this);
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                // 不断循环从 PendingPostQueue 取出 pendingPost 到 eventBus 执行
                while (true) {

                    // 在 1000 毫秒内从 PendingPostQueue 中获取 pendingPost
                    PendingPost pendingPost = queue.poll(1000);

                    // 双重校验锁判断 pendingPost 是否为 null
                    if (pendingPost == null) {
                        synchronized (this) {
                            // Check again, this time in synchronized
                            pendingPost = queue.poll(); // 再次尝试获取 pendingPost
                            if (pendingPost == null) {
                                executorRunning = false;
                                return;
                            }
                        }
                    }

                    // 将 pendingPost 通过 EventBus 分发出去
                    // 这里会将 PendingPostQueue 中【所有】的 pendingPost 都会分发，这里区别于 AsyncPoster
                    eventBus.invokeSubscriber(pendingPost);
                }
            } catch (InterruptedException e) {
                eventBus.getLogger().log(Level.WARNING, Thread.currentThread().getName() + " was interruppted", e);
            }
        } finally {
            executorRunning = false;
        }
    }

}
