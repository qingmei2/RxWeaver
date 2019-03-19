package com.github.qingmei2.utils

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.github.qingmei2.activity.login.NavigatorFragment
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

object GlobalErrorProcessorHolder {

    @Volatile
    var mLastRefreshTokenTimeStamp: Long = 0L
    @Volatile
    var mLastCancelRefreshTokenTimeStamp: Long = 0L
    @Volatile
    var mIsBlocking: Boolean = false

    fun tokenExpiredProcessor(
            currentActivity: FragmentActivity,
            currentWaitLoginInQueue: TokenExpiredProcessResult.WaitLoginInQueue
    ): Observable<Boolean> {
        val lastRefreshStamp = currentWaitLoginInQueue.lastRefreshStamp
        return Observable.defer {
            Log.d("Tag", "defer time: $lastRefreshStamp, isBlocking = $mIsBlocking")
            synchronized(GlobalErrorProcessorHolder::class.java) {
                when (mIsBlocking) {
                    true -> {
                        Log.d("Tag", "继续循环队列")
                        Observable.error<Boolean>(currentWaitLoginInQueue)
                    }
                    false -> {
                        Log.d("Tag", "处理单个消息")
                        mIsBlocking = true
                        return@synchronized when (mLastRefreshTokenTimeStamp > lastRefreshStamp) {
                            true -> {
                                Log.d("Tag", "单个消息处理，直接成功")
                                Observable
                                        .error<Boolean>(
                                                TokenExpiredProcessResult.LoginSuccess(lastRefreshStamp)
                                        )
                                        .doOnError { GlobalErrorProcessorHolder.mIsBlocking = false }
                            }
                            false -> {
                                when (mLastCancelRefreshTokenTimeStamp > lastRefreshStamp) {
                                    true -> {
                                        Log.d("Tag", "单个消息处理，直接失败")
                                        Observable
                                                .error<Boolean>(
                                                        TokenExpiredProcessResult.LoginFailed(lastRefreshStamp)
                                                )
                                                .doOnError { GlobalErrorProcessorHolder.mIsBlocking = false }
                                    }
                                    false -> {
                                        Log.d("Tag", "单个消息处理，进入登录")
                                        NavigatorFragment
                                                .startLoginForResult(currentActivity)
                                                .doOnSuccess {
                                                    when (it) {
                                                        true -> {
                                                            mLastRefreshTokenTimeStamp = System.currentTimeMillis()
                                                            Log.d("Tag", "登录成功，Token刷新= : $mLastRefreshTokenTimeStamp")
                                                        }
                                                        false -> {
                                                            mLastCancelRefreshTokenTimeStamp = System.currentTimeMillis()
                                                            Log.d("Tag", "登录失败，Cancel刷新= : $mLastRefreshTokenTimeStamp")
                                                        }
                                                    }
                                                }
                                                .observeOn(Schedulers.io())
                                                .toObservable()
                                                .flatMap { loginResult ->
                                                    when (loginResult) {
                                                        true -> {
                                                            Observable.error<Boolean>(
                                                                    TokenExpiredProcessResult.LoginSuccess(lastRefreshStamp)
                                                            )
                                                        }
                                                        false -> Observable.error<Boolean>(
                                                                TokenExpiredProcessResult.LoginFailed(lastRefreshStamp)
                                                        )
                                                    }
                                                }
                                                .doOnError {
                                                    GlobalErrorProcessorHolder.mIsBlocking = false
                                                    Log.d("Tag", "doOnError mIsBlocking = false")
                                                }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class TokenExpiredProcessResult : Exception() {

    data class LoginSuccess(val lastRefreshStamp: Long) : TokenExpiredProcessResult()

    data class LoginFailed(val lastRefreshStamp: Long) : TokenExpiredProcessResult()

    data class WaitLoginInQueue(val lastRefreshStamp: Long) : TokenExpiredProcessResult()
}