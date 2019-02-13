package org.greenrobot.eventbus;

final class PendingPostQueue {
    private PendingPost head; // 头
    private PendingPost tail; // 尾

    // 入队
    synchronized void enqueue(PendingPost pendingPost) {
        if (pendingPost == null) {
            throw new NullPointerException("null cannot be enqueued");
        }
        if (tail != null) { // 有尾
            tail.next = pendingPost;
            tail = pendingPost;
        } else if (head == null) { // 队列的头部和尾部都为空
            head = tail = pendingPost;  // 队列为空，添加第一个元素进队列
        } else {
            throw new IllegalStateException("Head present, but no tail"); // 有头无尾
        }
        notifyAll(); // 唤醒等候的全部线程
    }

    synchronized PendingPost poll() {
        PendingPost pendingPost = head;
        if (head != null) {
            head = head.next;
            if (head == null) {
                tail = null;
            }
        }
        return pendingPost;
    }

    synchronized PendingPost poll(int maxMillisToWait) throws InterruptedException {
        if (head == null) {
            wait(maxMillisToWait);
        }
        return poll();
    }

}