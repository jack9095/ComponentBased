package com.base.commonlib.AACBase.network;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import com.base.commonlib.AACBase.BaseFragment;
import com.base.commonlib.constant.StateConstants;
import com.base.commonlib.utils.TUtil;

public abstract class BaseLifecycleFragment<T extends NetWorkBaseViewModel> extends BaseFragment {

    protected T mViewModel;

    @Override
    public void initView() {
        mViewModel = createViewModel(this, (Class<T>) TUtil.getInstance(this, 0));
        if (null != mViewModel) {
            MutableLiveData loadState = mViewModel.loadState;
            loadState.observe(this, observer);
            dataObserver();
        }
    }

    /**
     * 创建 自实现的 ViewModel
     */
    protected <T extends ViewModel> T createViewModel(Fragment fragment, @NonNull Class<T> modelClass) {
        ViewModelProvider viewModelProvider = ViewModelProviders.of(fragment);
        return viewModelProvider.get(modelClass);
    }

    protected void dataObserver() {

    }

    /**
     * 获取网络数据
     */
    protected void getRemoteData() {

    }

    // lifecycle 中 liveData的监听者
    protected Observer<String> observer = new Observer<String>() {
        @Override
        public void onChanged(@Nullable String state) {
            if (!TextUtils.isEmpty(state)) {
                if (StateConstants.ERROR_STATE.equals(state)) {
                    showToast("加载错误");
                } else if (StateConstants.NET_WORK_STATE.equals(state)) {
                    showToast("网络不好，请稍后重试");
                } else if (StateConstants.LOADING_STATE.equals(state)) {
                    showToast("加载中");
                } else if (StateConstants.SUCCESS_STATE.equals(state)) {
                    showToast("加载成功");
                }
            }
        }
    };

}