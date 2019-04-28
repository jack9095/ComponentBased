package com.base.commonlib.AACBase.network;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * 在生命周期的某个时刻取消订阅。
 * 一个很常见的模式就是使用CompositeSubscription来持有所有的Subscriptions，然后在onDestroy()或者onDestroyView()里取消所有的订阅
 */
public abstract class BaseDataModel {

    // 可以缓解Rx内存占用不能释放的问题
    private CompositeSubscription mCompositeSubscription;

    // 添加订阅
    protected void addSubscribe(Subscription subscription) {
        if (mCompositeSubscription == null) {
            mCompositeSubscription = new CompositeSubscription();
        }
        mCompositeSubscription.add(subscription);
    }

    // 移除订阅
    void unSubscribe() {
        if (mCompositeSubscription != null && mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.clear();
        }
    }
}
