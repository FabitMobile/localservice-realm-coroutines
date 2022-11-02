package ru.fabit.localservice.realm.coroutines.database

import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import ru.fabit.localservice.realm.coroutines.util.AggregationFunction
import ru.fabit.localservice.realm.coroutines.util.MonitoringLog
import kotlin.reflect.KClass

interface LocalService {
    suspend fun get(
        clazz: KClass<out RealmObject>,
        predicate: (RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>,
        aggregationFunction: AggregationFunction,
        nameField: String = ""
    ): Flow<Number?>

    suspend fun get(
        localServiceParams: LocalServiceParams
    ): Flow<List<RealmObject>>

    suspend fun getSize(
        clazz: KClass<out RealmObject>,
        predicate: (RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>
    ): Flow<Int>

    suspend fun <T: RealmObject> storeObject(value: T)

    suspend fun <T: RealmObject> storeObjects(values: List<T>)

    suspend fun update(
        clazz: KClass<out RealmObject>,
        predicate: (RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>,
        action: (RealmObject) -> Unit
    )

    suspend fun delete(
        clazz: KClass<out RealmObject>,
        predicate: ((RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>)?
    )

    suspend fun deleteAndStoreObjects(
        clazz: KClass<out RealmObject>,
        predicate: ((RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>)?,
        values: List<RealmObject>
    )

    suspend fun getIds(
        clazz: KClass<out RealmObject>,
        predicate: ((RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>)?,
        action: (RealmObject) -> Int
    ): Set<Int>

    fun getMonitoringLog(): MonitoringLog?
}