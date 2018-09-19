package com.github.qingmei2.entity;

public class UserInfo extends BaseEntity {

    private String name;
    private int age;

    public UserInfo(int statusCode,
                    String message,
                    String name,
                    int age) {
        super(statusCode, message);
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", statusCode=" + getStatusCode() +
                ", message=" + getMessage() +
                '}';
    }
}
