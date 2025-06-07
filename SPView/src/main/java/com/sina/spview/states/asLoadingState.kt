/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart


fun <T> Flow<ResponseState<T>>.asLoadingState(): Flow<LoadingState<T>> =
    this
        .map { responseState -> // Use map to transform items from the original flow
            when (responseState) {
                is ResponseState.Success -> LoadingState.Success(responseState.data)
                is ResponseState.Error -> LoadingState.Error(responseState.exception)
            }
        }
        .onStart {
            // Emit an initial loading state.
            // Since this flow doesn't have intrinsic progress, 0% is a common default.
            // Or you could have a LoadingState without progress for such cases.
            emit(LoadingState.Loading(0)) // Default to 0% progress
        }
        .catch { e ->
            // This catches exceptions from the upstream flow (map, or the original flow itself)
            // AND from onStart if it were to throw (unlikely for a simple emit).
            emit(LoadingState.Error(e))
        }