package ru.fabit.localservice.realm.coroutines.util

import io.realm.kotlin.Realm

data class MonitoringLog(
    val connectionsCounter: MutableMap<String, Int> = mutableMapOf(),
    val instances: MutableMap<Long, Realm> = mutableMapOf(),
    val opened: MutableMap<String, Int> = mutableMapOf(),
    val closed: MutableMap<String, Int> = mutableMapOf(),
    val localServiceName: String = ""
)
