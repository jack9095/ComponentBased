package com.example.fly.componentbased.test.dagger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

// 自定义注解ActivityScope作用是限定被它标记的对象生命周期与对应的Activity相同
@Scope  // 标记局部单例
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityScope {
}
