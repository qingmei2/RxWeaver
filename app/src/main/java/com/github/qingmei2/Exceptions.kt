package com.github.qingmei2

import com.github.qingmei2.core.RxThrowable

class ConnectFailedAlertDialogException : RxThrowable(-1, "Connect Failed")

class ReLoginSuccessAndRetryException(entity: BaseEntity) : RxThrowable(entity.statusCode, entity.message)

class ReLoginFailedException(entity: BaseEntity) : RxThrowable(entity.statusCode, entity.message)