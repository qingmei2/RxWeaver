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
import kotlinx.android.synthetic.main.activity_03_token_expired.*
import qingmei2.github.qingmei2.R
import java.util.concurrent.TimeUnit

class A03TokenExpiredActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_03_token_expired)

        mFabRequest.setOnClickListener { queryUserInfo() }
    }

    @SuppressLint("CheckResult")
    private fun queryUserInfo() {
        Observable.fromCallable(FakeDataSource::queryUserInfo)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { entity ->
                    when (entity.statusCode == RxUtils.STATUS_OK) {
                        true -> mTvLogs.appendLine("请求接口,2秒后成功返回用户信息")
                        false -> mTvLogs.appendLine("请求接口,2秒后返回失败和401状态码")
                    }
                }
                .observeOn(Schedulers.io())
                .delay(2, TimeUnit.SECONDS)
                .compose(RxUtils.handleGlobalError<BaseEntity<UserInfo>>(this))
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.data!! }
                .subscribe({ userInfo ->
                    mTvLogs.appendLine("接口请求成功，用户信息：$userInfo")
                }, { error ->
                    mTvLogs.appendLine("接口请求失败，异常信息：$error")
                })
    }
}