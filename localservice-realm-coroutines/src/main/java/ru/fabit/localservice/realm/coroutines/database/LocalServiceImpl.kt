package ru.fabit.localservice.realm.coroutines.database

import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import ru.fabit.localservice.realm.coroutines.threading.RealmDispatcherFactory
import ru.fabit.localservice.realm.coroutines.util.AggregationFunction
import ru.fabit.localservice.realm.coroutines.util.MonitoringLog
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

class LocalServiceImpl(
    private val realm: Realm,
    private val realmDispatcherFactory: RealmDispatcherFactory,
    private val isEnabledMonitoring: Boolean
) : LocalService {

    private val connectionsCounter: MutableMap<String, Int> = mutableMapOf()
    private val openedCounter: MutableMap<String, Int> = mutableMapOf()
    private val closedCounter: MutableMap<String, Int> = mutableMapOf()
    private val instances: MutableMap<Long, Realm> = mutableMapOf()

    override suspend fun get(localServiceParams: LocalServiceParams): Flow<List<RealmObject>> {
        val dispatcher = realmDispatcherFactory.get(localServiceParams.clazz)
        val realmRef = AtomicReference<Realm>(null)

        val sortPair = localServiceParams.sortPair
        val predicate = localServiceParams.predicate

        if (isEnabledMonitoring) {
            incrementIfExist(openedCounter, localServiceParams.clazz.simpleName ?: "")
        }

        var flow = emptyFlow<List<RealmObject>>()

        withContext(dispatcher) {
            val realm = getRealm()
            realmRef.set(realm)
            var query = realm.query(localServiceParams.clazz)
            predicate?.let { predicate ->
                query = predicate(query)
            }
            flow =
                when (sortPair == null) {
                    true -> query
                    false -> query.sort(sortPair.first, sortPair.second)
                }
                    .asFlow()
                    .map {
                        it.list
                    }
        }

        return flow
            .onCompletion {
                closeRealm(realmRef.get())
                if (isEnabledMonitoring) {
                    incrementIfExist(closedCounter, localServiceParams.clazz.simpleName ?: "")
                }
            }
            .flowOn(dispatcher)

    }

    override suspend fun get(
        clazz: KClass<out RealmObject>,
        predicate: (RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>,
        aggregationFunction: AggregationFunction,
        nameField: String
    ): Flow<Number?> {
        val dispatcher = realmDispatcherFactory.get(clazz)
        var flow: Flow<Number?>
        val realmRef = AtomicReference<Realm>(null)
        withContext(dispatcher) {
            val realm = getRealm()
            realmRef.set(realm)
            var query = realm.query(clazz)
            query = predicate(query)
            flow = when (aggregationFunction) {
                AggregationFunction.MAX -> query.max(nameField, Number::class).asFlow()
                AggregationFunction.MIN -> query.min(nameField, Number::class).asFlow()
                AggregationFunction.SUM -> query.sum(nameField, Number::class).asFlow()
                AggregationFunction.SIZE -> query.count().asFlow()
            }
        }
        return flow
            .onCompletion {
                closeRealm(realmRef.get())
                if (isEnabledMonitoring) {
                    incrementIfExist(closedCounter, clazz.simpleName ?: "")
                }
            }
            .flowOn(dispatcher)
    }

    override suspend fun getSize(
        clazz: KClass<out RealmObject>,
        predicate: (RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>
    ): Flow<Int> {
        return get(clazz, predicate, AggregationFunction.SIZE)
            .map { number ->
                number?.toInt() ?: 0
            }
    }

    override suspend fun <T : RealmObject> storeObject(value: T) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.write {
                    copyToRealm(value, UpdatePolicy.ALL)
                }
            } catch (ex: Exception) {
                throw ex
            } finally {
                closeRealm(realm)
            }
        }
    }

    override suspend fun <T : RealmObject> storeObjects(values: List<T>) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.write {
                    values.forEach {
                        copyToRealm(it, UpdatePolicy.ALL)
                    }
                }
            } catch (ex: Exception) {
                throw ex
            } finally {
                closeRealm(realm)
            }
        }
    }

    override suspend fun update(
        clazz: KClass<out RealmObject>,
        predicate: (RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>,
        action: (RealmObject) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                var query = realm.query(clazz)
                query = predicate(query)
                realm.write {
                    val realmResults = query.find()
                    for (realmObject in realmResults) {
                        findLatest(realmObject)?.let { action(it) }
                    }
                }
            } catch (ex: Exception) {
                throw ex
            } finally {
                closeRealm(realm)
            }
        }
    }

    override suspend fun delete(
        clazz: KClass<out RealmObject>,
        predicate: ((RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>)?
    ) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.write {
                    var query = query(clazz)
                    predicate?.let {
                        query = predicate(query)
                    }
                    val realmResults = query.find()
                    if (realmResults.isNotEmpty()) {
                        delete(realmResults)
                    }
                }
            } catch (ex: Exception) {
                throw ex
            } finally {
                closeRealm(realm)
            }
        }
    }

    override suspend fun deleteAndStoreObjects(
        clazz: KClass<out RealmObject>,
        predicate: ((RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>)?,
        values: List<RealmObject>
    ) {
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            try {
                realm.write {
                    var query = query(clazz)
                    predicate?.let {
                        query = predicate(query)
                    }
                    val realmResults = query.find()
                    if (realmResults.isNotEmpty()) {
                        delete(realmResults)
                    }
                    values.forEach {
                        copyToRealm(it)
                    }
                }
            } catch (ex: Exception) {
                throw ex
            } finally {
                closeRealm(realm)
            }
        }
    }

    override suspend fun getIds(
        clazz: KClass<out RealmObject>,
        predicate: ((RealmQuery<out RealmObject>) -> RealmQuery<out RealmObject>)?,
        action: (RealmObject) -> Int
    ): Set<Int> {
        var ids: Set<Int> = setOf()
        withContext(Dispatchers.IO) {
            val realm = getRealm()
            var query = realm.query(clazz)
            predicate?.let {
                query = predicate(query)
            }
            ids = query.asFlow()
                .map { it.list }
                .map { listRealmObject -> listRealmObject.map { realmObject -> action(realmObject) } }
                .map { it.toSet() }
                .first()
            closeRealm(realm)
        }
        return ids
    }

    override fun getMonitoringLog() = when (isEnabledMonitoring) {
        true -> MonitoringLog(
            connectionsCounter,
            instances,
            openedCounter,
            closedCounter,
            this.toString()
        )
        false -> MonitoringLog()
    }

    private fun getRealm(): Realm {
        val threadName = Thread.currentThread().name
        val threadId = Thread.currentThread().id
        val realm = realm
        if (isEnabledMonitoring) {
            incrementIfExist(connectionsCounter, threadName)
            if (!instances.containsKey(threadId)) {
                instances[threadId] = realm
            }
        }
        return realm
    }

    @Synchronized
    private fun incrementIfExist(map: MutableMap<String, Int>, key: String) {
        if (!map.containsKey(key)) {
            map[key] = 0
        }
        var value = map[key] ?: 0
        value += 1
        map[key] = value
    }

    @Synchronized
    private fun decrementIfExist(map: MutableMap<String, Int>, key: String) {
        if (!map.containsKey(key)) {
            map[key] = 0
        }
        var value = map[key] ?: 0
        value -= 1
        map[key] = value

    }

    private fun closeRealm(realm: Realm?) {
        if (realm != null) {
            val threadName = Thread.currentThread().name
            if (isEnabledMonitoring) {
                decrementIfExist(connectionsCounter, threadName)
                instances.remove(Thread.currentThread().id)
            }
        }
    }

}