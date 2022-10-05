package ru.fabit.localservice.realm.coroutines.threading

import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

interface RealmDispatcherFactory {
    fun get(clazz: KClass<out RealmObject>): CoroutineDispatcher
}