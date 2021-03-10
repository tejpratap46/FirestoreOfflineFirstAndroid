package com.tejpratapsingh.firestoreofflinefirst.extensions

fun <T> List<T>.getFirstOrNull(): T? {
    if (this.isNotEmpty()) {
        return this[0]

    } else {
        return null
    }
}