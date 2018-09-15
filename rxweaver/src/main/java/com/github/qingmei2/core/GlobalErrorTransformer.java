package com.github.qingmei2.core;

import com.github.qingmei2.retry.FlowableRetryDelay;
import com.github.qingmei2.retry.ObservableRetryDelay;
import com.github.qingmei2.retry.RetryConfig;

import org.reactivestreams.Publisher;

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

public class GlobalErrorTransformer<T> implements ObservableTransformer<T, T>,
        FlowableTransformer<T, T>,
        SingleTransformer<T, T>,
        MaybeTransformer<T, T>,
        CompletableTransformer {

    private Suppiler<Scheduler> upStreamSchedulerProvider;
    private Suppiler<Scheduler> downStreamSchedlerProvider;
    private Function<T, Single<RxThrowable>> globalOnNextInterceptor;
    private Function<Throwable, Single<RxThrowable>> globalOnErrorResumeTransformer;
    private Function<Throwable, RetryConfig> retryConfigProvider;
    private Consumer<Throwable> globalDoOnErrorConsumer;

    public GlobalErrorTransformer(Function<T, Single<RxThrowable>> globalOnNextInterceptor,
                                  Function<Throwable, Single<RxThrowable>> globalOnErrorResumeTransformer,
                                  Function<Throwable, RetryConfig> retryConfigProvider,
                                  Consumer<Throwable> globalDoOnErrorConsumer) {
        this(
                AndroidSchedulers::mainThread,
                AndroidSchedulers::mainThread,
                globalOnNextInterceptor,
                globalOnErrorResumeTransformer,
                retryConfigProvider,
                globalDoOnErrorConsumer
        );
    }

    public GlobalErrorTransformer(Suppiler<Scheduler> upStreamSchedulerProvider,
                                  Suppiler<Scheduler> downStreamSchedlerProvider,
                                  Function<T, Single<RxThrowable>> globalOnNextInterceptor,
                                  Function<Throwable, Single<RxThrowable>> globalOnErrorResumeTransformer,
                                  Function<Throwable, RetryConfig> retryConfigProvider,
                                  Consumer<Throwable> globalDoOnErrorConsumer) {
        this.upStreamSchedulerProvider = upStreamSchedulerProvider;
        this.downStreamSchedlerProvider = downStreamSchedlerProvider;
        this.globalOnNextInterceptor = globalOnNextInterceptor;
        this.globalOnErrorResumeTransformer = globalOnErrorResumeTransformer;
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
                        return globalOnNextInterceptor.apply(t)
                                .flatMapObservable(new Function<Throwable, ObservableSource<? extends T>>() {
                                    @Override
                                    public ObservableSource<? extends T> apply(Throwable throwable) throws Exception {
                                        return (throwable != RxThrowable.EMPTY) ? Observable.<T>error(throwable) : Observable.just(t);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends T>>() {
                    @Override
                    public ObservableSource<? extends T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer.apply(throwable)
                                .flatMapObservable(new Function<Throwable, ObservableSource<? extends T>>() {
                                    @Override
                                    public ObservableSource<? extends T> apply(Throwable rxerror) throws Exception {
                                        return Observable.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new ObservableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider.call());
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .onErrorResumeNext(new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer.apply(throwable)
                                .flatMapCompletable(new Function<Throwable, CompletableSource>() {
                                    @Override
                                    public CompletableSource apply(Throwable rxerror) throws Exception {
                                        return Completable.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider.call());
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .flatMap(new Function<T, Publisher<T>>() {
                    @Override
                    public Publisher<T> apply(final T t) throws Exception {
                        return globalOnNextInterceptor.apply(t)
                                .flatMapPublisher(new Function<Throwable, Publisher<? extends T>>() {
                                    @Override
                                    public Publisher<? extends T> apply(Throwable throwable) throws Exception {
                                        if (throwable != RxThrowable.EMPTY) {
                                            return Flowable.error(throwable);
                                        }
                                        return Flowable.just(t);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, Publisher<T>>() {
                    @Override
                    public Publisher<T> apply(Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer.apply(throwable)
                                .flatMapPublisher(new Function<Throwable, Publisher<T>>() {
                                    @Override
                                    public Publisher<T> apply(final Throwable throwable) throws Exception {
                                        return Flowable.error(throwable != RxThrowable.EMPTY ? throwable : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider.call());
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .flatMap(new Function<T, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(final T t) throws Exception {
                        return globalOnNextInterceptor.apply(t)
                                .flatMapMaybe(new Function<Throwable, MaybeSource<? extends T>>() {
                                    @Override
                                    public MaybeSource<T> apply(Throwable throwable) throws Exception {
                                        if (throwable != RxThrowable.EMPTY) {
                                            return Maybe.error(throwable);
                                        }
                                        return Maybe.just(t);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer.apply(throwable)
                                .flatMapMaybe(new Function<Throwable, MaybeSource<? extends T>>() {
                                    @Override
                                    public MaybeSource<? extends T> apply(Throwable rxerror) throws Exception {
                                        return Maybe.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider.call());
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return upstream
                .observeOn(upStreamSchedulerProvider.call())
                .flatMap(new Function<T, SingleSource<T>>() {
                    @Override
                    public SingleSource<T> apply(final T t) throws Exception {
                        return globalOnNextInterceptor.apply(t)
                                .flatMap(new Function<Throwable, SingleSource<? extends T>>() {
                                    @Override
                                    public SingleSource<? extends T> apply(Throwable throwable) throws Exception {
                                        return (throwable != RxThrowable.EMPTY) ? Single.<T>error(throwable) : Single.just(t);
                                    }
                                });
                    }
                })
                .onErrorResumeNext(new Function<Throwable, SingleSource<T>>() {
                    @Override
                    public SingleSource<T> apply(final Throwable throwable) throws Exception {
                        return globalOnErrorResumeTransformer.apply(throwable)
                                .flatMap(new Function<Throwable, SingleSource<? extends T>>() {
                                    @Override
                                    public SingleSource<? extends T> apply(Throwable rxerror) throws Exception {
                                        return Single.error(rxerror != RxThrowable.EMPTY ? rxerror : throwable);
                                    }
                                });
                    }
                })
                .retryWhen(new FlowableRetryDelay(retryConfigProvider))
                .doOnError(globalDoOnErrorConsumer)
                .observeOn(downStreamSchedlerProvider.call());
    }
}
