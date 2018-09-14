package com.github.qingmei2.core

open class RxThrowable(
        private val customStatusCode: Int,
        private val customErrorMessage: String
) : Throwable() {

    companion object {
        val EMPTY = RxThrowable(0, "")
    }
}