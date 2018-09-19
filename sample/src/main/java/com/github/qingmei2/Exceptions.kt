package com.github.qingmei2

import com.github.qingmei2.core.RxThrowable

open class CustomThrowable(val statusCode: Int,
                           val statusMessage: String) : RxThrowable()

class ConnectFailedAlertDialogException : CustomThrowable(-1, "Connect Failed")

class TokenExpiredException(entity: BaseEntity) : CustomThrowable(entity.statusCode, entity.message)