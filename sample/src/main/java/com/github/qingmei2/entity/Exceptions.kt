package com.github.qingmei2.entity

sealed class Errors : Exception() {

    object ConnectFailedException : Errors()

    data class AuthorizationError(val timeStamp: Long) : Errors()
}