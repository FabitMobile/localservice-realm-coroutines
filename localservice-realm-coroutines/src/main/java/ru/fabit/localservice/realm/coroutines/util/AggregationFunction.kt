package ru.fabit.localservice.realm.coroutines.util

enum class AggregationFunction(private val text: String) {
    MAX("max"), MIN("min"), SUM("sum"), SIZE("size");

    override fun toString(): String {
        return text
    }
}