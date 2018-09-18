package com.github.qingmei2

import com.github.qingmei2.core.RxThrowable

class ConnectFailedAlertDialogException : RxThrowable(-1, "Connect Failed")

class TokenExpiredException(entity: BaseEntity) : RxThrowable(entity.statusCode, entity.message)