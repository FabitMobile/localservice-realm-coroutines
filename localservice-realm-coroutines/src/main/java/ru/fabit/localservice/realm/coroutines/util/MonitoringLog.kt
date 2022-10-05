package ru.fabit.localservice.realm.coroutines.util

import io.realm.kotlin.Realm

data class MonitoringLog(
    val connectionsCounter: MutableMap<String, Int>,
    val instances: MutableMap<Long, Realm>,
    val opened: MutableMap<String, Int>,
    val closed: MutableMap<String, Int>,
    val localServiceName: String = ""
)
