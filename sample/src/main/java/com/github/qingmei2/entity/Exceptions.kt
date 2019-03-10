package com.github.qingmei2.entity

open class CustomException : Exception()

object ConnectFailedAlertDialogException : CustomException()

object TokenExpiredException : CustomException()