package com.github.qingmei2.core

import com.github.qingmei2.retry.FlowableRetryDelay
import com.github.qingmei2.retry.ObservableRetryDelay
import com.github.qingmei2.retry.RetryConfig
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers

class GlobalErrorTransformer<T> constructor(
        private val globalOnNextRetryInterceptor: (T) -> Observable<T> = { Observable.just(it) },
        private val globalOnErrorResume: (Throwable) -> Observable<T> = { Observable.error(it) },
        private val retryErrorTransformer: (RxThrowable) -> Single<Boolean> = { Single.just(false) },
        private val retryConfigProvider: (RxThrowable) -> RetryConfig = { RetryConfig() },
        private val globalDoOnErrorConsumer: (Throwable) -> Unit = { },
        private val upStreamSchedulerProvider: () -> Scheduler = { AndroidSchedulers.mainThread() },
        private val downStreamSchedulerProvider: () -> Scheduler = { AndroidSchedulers.mainThread() }
) : ObservableTransformer<T, T>,
        FlowableTransformer<T, T>,
        SingleTransformer<T, T>,
        MaybeTransformer<T, T>,
        CompletableTransformer {

    override fun apply(upstream: Observable<T>): Observable<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextRetryInterceptor(it)
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResume(throwable)
                    }
                    .retryWhen(ObservableRetryDelay(retryErrorTransformer, retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Completable): Completable =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .onErrorResumeNext {
                        globalOnErrorResume(it).ignoreElements()
                    }
                    .retryWhen(FlowableRetryDelay(retryErrorTransformer, retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Flowable<T>): Flowable<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextRetryInterceptor(it)
                                .toFlowable(BackpressureStrategy.BUFFER)
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResume(throwable)
                                .toFlowable(BackpressureStrategy.BUFFER)
                    }
                    .retryWhen(FlowableRetryDelay(retryErrorTransformer, retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Maybe<T>): Maybe<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextRetryInterceptor(it)
                                .firstElement()
                    }
                    .onErrorResumeNext { throwable: Throwable ->
                        globalOnErrorResume(throwable)
                                .firstElement()
                    }
                    .retryWhen(FlowableRetryDelay(retryErrorTransformer, retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())

    override fun apply(upstream: Single<T>): Single<T> =
            upstream
                    .observeOn(upStreamSchedulerProvider())
                    .flatMap {
                        globalOnNextRetryInterceptor(it)
                                .firstOrError()
                    }
                    .onErrorResumeNext { throwable ->
                        globalOnErrorResume(throwable)
                                .firstOrError()
                    }
                    .retryWhen(FlowableRetryDelay(retryErrorTransformer, retryConfigProvider))
                    .doOnError(globalDoOnErrorConsumer)
                    .observeOn(downStreamSchedulerProvider())
}
