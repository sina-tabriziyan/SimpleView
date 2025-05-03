package com.sina.spview.states

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class ErrorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()

        return chain.proceed(builder.build())
    }
}