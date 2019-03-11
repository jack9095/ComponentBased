package com.example.fly.componentbased.fragment;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.launcher.ARouter;
import com.base.commonlib.AACBase.network.BaseLifecycleFragment;
import com.base.commonlib.service.HomeExportService;
import com.example.fly.componentbased.R;
import com.example.fly.componentbased.bean.HomeResponse;
import com.example.fly.componentbased.viewmodel.HomeViewModel;

/**
 * 首页
 * https://www.jianshu.com/p/35d143e84d42
 */
public class HomeFragment extends BaseLifecycleFragment<HomeViewModel> implements View.OnClickListener {

    private TextView chat_tv;
    private TextView contract_tv;
    private TextView find_tv;
    private TextView mine_tv;
    private TextView say_hello_tv;

    @Autowired(name = "/home/HomeService")
    public HomeExportService baseService;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    protected void dataObserver() {
        mViewModel.getHomeMutableLiveData().observe(this, new Observer<HomeResponse>() {
            @Override
            public void onChanged(@Nullable HomeResponse homeResponse) {
                assert homeResponse != null;
                showData(homeResponse);
            }
        });
    }

    /**
     * 列表展示数据
     */
    private void showData(HomeResponse homeResponse) {
        Log.e("HomeFragment = ",homeResponse.toString());
    }

    @Override
    protected View initLayout(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.activity_target, null);
    }

    @Override
    public void initView() {
        super.initView();
        ARouter.getInstance().inject(this);

        chat_tv = view.findViewById(R.id.chat_tv);
        contract_tv = view.findViewById(R.id.contract_tv);
        find_tv = view.findViewById(R.id.find_tv);
        mine_tv = view.findViewById(R.id.mine_tv);
        say_hello_tv = view.findViewById(R.id.say_hello_tv);

        chat_tv.setOnClickListener(this);
        contract_tv.setOnClickListener(this);
        find_tv.setOnClickListener(this);
        mine_tv.setOnClickListener(this);
        say_hello_tv.setOnClickListener(this);
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        mViewModel.getRequestHomeData();
    }

    @Override
    protected boolean isBindEventBusHere() {
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chat_tv:
                ARouter.getInstance().build("/message/main")
                        .withLong("key1", 666L)
                        .withString("key3", "888")
                        .navigation();
                break;
            case R.id.contract_tv:
                ARouter.getInstance().build("/home/main")
                        .navigation();
                break;
            case R.id.find_tv:
                ARouter.getInstance().build("/video/main")
                        .navigation();
                break;
            case R.id.mine_tv:
                ARouter.getInstance().build("/live/main")
                        .navigation();
                break;
            case R.id.say_hello_tv:
//                Toast.makeText(this, baseService.sayHello("组件化测试使用"), Toast.LENGTH_SHORT).show();
                ARouter.getInstance().build("/test/target")
                        .navigation();
                break;
            default:
                break;
        }
    }
}