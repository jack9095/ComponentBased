package com.example.fly.componentbased.bean;

public class User {
    private String name;
    private String pw;
    private String sex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", pw='" + pw + '\'' +
                ", sex='" + sex + '\'' +
                '}';
    }
}
