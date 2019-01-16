package com.base.commonlib.di.module;

import com.base.commonlib.di.scope.ActivityScope;
import dagger.Module;
import dagger.Provides;


@Module
public abstract class BaseActivityModule<E> {

    private E view;

    public BaseActivityModule(E view) {
        this.view = view;
    }

    @ActivityScope
    @Provides
    E providerModel() {
        return view;
    }

}
