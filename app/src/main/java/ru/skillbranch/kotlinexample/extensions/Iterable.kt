package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    var reversed = reversed()
    for (item in reversed) {
        reversed = reversed.drop(1)
        if (predicate.invoke(item)) break
    }
    return reversed.reversed()
}
