package com.sina.spview.states

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

suspend fun <T> Flow<T>.collectWithHandler(
    onError: (Throwable) -> Unit,
    onCollect: (T) -> Unit
) {
    this
        .catch { exception -> onError(exception) }
        .collect { value -> onCollect(value) }
}