package com.base.commonlib.AACBase.DataBinding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import com.base.commonlib.AACBase.BaseActivity;

/**
 * BaseActivity基础类，处理ViewDataBinding的初始化
 */
//public abstract class BaseDataBindActivity<V extends ViewDataBinding> extends BaseActivity {
//    protected V dataBind;
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        dataBind = DataBindingUtil.setContentView(this,getLayoutId());
//    }
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if(dataBind!=null){
//            dataBind.unbind();
//            dataBind = null;
//        }
//    }
//    /**
//     * 获取布局ID
//     * @return
//     */
//    protected abstract int getLayoutId();
//}