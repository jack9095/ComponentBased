package com.example.fly.componentbased.test.dagger;

import android.content.Context;
import android.widget.Toast;
import com.example.fly.componentbased.bean.User;
import javax.inject.Inject;

public class LoginPresenter {
    ICommonView iView;

    @Inject
    public LoginPresenter(ICommonView iView) {
        this.iView = iView;
    }

    public void login(User user) {
        Context mContext = iView.getContext();
        Toast.makeText(mContext, "login......", Toast.LENGTH_SHORT).show();
    }
}