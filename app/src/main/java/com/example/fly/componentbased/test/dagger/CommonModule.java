package com.example.fly.componentbased.test.dagger;

import dagger.Module;
import dagger.Provides;

@Module
public class CommonModule{

    private ICommonView iView;
    public CommonModule(ICommonView iView){
        this.iView = iView;
    }


    @Provides
    @ActivityScope
    public ICommonView provideIcommonView(){
        return this.iView;
    }

}