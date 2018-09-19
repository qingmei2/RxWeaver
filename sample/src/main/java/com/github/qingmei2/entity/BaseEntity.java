package com.github.qingmei2.entity;

public class BaseEntity {

    private int statusCode;
    private String message;

    public BaseEntity(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}