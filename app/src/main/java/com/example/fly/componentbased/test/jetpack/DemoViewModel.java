package com.example.fly.componentbased.test.jetpack;

import android.arch.lifecycle.MutableLiveData;
import com.base.commonlib.AACBase.BaseViewModel;
import com.example.fly.componentbased.bean.User;

/**
 * 管理数据
 */
public class DemoViewModel extends BaseViewModel {

    // 获取 User 对象对应的 MutableLiveData
    MutableLiveData<User> getUserMutableLiveData() {
        return get(User.class);
    }

}
