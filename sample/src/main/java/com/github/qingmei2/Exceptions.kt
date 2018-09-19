package com.github.qingmei2

import java.lang.Exception

open class CustomException : Exception()

class ConnectFailedAlertDialogException : CustomException()

class TokenExpiredException : CustomException()