package ru.fabit.localservice.realm.coroutines.database

import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.RealmObject
import kotlin.reflect.KClass

data class LocalServiceParams(
    val clazz: KClass<out RealmObject>,
    val predicate: ((RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>)? = null,
    val sortPair: Pair<String, Sort>? = null
)