package com.base;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.base.bean.User;
import com.base.dataBindingClick.MainClickHandlers;
import com.base.home.R;
import com.base.home.databinding.ActivityHomeBinding;

@Route(path = "/home/main")
public class HomeMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_home);

        // 通过下面的方式就完成了User和View的绑定
//        ActivityHomeBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
//        User user = new User("颜如玉", "女");
//        binding.setUser(user);

//        View view = binding.getRoot();//获取对应的View
//
//        binding.tvHello.setText("Hi,I'm from DataBinding.");
//
//        binding.setHandlers(new MainClickHandlers());

        // 也可以通过下述方式获取整个layout的View
//        ActivityHomeBinding binding = ActivityHomeBinding.inflate(getLayoutInflater());

        // 如果你是在ListView 或者RecycleView的Adapter中bind Item，你可以通过如下方式获取
//        ListItemBinding binding = ListItemBinding.inflate(layoutInflater, viewGroup, false);
        // 或者
//        ListItemBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.list_item, viewGroup, false);
    }
}
