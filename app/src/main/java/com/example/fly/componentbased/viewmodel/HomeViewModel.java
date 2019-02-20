package com.example.fly.componentbased.viewmodel;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.base.commonlib.AACBase.network.NetWorkBaseViewModel;
import com.base.commonlib.constant.StateConstants;
import com.example.fly.componentbased.api.CallBack;
import com.example.fly.componentbased.bean.HomeResponse;
import com.example.fly.componentbased.datamodel.HomeDataModel;

/**
 * 业务逻辑处理
 */
public class HomeViewModel extends NetWorkBaseViewModel<HomeDataModel> {

    public HomeViewModel(@NonNull Application application) {
        super(application);
    }

    private MutableLiveData<HomeResponse> homeMutableLiveData;  // 存储首页数据的 MutableLiveData

    public MutableLiveData<HomeResponse> getHomeMutableLiveData(){
        if (homeMutableLiveData == null) {
            homeMutableLiveData = new MutableLiveData<>();
        }
        return homeMutableLiveData;
    }

    // 发起网络请求
    public void getRequestHomeData() {

        mDataModel.requestNetWorHomekData(new CallBack<Object>() {
            @Override
            public void onNoNetWork() {
                Log.e("HomeViewModel 错误 = ", "网络异常");
                loadState.postValue(StateConstants.NET_WORK_STATE);
            }

            @Override
            public void onNext(Object object) {
                if (object instanceof HomeResponse) {
                    HomeResponse homeResponse = (HomeResponse) object;
                    homeMutableLiveData.postValue(homeResponse);
                    loadState.postValue(StateConstants.SUCCESS_STATE);
                }
            }

            @Override
            public void onError(String e) {
                Log.e("HomeViewModel 错误 = ", e);
            }
        });
    }
}
