@file:Suppress("UNREACHABLE_CODE")

package com.github.qingmei2

import android.support.v4.app.FragmentActivity
import com.github.qingmei2.core.ThrowableDelegate
import com.github.qingmei2.core.WeaverTransformer
import com.github.qingmei2.retry.RetryConfig
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.net.ConnectException

object WeaverHelper {

    /**
     * Status code
     */
    private const val STATUS_OK = 200
    private const val STATUS_UNAUTHORIZED = 401
    private const val FORBIDDEN = 403
    private const val NOT_FOUND = 404
    private const val REQUEST_TIMEOUT = 408
    private const val INTERNAL_SERVER_ERROR = 500
    private const val BAD_GATEWAY = 502
    private const val SERVICE_UNAVAILABLE = 503
    private const val GATEWAY_TIMEOUT = 504

    fun <T : BaseEntity> handleGlobalError(context: FragmentActivity): WeaverTransformer<T> = WeaverTransformer(
            upStreamSchedulerProvider = {
                AndroidSchedulers.mainThread()
            },
            downStreamSchedulerProvider = {
                AndroidSchedulers.mainThread()
            },
            // 通过onNext流中数据的状态进行操作
            globalOnNextInterceptor = {
                when (it.statusCode) {
                    STATUS_UNAUTHORIZED -> {
                        // 用户验证失败,重新登录刷新token
                        Single.just(ReLoginAndRetryException(it))
                    }
                    else -> Single.just(ThrowableDelegate.EMPTY)
                }
            },
            // 通过onError中Throwable状态进行操作
            globalOnErrorResumeTransformer = { error ->
                when (error) {
                    is ConnectException -> Single.just(ConnectFailedAlertDialogException())
                    else -> Single.just(ThrowableDelegate.EMPTY)
                }
            },
            retryConfigProvider = {
                RetryConfig.Builder().setCondition { error ->
                    when (error) {
                        is ConnectFailedAlertDialogException,
                        is ReLoginAndRetryException -> true
                        else -> false
                    }
                }.build()
            },
            globalDoOnErrorConsumer = { error ->

            }
    )
}