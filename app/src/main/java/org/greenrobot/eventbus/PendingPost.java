package org.greenrobot.eventbus;

import java.util.ArrayList;
import java.util.List;

final class PendingPost {
    // 通过ArrayList来实现PendingPost的添加和删除
    private final static List<PendingPost> pendingPostPool = new ArrayList<PendingPost>();

    Object event;
    Subscription subscription;
    PendingPost next;

    private PendingPost(Object event, Subscription subscription) {
        this.event = event;
        this.subscription = subscription;
    }

    // 获取 PendingPost
    static PendingPost obtainPendingPost(Subscription subscription, Object event) {
        synchronized (pendingPostPool) {
            int size = pendingPostPool.size();
            if (size > 0) {
                PendingPost pendingPost = pendingPostPool.remove(size - 1); // 返回一个PendingPost，就把该对象从缓存池中移除出去
                pendingPost.event = event;
                pendingPost.subscription = subscription;
                pendingPost.next = null;
                return pendingPost;
            }
        }
        return new PendingPost(event, subscription);  // pendingPostPool缓存池中没有了就新创建一个
    }

    // 释放 PendingPost
    static void releasePendingPost(PendingPost pendingPost) {
        pendingPost.event = null;
        pendingPost.subscription = null;
        pendingPost.next = null;
        synchronized (pendingPostPool) {
            // Don't let the pool grow indefinitely
            if (pendingPostPool.size() < 10000) {  // 缓存池中最多存放一万条数据
                pendingPostPool.add(pendingPost);
            }
        }
    }

}