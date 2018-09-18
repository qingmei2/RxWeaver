package com.github.qingmei2.core

open class RxThrowable(
        val customStatusCode: Int,
        val customErrorMessage: String
) : Throwable() {

    companion object {
        val EMPTY = RxThrowable(0, "")
    }
}