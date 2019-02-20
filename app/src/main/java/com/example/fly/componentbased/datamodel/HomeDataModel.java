package com.example.fly.componentbased.datamodel;

import com.example.fly.componentbased.api.ApiDataModel;
import com.example.fly.componentbased.api.CallBack;
import com.example.fly.componentbased.api.RxSchedulers;
import com.example.fly.componentbased.api.RxSubscriber;
import com.example.fly.componentbased.bean.HomeResponse;

import rx.Observable;

/**
 * 网络请求和数据库，持久层的操作
 */
public class HomeDataModel extends ApiDataModel {


    public void requestNetWorHomekData(CallBack listener){
        Observable<HomeResponse> homeResponseObservable = serviceApi.requestHomeData();
        addSubscribe(
                homeResponseObservable
                .compose(RxSchedulers.io_main())
                .subscribe(new RxSubscriber<Object>() {

                    @Override
                    protected void onNoNetWork() {
                        super.onNoNetWork();
                        listener.onNoNetWork();
                    }

                    @Override
                    public void onSuccess(Object o) {
                        listener.onNext(o);
                    }

                    @Override
                    public void onFailure(String msg) {
                        listener.onError(msg);
                    }
                }));
    }



}
