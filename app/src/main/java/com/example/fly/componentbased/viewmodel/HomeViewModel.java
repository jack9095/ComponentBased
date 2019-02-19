package com.example.fly.componentbased.viewmodel;

import android.app.Application;
import android.support.annotation.NonNull;
import com.base.commonlib.AACBase.network.NetWorkBaseViewModel;
import com.example.fly.componentbased.datamodel.HomeDataModel;

public class HomeViewModel extends NetWorkBaseViewModel<HomeDataModel> {

    public HomeViewModel(@NonNull Application application) {
        super(application);
    }
}
