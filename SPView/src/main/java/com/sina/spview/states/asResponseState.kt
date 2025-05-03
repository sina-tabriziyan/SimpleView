/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

fun <T> Flow<T>.asResponseState(timeoutMs: Long = 5000): Flow<ResponseState<T>> =
    this.map { value ->
        withTimeoutOrNull(timeoutMs) { value }?.let { ResponseState.Success(it) }
            ?: ResponseState.Error(Exception("Timeout after $timeoutMs ms"))
    }.catch { e -> emit(ResponseState.Error(e)) }
        .flowOn(Dispatchers.IO)