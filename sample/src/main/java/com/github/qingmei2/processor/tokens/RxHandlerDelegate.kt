package com.github.qingmei2.processor.tokens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class RxHandlerDelegate : Thread() {

    private lateinit var mHandler: Handler

    private val mMessageSubject: PublishSubject<RxHandlerMessage> =
            PublishSubject.create<RxHandlerMessage>()

    @SuppressLint("HandlerLeak")
    override fun run() {
        super.run()
        Looper.prepare()

        mHandler = object : Handler() {

            override fun handleMessage(msg: Message) {
                val messageWrapper = msg.obj as RxHandlerMessage
                mMessageSubject.onNext(messageWrapper)
            }
        }

        Looper.loop()
    }

    fun sendMessage(timeStamp: Long): Observable<RxHandlerMessage> {
        val message = Message.obtain()
        message.obj = obtainMessageWrapper(timeStamp)
        mHandler.sendMessageDelayed(message, 200)

        return mMessageSubject
                .filter { it.timeStamp == timeStamp }
                .firstOrError()
                .toObservable()
    }

    private fun obtainMessageWrapper(timeStamp: Long): RxHandlerMessage {
        return RxHandlerMessage(timeStamp)
    }

    companion object {

        @Volatile
        private var instance: RxHandlerDelegate? = null

        fun getInstance(): RxHandlerDelegate =
                instance ?: synchronized(this) {
                    instance ?: RxHandlerDelegate().apply {
                        instance = this
                        this.start()
                    }
                }
    }
}