package com.example.fly.componentbased.api;

import com.base.commonlib.AACBase.network.BaseDataModel;

public class ApiDataModel extends BaseDataModel {

    protected ServiceApi serviceApi;

    public ApiDataModel() {
        if (serviceApi == null) {
            serviceApi = HttpHelper.getInstance().create(ServiceApi.class);
        }
    }
}
