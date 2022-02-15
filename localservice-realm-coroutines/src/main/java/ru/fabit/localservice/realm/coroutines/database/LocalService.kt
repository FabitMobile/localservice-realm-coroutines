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
        clazz: Class<RealmModel>,
        predicate: (RealmQuery<RealmModel>) -> RealmQuery<RealmModel>,
        aggregationFunction: AggregationFunction,
        nameField: String = ""
    ): Flow<Number?>

    suspend fun get(
        localServiceParams: LocalServiceParams
    ): Flow<List<RealmModel>>

    suspend fun getSize(
        clazz: Class<RealmModel>,
        predicate: (RealmQuery<RealmModel>) -> RealmQuery<RealmModel>
    ): Flow<Int>

    suspend fun storeObject(clazz: Class<RealmModel>, jsonObject: JSONObject)

    suspend fun storeObjects(clazz: Class<RealmModel>, jsonArray: JSONArray)

    suspend fun update(
        clazz: Class<RealmModel>,
        predicate: (RealmQuery<RealmModel>) -> RealmQuery<RealmModel>,
        action: (RealmModel) -> Unit
    )

    suspend fun delete(
        clazz: Class<RealmModel>,
        predicate: ((RealmQuery<RealmModel>) -> RealmQuery<RealmModel>)?
    )

    suspend fun deleteAndStoreObjects(
        clazz: Class<RealmModel>,
        predicate: ((RealmQuery<RealmModel>) -> RealmQuery<RealmModel>)?,
        jsonArray: JSONArray
    )

    suspend fun getIds(
        clazz: Class<RealmModel>,
        predicate: ((RealmQuery<RealmModel>) -> RealmQuery<RealmModel>)?,
        action: (RealmModel) -> Int
    ): Set<Int>

    fun getMonitoringLog(): MonitoringLog?

    fun getGlobalInstanceCount(): Int

    fun getLocalInstanceCount(): Int
}