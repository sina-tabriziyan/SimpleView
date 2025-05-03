/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Response

fun <T> Response<T>.openResponse(): T {
    return if (isSuccessful) {
        val body = body()
        when {
            code() in 200..201 && body != null -> body
            body == null -> throw NoBodyException()
            else -> throw Exception()
        }
    } else {
        val errorBody: ResponseBody? = errorBody()
        val networkError: NetworkError =
            Gson().fromJson(errorBody?.charStream(), NetworkError::class.java)
        throw NetworkException(error = networkError.asError())
    }
}