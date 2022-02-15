package ru.fabit.localservice.realm.coroutines.database

import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.Sort

data class LocalServiceParams(
    val clazz: Class<RealmModel>,
    val predicate: ((RealmQuery<RealmModel>) -> RealmQuery<RealmModel>)? = null,
    val sortPair: Map.Entry<Array<String>, Array<Sort>>? = null
)