package com.base.bean;

import java.io.Serializable;

public class User implements Serializable {

    public final String name;
    public final String sex;

    public User(String name, String sex) {
        this.name = name;
        this.sex = sex;
    }
}
