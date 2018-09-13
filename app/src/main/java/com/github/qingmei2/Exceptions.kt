package com.github.qingmei2

import com.github.qingmei2.core.ThrowableDelegate

class ConnectFailedAlertDialogException : ThrowableDelegate(1000, "Connect Failed")

class ReLoginAndRetryException(entity: BaseEntity) : ThrowableDelegate(entity.statusCode, entity.message)