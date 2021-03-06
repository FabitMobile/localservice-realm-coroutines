package ru.fabit.localservice.realm.coroutines.database

import io.realm.RealmModel
import io.realm.RealmQuery
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import ru.fabit.localservice.realm.coroutines.util.AggregationFunction
import ru.fabit.localservice.realm.coroutines.util.MonitoringLog

interface LocalService {
    suspend fun get(
        clazz: Class<out RealmModel>,
        predicate: (RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>,
        aggregationFunction: AggregationFunction,
        nameField: String = ""
    ): Flow<Number?>

    suspend fun get(
        localServiceParams: LocalServiceParams
    ): Flow<List<RealmModel>>

    suspend fun getSize(
        clazz: Class<out RealmModel>,
        predicate: (RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>
    ): Flow<Int>

    suspend fun storeObject(clazz: Class<out RealmModel>, jsonObject: JSONObject)

    suspend fun storeObjects(clazz: Class<out RealmModel>, jsonArray: JSONArray)

    suspend fun update(
        clazz: Class<out RealmModel>,
        predicate: (RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>,
        action: (RealmModel) -> Unit
    )

    suspend fun delete(
        clazz: Class<out RealmModel>,
        predicate: ((RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>)?
    )

    suspend fun deleteAndStoreObjects(
        clazz: Class<out RealmModel>,
        predicate: ((RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>)?,
        jsonArray: JSONArray
    )

    suspend fun getIds(
        clazz: Class<out RealmModel>,
        predicate: ((RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>)?,
        action: (RealmModel) -> Int
    ): Set<Int>

    fun getMonitoringLog(): MonitoringLog?

    fun getGlobalInstanceCount(): Int

    fun getLocalInstanceCount(): Int
}