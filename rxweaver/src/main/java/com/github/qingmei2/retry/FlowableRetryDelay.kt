package com.github.qingmei2.retry

import io.reactivex.Flowable
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Function
import org.reactivestreams.Publisher
import java.util.concurrent.TimeUnit

class FlowableRetryDelay(
        @NonNull retryConfig: RetryConfig
) : Function<Flowable<Throwable>, Publisher<*>> {

    private val maxRetries: Int
    private val delay: Int
    private var retryCount: Int = 0

    private val condition: (Throwable) -> Boolean

    init {
        this.maxRetries = retryConfig.maxRetries
        this.delay = retryConfig.delay
        this.condition = retryConfig.condition
    }

    @Throws(Exception::class)
    override fun apply(@NonNull throwableFlowable: Flowable<Throwable>): Publisher<*> {
        return throwableFlowable
                .flatMap(Function<Throwable, Publisher<*>> { throwable ->
                    if (!condition(throwable))
                        return@Function Flowable.error<Any>(throwable)

                    if (++retryCount <= maxRetries) {
                        Flowable.timer(delay.toLong(), TimeUnit.MILLISECONDS)
                    } else Flowable.error<Any>(throwable)
                })
    }
}
