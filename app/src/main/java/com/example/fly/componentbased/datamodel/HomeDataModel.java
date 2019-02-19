package com.example.fly.componentbased.datamodel;

import android.arch.lifecycle.MutableLiveData;
import com.example.fly.componentbased.api.ApiDataModel;
import com.example.fly.componentbased.bean.HomeResponse;

public class HomeDataModel extends ApiDataModel {

    private MutableLiveData<HomeResponse> homeMutableLiveData;  // 存储首页数据的 MutableLiveData

    public MutableLiveData<HomeResponse> getHomeMutableLiveData(){
        if (homeMutableLiveData == null) {
            homeMutableLiveData = new MutableLiveData<>();
        }
        return homeMutableLiveData;
    }

}
