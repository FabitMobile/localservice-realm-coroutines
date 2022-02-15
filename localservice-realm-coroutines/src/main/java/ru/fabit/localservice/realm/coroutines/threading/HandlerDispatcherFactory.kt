package ru.fabit.localservice.realm.coroutines.threading

import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher

class HandlerDispatcherFactory : DispatcherFactory {
    private val namePrefix = "HandlerThread"

    override fun get(name: String): CoroutineDispatcher {
        val handlerThread = HandlerThread(namePrefix.plus("_").plus(name))
        if (!handlerThread.isAlive) {
            handlerThread.start()
        }
        return Handler(handlerThread.looper).asCoroutineDispatcher()
    }
}