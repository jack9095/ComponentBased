package com.example.fly.componentbased.test.jetpack;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.base.commonlib.AACBase.BaseActivity;
import com.example.fly.componentbased.R;
import com.example.fly.componentbased.bean.User;

/**
 * ViewModel：以注重生命周期的方式管理界面相关的数据；
 * LiveData：在底层数据库更改时通知视图；
 * Room：流畅地访问 SQLite 数据库；
 * LifeCycles：管理您的 Activity 和 Fragment 生命周期；
 * DataBinding：以声明方式将可观察数据绑定到界面元素；
 * Navigation：处理应用内导航所需的一切；
 * Paging：逐步从您的数据源按需加载信息；
 * WorkManager：管理您的 Android 后台作业；
 */
@Route(path = "/test/target")
public class DemoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoViewModel demoViewModel = get(DemoViewModel.class);
        MutableLiveData<User> userMutableLiveData = demoViewModel.getUserMutableLiveData();
        // 1.添加数据更改监听器  监听数据的回调
        userMutableLiveData.observe(this, new Observer<User>() {
            @Override
            public void onChanged(@Nullable User user) {
                assert user != null;
                Log.e("DemoActivity = ", "DemoActivity中接收user：" + user.toString());
            }
        });
    }

    public void onClick(View view){
        getUser();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_demo;
    }

    @Override
    protected boolean isBindEventBusHere() {
        return false;
    }

    // 2.更改数据
    public void getUser() {
        User user = new User();
        user.setName("lin");
        user.setPw("123456");
        user.setSex("male");
        //同步更改setValue  ;  异步更改postValue
        DemoViewModel demoViewModel = get(DemoViewModel.class);
        MutableLiveData<User> userMutableLiveData = demoViewModel.getUserMutableLiveData();
//        userMutableLiveData.setValue(user);
        userMutableLiveData.postValue(user);  // 添加数据
    }
}
