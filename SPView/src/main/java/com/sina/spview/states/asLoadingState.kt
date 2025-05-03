/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform


fun <T> Flow<ResponseState<T>>.asLoadingState(): Flow<LoadingState<T>> =
    transform { responseState ->
        when (responseState) {
            is ResponseState.Success -> emit(LoadingState.Success(responseState.data))
            is ResponseState.Error -> emit(LoadingState.Error(responseState.exception))
        }
    }.onStart { emit(LoadingState.Loading) }
        .catch { e -> emit(LoadingState.Error(e)) }
