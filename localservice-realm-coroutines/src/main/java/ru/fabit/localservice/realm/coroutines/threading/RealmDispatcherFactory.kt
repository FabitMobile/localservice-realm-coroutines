package ru.fabit.localservice.realm.coroutines.threading

import io.realm.RealmModel
import kotlinx.coroutines.CoroutineDispatcher

interface RealmDispatcherFactory {
    fun get(clazz: Class<out RealmModel>): CoroutineDispatcher
}