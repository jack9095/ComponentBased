package com.dingxin.baselibrary.test.datamodel;

import com.dingxin.baselibrary.base.network.CallBack;
import com.dingxin.baselibrary.base.network.api.ApiDataModel;
import com.dingxin.baselibrary.test.bean.TestResponse;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class TestDataModel extends ApiDataModel {

    public void getBannerAll(final CallBack callBack) {
        Observable<TestResponse> responseBeanObservable = serviceApi.requestWorkBannerData();
        addDisposable(
                responseBeanObservable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<TestResponse>() {
                            @Override
                            public void accept(TestResponse o) throws Exception {
                                callBack.onNext(o);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                callBack.onError(throwable.getMessage());
                            }
                        }));
    }
}
