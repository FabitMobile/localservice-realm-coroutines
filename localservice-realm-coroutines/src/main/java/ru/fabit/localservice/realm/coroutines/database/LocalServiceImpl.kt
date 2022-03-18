package ru.fabit.localservice.realm.coroutines.database

import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.kotlin.toFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.fabit.localservice.realm.coroutines.threading.RealmDispatcherFactory
import ru.fabit.localservice.realm.coroutines.util.AggregationFunction
import ru.fabit.localservice.realm.coroutines.util.MonitoringLog
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Provider

class LocalServiceImpl(
    private val realmConfiguration: RealmConfiguration,
    private val realmProvider: Provider<Realm>,
    private val realmDispatcherFactory: RealmDispatcherFactory
) : LocalService {

    private val connectionsCounter: MutableMap<String, Int> = mutableMapOf()
    private val openedCounter: MutableMap<String, Int> = mutableMapOf()
    private val closedCounter: MutableMap<String, Int> = mutableMapOf()
    private val instances: MutableMap<Long, Realm> = mutableMapOf()

    override suspend fun get(localServiceParams: LocalServiceParams): Flow<List<RealmModel>> {
        val dispatcher = realmDispatcherFactory.get(localServiceParams.clazz)
        val realmRef = AtomicReference<Realm>(null)

        val sortPair = localServiceParams.sortPair
        val predicate = localServiceParams.predicate
        incrementIfExist(openedCounter, localServiceParams.clazz.simpleName)

        var flow = emptyFlow<List<RealmModel>>()

        withContext(dispatcher) {
            val realm = getRealm()
            realmRef.set(realm)
            var query = realm.where(localServiceParams.clazz)
            predicate?.let { predicate ->
                query = predicate(query)
            }
            flow =
                when (sortPair == null) {
                    true -> query.findAll()
                    false -> query.findAll().sort(sortPair.key, sortPair.value)
                }
                    .toFlow()
                    .filter {
                        it.isLoaded
                    }
                    .map {
                        realm.copyFromRealm(it)
                    }
        }

        return flow
            .onCompletion {
                closeRealm(realmRef.get())
                incrementIfExist(closedCounter, localServiceParams.clazz.simpleName)
            }
            .flowOn(dispatcher)


    }

    override suspend fun get(
        clazz: Class<out RealmModel>,
        predicate: (RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>,
        aggregationFunction: AggregationFunction,
        nameField: String
    ): Flow<Number?> {
        val dispatcher = realmDispatcherFactory.get(clazz)
        var flow = emptyFlow<Number?>()
        val realmRef = AtomicReference<Realm>(null)
        withContext(dispatcher) {
            val realm = getRealm()
            realmRef.set(realm)
            var query = realm.where(clazz)
            query = predicate(query)
            flow = query.findAll()
                .toFlow()
                .filter { model ->
                    model.isLoaded
                }
                .map { results ->
                    if (results.size > 0) {
                        when (aggregationFunction) {
                            AggregationFunction.MAX -> results.max(nameField)
                            AggregationFunction.MIN -> results.min(nameField)
                            AggregationFunction.SUM -> results.sum(nameField)
                            AggregationFunction.SIZE -> results.size
                            AggregationFunction.AVERAGE -> results.average(nameField)
                        }
                    } else {
                        null
                    }
                }

        }
        return flow
            .onCompletion {
                closeRealm(realmRef.get())
                incrementIfExist(closedCounter, clazz.simpleName)
            }
            .flowOn(dispatcher)
    }

    override suspend fun getSize(
        clazz: Class<out RealmModel>,
        predicate: (RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>
    ): Flow<Int> {
        return get(clazz, predicate, AggregationFunction.SIZE)
            .map { number ->
                number?.toInt() ?: 0
            }
    }


    override suspend fun storeObject(clazz: Class<out RealmModel>, jsonObject: JSONObject) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.beginTransaction()
                realm.createOrUpdateObjectFromJson(clazz, jsonObject)
            } catch (ex: Exception) {
                throw ex
            } finally {
                realm.commitTransaction()
                closeRealm(realm)
            }
        }
    }

    override suspend fun storeObjects(clazz: Class<out RealmModel>, jsonArray: JSONArray) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.beginTransaction()
                realm.createOrUpdateAllFromJson(clazz, jsonArray)
            } catch (ex: Exception) {
                throw ex
            } finally {
                realm.commitTransaction()
                closeRealm(realm)
            }
        }
    }

    override suspend fun update(
        clazz: Class<out RealmModel>,
        predicate: (RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>,
        action: (RealmModel) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.beginTransaction()
                var query = realm.where(clazz)
                query = predicate(query)
                val realmObject = query.findFirst()
                realmObject?.let {
                    action(realmObject)
                    realm.copyToRealmOrUpdate(realmObject)
                }
            } catch (ex: Exception) {
                throw ex
            } finally {
                realm.commitTransaction()
                closeRealm(realm)
            }
        }
    }

    override suspend fun delete(
        clazz: Class<out RealmModel>,
        predicate: ((RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>)?
    ) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.beginTransaction()
                var query = realm.where(clazz)
                predicate?.let {
                    query = predicate(query)
                }
                val results = query.findAll()
                results.deleteAllFromRealm()
            } catch (ex: Exception) {
                throw ex
            } finally {
                realm.commitTransaction()
                closeRealm(realm)
            }
        }
    }

    override suspend fun deleteAndStoreObjects(
        clazz: Class<out RealmModel>,
        predicate: ((RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>)?,
        jsonArray: JSONArray
    ) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.beginTransaction()
                var query = realm.where(clazz)
                predicate?.let {
                    query = predicate(query)
                }
                val results = query.findAll()
                results.deleteAllFromRealm()
                realm.createOrUpdateAllFromJson(clazz, jsonArray)
            } catch (ex: Exception) {
                throw ex
            } finally {
                realm.commitTransaction()
                closeRealm(realm)
            }
        }
    }

    override suspend fun getIds(
        clazz: Class<out RealmModel>,
        predicate: ((RealmQuery<out RealmModel>) -> RealmQuery<out RealmModel>)?,
        action: (RealmModel) -> Int
    ): Set<Int> {
        var ids: Set<Int> = setOf()
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            var query = realm.where(clazz)
            predicate?.let {
                query = predicate(query)
            }
            val results = query.findAll()
            ids = realm.copyFromRealm(results)
                .map { realmModel -> action(realmModel) }
                .toSet()
            closeRealm(realm)
        }
        return ids
    }

    override fun getMonitoringLog() =
        MonitoringLog(
            connectionsCounter,
            instances,
            openedCounter,
            closedCounter,
            this.toString()
        )


    override fun getGlobalInstanceCount(): Int {
        return Realm.getGlobalInstanceCount(realmConfiguration)
    }

    override fun getLocalInstanceCount(): Int {
        return Realm.getLocalInstanceCount(realmConfiguration)
    }

    private fun getRealm(): Realm {
        val threadName = Thread.currentThread().name
        val threadId = Thread.currentThread().id
        incrementIfExist(connectionsCounter, threadName)
        val realm = realmProvider.get()
        if (!instances.containsKey(threadId)) {
            instances[threadId] = realm
        }
        return realm
    }

    private fun incrementIfExist(map: MutableMap<String, Int>, key: String) {
        if (!map.containsKey(key)) {
            map[key] = 0
        }
        var value = map[key]!!
        value += 1
        map[key] = value
    }

    private fun decrementIfExist(map: MutableMap<String, Int>, key: String) {
        if (!map.containsKey(key)) {
            map[key] = 0
        }
        var value = map[key]!!
        value -= 1
        map[key] = value
    }

    private fun closeRealm(realm: Realm?) {
        if (realm != null) {
            val threadName = Thread.currentThread().name
            decrementIfExist(connectionsCounter, threadName)
            instances.remove(Thread.currentThread().id)
            realm.close()
        }
    }

}