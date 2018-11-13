package com.github.qingmei2.core;

import com.github.qingmei2.func.Suppiler;
import com.github.qingmei2.retry.FlowableRetryDelay;
import com.github.qingmei2.retry.ObservableRetryDelay;
import com.github.qingmei2.retry.RetryConfig;

import org.reactivestreams.Publisher;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.MaybeTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

@SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
public class GlobalErrorTransformer<T> implements ObservableTransformer<T, T>,
        FlowableTransformer<T, T>,
        SingleTransformer<T, T>,
        MaybeTransformer<T, T>,
        CompletableTransformer {

    private static Suppiler<Scheduler> SCHEDULER_PROVIDER_DEFAULT = new Suppiler<Scheduler>() {
        @Override
        public Scheduler call() {
            return AndroidSchedulers.mainThread();
        }
    };

    private Suppiler<Scheduler> upStreamSchedulerProvider;
    private Suppiler<Scheduler> downStreamSchedulerProvider;

    private Function<T, Observable<T>> globalOnNextRetryInterceptor;
    private Function<Throwable, Observable<T>> globalOnErrorResume;
    private Function<Throwable, RetryConfig> retryConfigProvider;
    private Consumer<Throwable> globalDoOnErrorConsumer;

    public GlobalErrorTransformer(Function<T, Observable<T>> globalOnNextRetryInterceptor,
                                  Function<Throwable, Observable<T>> globalOnErrorResume,
                                  Function<Throwable, RetryConfig> retryConfigProvider,
                                  Consumer<Throwable> globalDoOnErrorConsumer) {
        this(
                SCHEDULER_PROVIDER_DEFAULT,
                SCHEDULER_PROVIDER_DEFAULT,
                globalOnNextRetryInterceptor,
                globalOnErrorResume,
                retryConfigProvider,
                globalDoOnErrorConsumer
        );
    }

    public GlobalErrorTransformer(Suppiler<Scheduler> upStreamSchedulerProvider,
                                  Suppiler<Scheduler> downStreamSchedulerProvider,
                                  Function<T, Observable<T>> globalOnNextRetryInterceptor,
                                  Function<Throwable, Observable<T>> globalOnErrorResume,
                                  Function<Throwable, RetryConfig> retryConfigProvider,
                                  Consumer<Throwable> globalDoOnErrorConsumer) {
        this.upStreamSchedulerProvider = upStreamSchedulerProvider;
        this.downStreamSchedulerProvider = downStreamSchedulerProvider;
        this.globalOnNextRetryInterceptor = globalOnNextRetryInterceptor;
        this.globalOnErrorResume = globalOnErrorResume;
        this.retryConfigProvider = retryConfigProvider;
        this.globalDoOnErrorConsumer = globalDoOnErrorConsumer;
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .flatMap(new Function<T, ObservableSource<T>>() {
                    @Override
                    public ObservableSource<T> apply(final T t) throws Exception {
                        return globalOnNextRetryInterceptor.apply(t);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends T>>() {
                    @Override
                    public ObservableSource<? extends T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResume.apply(throwable);
                    }
                })
                .retryWhen(new ObservableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedulerProvider.call());
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .onErrorResumeNext(new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResume.apply(throwable)
                                .ignoreElements();
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedulerProvider.call());
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .flatMap(new Function<T, Publisher<T>>() {
                    @Override
                    public Publisher<T> apply(final T t) throws Exception {
                        return globalOnNextRetryInterceptor.apply(t)
                                .toFlowable(BackpressureStrategy.BUFFER);
                    }
                })
                .onErrorResumeNext(new Function<Throwable, Publisher<T>>() {
                    @Override
                    public Publisher<T> apply(Throwable throwable) throws Exception {
                        return globalOnErrorResume.apply(throwable)
                                .toFlowable(BackpressureStrategy.BUFFER);
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedulerProvider.call());
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .flatMap(new Function<T, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(final T t) throws Exception {
                        return globalOnNextRetryInterceptor.apply(t)
                                .firstElement();
                    }
                })
                .onErrorResumeNext(new Function<Throwable, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResume.apply(throwable)
                                .firstElement();
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedulerProvider.call());
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .flatMap(new Function<T, SingleSource<T>>() {
                    @Override
                    public SingleSource<T> apply(final T t) throws Exception {
                        return globalOnNextRetryInterceptor.apply(t)
                                .firstOrError();
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<T>>() {
                    @Override
                    public SingleSource<T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResume.apply(throwable)
                                .firstOrError();
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedulerProvider.call());
    }
}
