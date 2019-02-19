package com.base.commonlib.AACBase.network;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.base.commonlib.utils.TUtil;

/**
 * jetPack新组件使用 ViewModel  LiveData
 * @param <T>
 */
public class NetWorkBaseViewModel<T extends BaseDataModel> extends AndroidViewModel {

    public MutableLiveData<String> loadState; // 网络加载状态的 LiveData

    public T mRepository;

    public NetWorkBaseViewModel(@NonNull Application application) {
        super(application);
        loadState = new MutableLiveData<>();
        mRepository = TUtil.getNewInstance(this, 0);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mRepository != null) {
            mRepository.unSubscribe();  // 清除所有订阅 释放内存 （Rx)）
        }
    }
}
