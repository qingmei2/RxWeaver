package com.github.qingmei2.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.entity.UserInfo
import com.github.qingmei2.utils.RxUtils
import com.github.qingmei2.utils.appendLine
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_multi_async.*
import qingmei2.github.qingmei2.R
import java.util.concurrent.TimeUnit

class MultiAsyncActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_async)

        mFabRequest.setOnClickListener { startMultiAsync() }
    }

    // 同时进行2个异步请求
    // 这里我们模拟快速的响应，2个接口几乎同时进行网络请求，短时间内先后收到用户信息失效的401错误；
    @SuppressLint("CheckResult")
    private fun startMultiAsync() {

        // A接口
        Observable.fromCallable(::fakeRemoteDataSource)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { mTvLogs.appendLine("开始请求A接口,3秒后返回401状态码") }
                .observeOn(Schedulers.io())
                .delay(3, TimeUnit.SECONDS)
                .compose(RxUtils.handleGlobalError<BaseEntity<UserInfo>>(this))
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.data!! }
                .subscribe({ userInfo ->
                    mTvLogs.appendLine("A接口请求成功，用户信息：$userInfo")
                }, { error ->
                    mTvLogs.appendLine("A接口请求失败，异常信息：$error")
                })

        // B接口
        Observable.fromCallable(::fakeRemoteDataSource)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { mTvLogs.appendLine("开始请求B接口,4秒后返回401状态码") }
                .observeOn(Schedulers.io())
                .delay(6, TimeUnit.SECONDS)
                .compose(RxUtils.handleGlobalError<BaseEntity<UserInfo>>(this))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ userInfo ->
                    mTvLogs.appendLine("B接口请求成功，用户信息：$userInfo")
                }, { error ->
                    mTvLogs.appendLine("B接口请求失败，异常信息：$error")
                })
    }

    private fun fakeRemoteDataSource(): BaseEntity<UserInfo> {
        return when (RxUtils.hasRefreshToken) {
            false -> BaseEntity(
                    statusCode = 401,
                    message = "unauthorized",
                    data = null
            )
            true -> BaseEntity(
                    statusCode = 200,
                    message = "success",
                    data = UserInfo("qingmei2", 26)
            )
        }
    }
}