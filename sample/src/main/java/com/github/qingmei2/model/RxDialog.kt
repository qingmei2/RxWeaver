package com.github.qingmei2.model

import android.content.Context
import android.support.v7.app.AlertDialog
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object RxDialog {

    /**
     * 简单的示例，弹出一个dialog提示用户，将用户的操作转换为一个流并返回
     */
    fun showErrorDialog(context: Context,
                        message: String): Single<Boolean> =
            Single
                    .create<Boolean> { emitter ->
                        AlertDialog.Builder(context)
                                .setTitle("错误")
                                .setMessage("您收到了一个异常:$message,是否重试本次请求？")
                                .setCancelable(false)
                                .setPositiveButton("重试") { _, _ -> emitter.onSuccess(true) }
                                .setNegativeButton("取消") { _, _ -> emitter.onSuccess(false) }
                                .show()
                    }
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.io())

//            【注意】上面的线程调度代码并未出错，因为从子线程的异步操作中，接收到错误并显示dialog;
//                   这时需要把线程切换回MainThread, 在选择完成后，再将结果在子线程中返回给下游。
//              ---> 比如，如果将线程调度的代码改成下面，则会出错:
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
}