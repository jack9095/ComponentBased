package com.base.commonlib.base.mvp_no_dagger;

import android.app.Activity;
import android.support.v4.app.Fragment;



public class BasePresenter<T> {

    private T IView;
    private BaseApplication app;

    public BasePresenter(T IView) {
        this.IView = IView;
        try {
            this.app = (BaseApplication) ((Activity) IView).getApplication();
        } catch (ClassCastException ignored) {
            this.app = (BaseApplication) (((Fragment) IView).getActivity()).getApplication();
        }
    }

    public T getIView() {
        return IView;
    }

    public BaseApplication getApp() {
        return app;
    }

}
