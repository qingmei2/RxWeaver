package com.github.qingmei2.activity.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import qingmei2.github.qingmei2.R
import java.util.concurrent.TimeUnit

@SuppressWarnings("CheckResult")
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mBtnSuccess.setOnClickListener {
            loginAsync(true)
        }
        mBtnFailed.setOnClickListener {
            loginAsync(false)
        }
    }

    private fun loginAsync(success: Boolean) {
        when (success) {
            // 模拟2秒耗时，作为用户一系列登录（输入、点击登录按钮、异步请求网络等）的过程
            true -> Observable.just(true)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        mProgressBar.visibility = View.VISIBLE  // 展示progressBar
                    }
                    .observeOn(Schedulers.io())
                    .delay(2000, TimeUnit.MILLISECONDS)   // 延迟两秒
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        mProgressBar.visibility = View.GONE     // 隐藏progressBar
                        onLoginResult(true)
                    }
            // 登录失败不需要模拟耗时，因为用户有可能立即点击返回键关闭页面
            false -> onLoginResult(false)
        }
    }

    private fun onLoginResult(success: Boolean) {
        val intent = Intent().putExtra(EXTRA_SUCCESS, success)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        const val EXTRA_SUCCESS = "EXTRA_SUCCESS"
    }
}
