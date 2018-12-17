package com.github.qingmei2.retry

import io.reactivex.Single

private const val DEFAULT_RETRY_TIMES = 1
private const val DEFAULT_DELAY_DURATION = 1000

@Suppress("DataClassPrivateConstructor")
data class RetryConfig private constructor(
        val maxRetries: Int,
        val delay: Int,
        val condition: () -> Single<Boolean>
) {
    companion object Extension {

        fun none(): RetryConfig = simpleInstance { Single.just(false) }

        fun simpleInstance(maxRetries: Int = DEFAULT_RETRY_TIMES,
                           delay: Int = DEFAULT_DELAY_DURATION,
                           condition: () -> Single<Boolean>): RetryConfig =
                RetryConfig(
                        maxRetries = maxRetries,
                        delay = delay,
                        condition = condition
                )
    }
}