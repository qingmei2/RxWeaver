package com.github.qingmei2.retry

import io.reactivex.Flowable
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Function
import org.reactivestreams.Publisher
import java.util.concurrent.TimeUnit

class FlowableRetryDelay(
        val provider: (Throwable) -> RetryConfig
) : Function<Flowable<Throwable>, Publisher<*>> {

    private var retryCount: Int = 0

    @Throws(Exception::class)
    override fun apply(@NonNull throwableFlowable: Flowable<Throwable>): Publisher<*> {
        return throwableFlowable
                .flatMap(Function<Throwable, Publisher<*>> { throwable ->
                    val (maxRetries, delay, retryCondition) = provider(throwable)

                    if (!retryCondition)
                        return@Function Flowable.error<Any>(throwable)

                    if (++retryCount < maxRetries) {
                        Flowable.timer(delay.toLong(), TimeUnit.MILLISECONDS)
                    } else Flowable.error<Any>(throwable)
                })
    }
}
