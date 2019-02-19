package com.base.commonlib.AACBase;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.base.commonlib.eventbus.EventCenter;

import org.greenrobot.eventbus.EventBus;

/**
 * BaseActivity基础类，处理ViewModelProvider的初始化
 */
public abstract class BaseActivity extends AppCompatActivity {
    private ViewModelProvider viewModelProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        viewModelProvider = getViewModelProvider();

        if (isBindEventBusHere()) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModelProvider = null;
        if (isBindEventBusHere()) {
            EventBus.getDefault().unregister(this);
        }
    }

    /**
     * 创建ViewModel对象
     *
     * @param clazz
     * @return
     */
    public <T extends ViewModel> T get(Class<T> clazz) {
        return viewModelProvider.get(clazz);
    }

    /**
     * 初始化ViewModelProvider对象
     *
     * @return
     */
    private ViewModelProvider getViewModelProvider() {
        return ViewModelProviders.of(this);
    }

    /**
     * 获取布局ID
     *
     * @return
     */
    protected abstract int getLayoutId();

    // 添加点击事件
    protected void addOnClickListeners(View.OnClickListener listener, @IdRes int... ids) {
        if (ids != null) {
            for (@IdRes int id : ids) {
                findViewById(id).setOnClickListener(listener);
            }
        }
    }

    /**
     * is bind eventBus
     *
     * @return
     */
    protected abstract boolean isBindEventBusHere();

    public void postEventBus(String type) {
        EventBus.getDefault().post(new EventCenter<Object>(type));
    }

    public void postEventBusSticky(String type) {
        EventBus.getDefault().postSticky(new EventCenter<Object>(type));
    }

    public void postEventBusSticky(String type, Object obj) {
        EventBus.getDefault().postSticky(new EventCenter<Object>(type, obj));
    }

    public void postEventBus(String type, Object obj) {
        EventBus.getDefault().post(new EventCenter<Object>(type, obj));
    }

    /**
     * 移动到position位置
     * @param mRecyclerView RecyclerView
     * @param position 位置对应的角标
     */
    protected void smoothMoveToPosition(RecyclerView mRecyclerView, final int position) {
        int firstItem = mRecyclerView.getChildLayoutPosition(mRecyclerView.getChildAt(0));
        int lastItem = mRecyclerView.getChildLayoutPosition(mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1));
        if (position < firstItem) {
            mRecyclerView.smoothScrollToPosition(position);
        } else if (position <= lastItem) {
            int movePosition = position - firstItem;
            if (movePosition >= 0 && movePosition < mRecyclerView.getChildCount()) {
                int top = mRecyclerView.getChildAt(movePosition).getTop();
                mRecyclerView.smoothScrollBy(0, top);
            }
        } else {
            mRecyclerView.smoothScrollToPosition(position);
        }
    }
}
