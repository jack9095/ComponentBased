package com.example.fly.componentbased;

public interface Test {

    // TODO jdk7以后接口中可以编写方法的实现，但是必须在方法前面设置一个关键字`default'
    default String setName() {
        return "测试";
    }

}
