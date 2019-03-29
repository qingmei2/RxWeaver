package com.github.qingmei2.processor.tokens

import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class RxHandlerDelegate : Runnable {

    private var mService: ExecutorService = Executors.newSingleThreadExecutor()

    private var mMessageQueue: LinkedBlockingQueue<MessageWrapper> = LinkedBlockingQueue()

    private val mMessageSubject: PublishSubject<MessageWrapper> =
            PublishSubject.create<MessageWrapper>()

    init {
        mService.submit(this)
    }

    @SuppressLint("HandlerLeak")
    override fun run() {
        while (true) {
            Thread.sleep(200)
            while (AuthorizationErrorProcessor.mIsBlocking.not()) {
                val msg = mMessageQueue.take()
                when (AuthorizationErrorProcessor.mIsBlocking) {
                    true -> {
                        mMessageQueue.put(msg)
                    }
                    false -> mMessageSubject.onNext(msg)
                }
            }
        }
    }

    fun sendMessage(timeStamp: Long): Observable<MessageWrapper> {
        val msg = obtainMessageWrapper(timeStamp)
        mMessageQueue.put(msg)

        return mMessageSubject
                .filter { it.timeStamp == timeStamp }
                .observeOn(Schedulers.io())
    }

    private fun obtainMessageWrapper(timeStamp: Long): MessageWrapper {
        return MessageWrapper(timeStamp)
    }

    companion object {

        @Volatile
        private var instance: RxHandlerDelegate? = null

        fun getInstance(): RxHandlerDelegate =
                instance ?: synchronized(this) {
                    instance ?: RxHandlerDelegate().apply {
                        instance = this
                    }
                }
    }
}