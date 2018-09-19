package com.github.qingmei2.retry

import com.github.qingmei2.core.RxThrowable
import io.reactivex.Single

private const val DEFAULT_RETRY_TIMES = 1
private const val DEFAULT_DELAY_DURATION = 1000

data class RetryConfig(
        val maxRetries: Int = DEFAULT_RETRY_TIMES,
        val delay: Int = DEFAULT_DELAY_DURATION,
        val condition: () -> Single<Boolean> = { Single.just(false) }
)
