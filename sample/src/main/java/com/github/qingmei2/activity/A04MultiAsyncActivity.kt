package com.github.qingmei2.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.qingmei2.api.FakeDataSource
import com.github.qingmei2.entity.BaseEntity
import com.github.qingmei2.entity.UserInfo
import com.github.qingmei2.utils.RxUtils
import com.github.qingmei2.utils.appendLine
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_04_multi_async.*
import qingmei2.github.qingmei2.R
import java.util.concurrent.TimeUnit

/**
 * 多个异步请求的错误处理
 */
class A04MultiAsyncActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_04_multi_async)

        mFabRequest.setOnClickListener { startMultiAsync() }
    }

    // 同时进行2个异步请求
    // 这里我们模拟快速的响应，2个接口几乎同时进行网络请求，短时间内先后收到用户信息失效的401错误；
    @SuppressLint("CheckResult")
    private fun startMultiAsync() {

        // A接口
        Observable.fromCallable(FakeDataSource::queryUserInfo)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { entity ->
                    when (entity.statusCode == RxUtils.STATUS_OK) {
                        true -> mTvLogs.appendLine("请求A接口,3秒后成功返回用户信息")
                        false -> mTvLogs.appendLine("请求A接口,3秒后返回失败和401状态码")
                    }
                }
                .observeOn(Schedulers.io())
                .delay(3, TimeUnit.SECONDS)
                .compose(RxUtils.processGlobalError<BaseEntity<UserInfo>>(this))
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.data!! }
                .subscribe({ userInfo ->
                    mTvLogs.appendLine("A接口请求成功，用户信息：$userInfo")
                }, { error ->
                    mTvLogs.appendLine("A接口请求失败，异常信息：$error")
                })

        // B接口
        Observable.fromCallable(FakeDataSource::queryUserInfo)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { entity ->
                    when (entity.statusCode == RxUtils.STATUS_OK) {
                        true -> mTvLogs.appendLine("请求B接口,4秒后成功返回用户信息")
                        false -> mTvLogs.appendLine("请求B接口,4秒后返回失败和401状态码")
                    }
                }
                .observeOn(Schedulers.io())
                .delay(4, TimeUnit.SECONDS)
                .compose(RxUtils.processGlobalError<BaseEntity<UserInfo>>(this))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ userInfo ->
                    mTvLogs.appendLine("B接口请求成功，用户信息：$userInfo")
                }, { error ->
                    mTvLogs.appendLine("B接口请求失败，异常信息：$error")
                })
    }
}