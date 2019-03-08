package com.github.qingmei2.entity

import java.lang.Exception

open class CustomException : Exception()

class ConnectFailedAlertDialogException : CustomException()

class TokenExpiredException : CustomException()