package com.dingxin.baselibrary.test.viewmodel;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.dingxin.baselibrary.base.constant.StateConstants;
import com.dingxin.baselibrary.base.network.CallBack;
import com.dingxin.baselibrary.base.viewmodel.NetWorkBaseViewModel;
import com.dingxin.baselibrary.test.bean.TestResponse;
import com.dingxin.baselibrary.test.datamodel.TestDataModel;
import com.dingxin.baselibrary.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务逻辑处理
 */
public class TestViewModel extends NetWorkBaseViewModel<TestDataModel> {

    private static final String TAG = TestViewModel.class.getSimpleName();

    public TestViewModel(@NonNull Application application) {
        super(application);
    }

    private MutableLiveData<TestResponse> TestMutableLiveData;  // 存储首页数据的 MutableLiveData

    public MutableLiveData<TestResponse> getTestMutableLiveData(){
        if (TestMutableLiveData == null) {
            TestMutableLiveData = new MutableLiveData<>();
        }
        return TestMutableLiveData;
    }

    // 发起网络请求
    public void getRequestTestBannerData() {

        mDataModel.getBannerAll(new CallBack<TestResponse>() {

            @Override
            public void onNoNetWork() {
                LogUtil.e(TAG, "网络异常");
                loadState.postValue(StateConstants.NET_WORK_STATE);
            }

            @Override
            public void onNext(TestResponse homeResponse) {
                TestMutableLiveData.postValue(homeResponse);
                loadState.postValue(StateConstants.SUCCESS_STATE);
            }

            @Override
            public void onError(String e) {
                LogUtil.e(TAG, e);
            }
        });
    }

}
