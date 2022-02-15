package ru.fabit.localservice.realm.coroutines.threading

import kotlinx.coroutines.CoroutineDispatcher

interface DispatcherFactory {
    fun get(name: String = ""): CoroutineDispatcher
}