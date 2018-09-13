package com.github.qingmei2.retry

import io.reactivex.functions.Function

internal interface RetryFuction<T, R> : Function<T, R>