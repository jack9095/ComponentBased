package com.dingxin.baselibrary.test;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import com.dingxin.baselibrary.R;
import com.dingxin.baselibrary.base.BaseLifecycleFragment;
import com.dingxin.baselibrary.test.bean.TestResponse;
import com.dingxin.baselibrary.test.viewmodel.TestViewModel;

/**
 * 测试
 */
public class TestFragment extends BaseLifecycleFragment<TestViewModel> {

    @Override
    protected void dataObserver() {
        mViewModel.getTestMutableLiveData().observe(this, new Observer<TestResponse>() {
            @Override
            public void onChanged(@Nullable TestResponse homeResponse) {
                assert homeResponse != null;
                showData(homeResponse);
            }
        });
    }

    // 展示网络返回的数据
    private void showData(TestResponse homeResponse) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_test;
    }

    @Override
    public void initView() {
        super.initView();
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        mViewModel.getRequestTestBannerData();
    }

    @Override
    protected boolean isBindEventBusHere() {
        return false;
    }

    @Override
    public void onClick(View view) {

    }
}
