@file:Suppress("UNREACHABLE_CODE")

package com.github.qingmei2

import android.support.v4.app.FragmentActivity
import android.widget.Toast
import com.github.qingmei2.core.RxThrowable
import com.github.qingmei2.core.WeaverTransformer
import com.github.qingmei2.model.NavigatorFragment
import com.github.qingmei2.model.RxDialog
import com.github.qingmei2.retry.RetryConfig
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import org.json.JSONException
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

            upStreamSchedulerProvider = { AndroidSchedulers.mainThread() },

            downStreamSchedulerProvider = { AndroidSchedulers.mainThread() },

            // 通过onNext流中数据的状态进行操作
            globalOnNextInterceptor = {
                when (it.statusCode) {
                    STATUS_UNAUTHORIZED -> {
                        Toast.makeText(context, "Token失效，跳转到Login重新登录！", Toast.LENGTH_SHORT).show()
                        NavigatorFragment()
                                .startLoginForResult(context)
                                .flatMap { loginSuccess ->
                                    if (loginSuccess) {
                                        Toast.makeText(context, "登陆成功！3秒延迟后重试！", Toast.LENGTH_SHORT).show()
                                        Single.just(ReLoginSuccessAndRetryException(it))
                                    } else {
                                        Toast.makeText(context, "登陆失败！", Toast.LENGTH_SHORT).show()
                                        Single.just(ReLoginFailedException(it))
                                    }
                                }
                    }
                    else -> Single.just(RxThrowable.EMPTY)
                }
            },

            // 通过onError中Throwable状态进行操作
            globalOnErrorResumeTransformer = { error ->
                when (error) {
                    is ConnectException -> {
                        RxDialog.showErrorDialog(context, "ConnectException")
                                .flatMap {
                                    if (it)   // 用户选择重试按钮,发送重试事件
                                        Single.just(ConnectFailedAlertDialogException())
                                    else
                                        Single.just(RxThrowable.EMPTY)

                                }
                    }
                    else -> Single.just(RxThrowable.EMPTY)
                }
            },

            retryConfigProvider = { error ->
                when (error) {
                    is ConnectFailedAlertDialogException -> RetryConfig(retryCondition = true)
                    is ReLoginSuccessAndRetryException -> RetryConfig(delay = 3000, retryCondition = true)
                    else -> RetryConfig(retryCondition = false) // 其它异常都不重试,比如ReLoginFailedException
                }
            },

            globalDoOnErrorConsumer = { error ->
                when (error) {
                    is JSONException -> {
                        Toast.makeText(context, "全局异常捕获-Json解析异常！", Toast.LENGTH_SHORT).show()
                    }
                    else -> {

                    }
                }
            }
    )
}