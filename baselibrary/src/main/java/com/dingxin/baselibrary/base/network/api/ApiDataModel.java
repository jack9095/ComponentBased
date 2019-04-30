package com.dingxin.baselibrary.base.network.api;


import com.dingxin.baselibrary.base.datamodel.BaseDataModel;
import com.dingxin.baselibrary.base.network.http.HttpHelper;

import io.reactivex.disposables.CompositeDisposable;

public class ApiDataModel extends BaseDataModel {

    protected ApiService serviceApi;

    public ApiDataModel() {
        if (serviceApi == null) {
            serviceApi = HttpHelper.getInstance().create(ApiService.class);
        }
    }
}
