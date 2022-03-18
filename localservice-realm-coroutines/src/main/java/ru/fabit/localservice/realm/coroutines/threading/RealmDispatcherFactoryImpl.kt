package ru.fabit.localservice.realm.coroutines.threading

import io.realm.RealmModel

class RealmDispatcherFactoryImpl(private val factory: HandlerDispatcherFactory) :
    RealmDispatcherFactory {
    override fun get(clazz: Class<out RealmModel>) = factory.get()
}