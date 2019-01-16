package com.base.commonlib.base.mvp_no_dagger;



public abstract class BaseFragmentModule<E> {

    private E e;

    public BaseFragmentModule(E e) {
        this.e = e;
    }

    public E providerView(E e) {
        return e;
    }
}
