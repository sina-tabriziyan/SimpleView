/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T> Flow<ResponseState<T>>.safeOpen(): Flow<T> = map { response ->
    return@map when (response) {
        is ResponseState.Error -> throw response.exception!!
        is ResponseState.Success -> response.data
    }
}