package com.github.qingmei2.core

import com.github.qingmei2.retry.FlowableRetryDelay
import com.github.qingmei2.retry.ObservableRetryDelay
import com.github.qingmei2.retry.RetryConfig
import io.reactivex.*

class GlobalErrorTransformer<T> constructor(
        private val globalOnNextInterceptor: (T) -> Observable<T> = { Observable.just(it) },
        private val globalOnErrorResume: (Throwable) -> Observable<T> = { Observable.error(it) },
        private val retryConfigProvider: (Throwable) -> RetryConfig = { RetryConfig() },
        private val globalDoOnErrorConsumer: (Throwable) -> Unit = { }
) : ObservableTransformer<T, T>,
        FlowableTransformer<T, T>,
        SingleTransformer<T, T>,
        MaybeTransformer<T, T>,
        CompletableTransformer {

    override fun apply(upstream: Observable<T>): Observable<T> =
            upstream
                    .flatMap {
                        globalOnNextInterceptor(it)
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResume(throwable)
                    }
                    .retryWhen(ObservableRetryDelay(retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)

    override fun apply(upstream: Completable): Completable =
            upstream
                    .onErrorResumeNext {
                        globalOnErrorResume(it).ignoreElements()
                    }
                    .retryWhen(FlowableRetryDelay(retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)

    override fun apply(upstream: Flowable<T>): Flowable<T> =
            upstream
                    .flatMap {
                        globalOnNextInterceptor(it)
                                .toFlowable(BackpressureStrategy.BUFFER)
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResume(throwable)
                                .toFlowable(BackpressureStrategy.BUFFER)
                    }
                    .retryWhen(FlowableRetryDelay(retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)

    override fun apply(upstream: Maybe<T>): Maybe<T> =
            upstream
                    .flatMap {
                        globalOnNextInterceptor(it)
                                .firstElement()
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResume(throwable)
                                .firstElement()
                    }
                    .retryWhen(FlowableRetryDelay(retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)

    override fun apply(upstream: Single<T>): Single<T> =
            upstream
                    .flatMap {
                        globalOnNextInterceptor(it)
                                .firstOrError()
                    }
                    .onErrorResumeNext { throwable ->
                        globalOnErrorResume(throwable)
                                .firstOrError()
                    }
                    .retryWhen(FlowableRetryDelay(retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)
}