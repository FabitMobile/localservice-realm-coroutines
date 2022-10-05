package ru.fabit.localservice.realm.coroutines.threading

import io.realm.kotlin.types.RealmObject
import kotlin.reflect.KClass

class RealmDispatcherFactoryImpl(private val factory: HandlerDispatcherFactory) :
    RealmDispatcherFactory {
    override fun get(clazz: KClass<out RealmObject>) = factory.get(clazz.simpleName ?: "")
}