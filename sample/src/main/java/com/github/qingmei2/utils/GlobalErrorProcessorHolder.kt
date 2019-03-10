package com.github.qingmei2.utils

import androidx.fragment.app.FragmentActivity
import com.github.qingmei2.activity.widget.NavigatorFragment
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object GlobalErrorProcessorHolder {

    var isLoginBlocking: Boolean = false

    fun <T> tokenExpiredProcessor(currentActivity: FragmentActivity)
            : ObservableTransformer<T, T> =
            ObservableTransformer { obs ->
                synchronized(GlobalErrorProcessorHolder::class.java) {
                    when (isLoginBlocking) {
                        true -> obs
                                .flatMap {
                                    Observable.error<T>(TokenExpiredProcessResult.WaitLoginInQueue)
                                }
                                .delay(50, TimeUnit.MILLISECONDS)
                        false -> {
                            isLoginBlocking = true
                            NavigatorFragment
                                    .startLoginForResult(currentActivity)
                                    .doOnSuccess { isLoginBlocking = false }
                                    .observeOn(Schedulers.io())
                                    .toObservable()
                                    .flatMap { loginResult ->
                                        when (loginResult) {
                                            true -> Observable.error<T>(
                                                    TokenExpiredProcessResult.LoginSuccess(123)
                                            )
                                            false -> Observable.error<T>(
                                                    TokenExpiredProcessResult.LoginFailed
                                            )
                                        }
                                    }
                        }
                    }
                }
            }
}

sealed class TokenExpiredProcessResult : Exception() {

    data class LoginSuccess(val lastRefreshStamp: Long) : TokenExpiredProcessResult()

    object LoginFailed : TokenExpiredProcessResult()

    object WaitLoginInQueue : TokenExpiredProcessResult()
}