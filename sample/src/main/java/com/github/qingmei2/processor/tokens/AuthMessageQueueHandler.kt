package com.github.qingmei2.processor.tokens

import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class AuthMessageQueueHandler : Runnable {

    private var mService: ExecutorService = Executors.newSingleThreadExecutor()

    private var mMessageQueue: LinkedBlockingQueue<AuthMessage> = LinkedBlockingQueue()

    private val mMessageSubject: PublishSubject<AuthMessage> =
            PublishSubject.create<AuthMessage>()

    init {
        mService.submit(this)
    }

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

    fun sendMessage(timeStamp: Long): Observable<AuthMessage> {
        val msg = obtainMessageWrapper(timeStamp)
        mMessageQueue.put(msg)

        return mMessageSubject
                .filter { it.timeStamp == timeStamp }
                .observeOn(Schedulers.io())
    }

    private fun obtainMessageWrapper(timeStamp: Long): AuthMessage {
        return AuthMessage(timeStamp)
    }

    companion object {

        @Volatile
        private var instance: AuthMessageQueueHandler? = null

        fun getInstance(): AuthMessageQueueHandler =
                instance ?: synchronized(this) {
                    instance ?: AuthMessageQueueHandler().apply {
                        instance = this
                    }
                }
    }
}