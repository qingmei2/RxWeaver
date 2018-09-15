package com.github.qingmei2.java.core;

public class RxThrowable extends Throwable {

    public static RxThrowable EMPTY = new RxThrowable(0, "");

    private int customStatusCode;
    private String customErrorMessage;

    public RxThrowable(int customStatusCode, String customErrorMessage) {
        this.customStatusCode = customStatusCode;
        this.customErrorMessage = customErrorMessage;
    }
}
