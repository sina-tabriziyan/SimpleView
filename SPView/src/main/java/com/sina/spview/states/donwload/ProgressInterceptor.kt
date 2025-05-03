/**
 * Created by ST on 5/3/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states.donwload

import com.sina.spview.states.ProgressResponseBody
import okhttp3.Interceptor
import okhttp3.Response

class ProgressInterceptor(
    private var progressListener: ProgressListener,
    private var downloadId: Int = 0
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())

        if (downloadId == 0)
            return originalResponse

        return originalResponse.newBuilder()
            .body(originalResponse.body?.let {
                ProgressResponseBody(
                    chain.request().url.toUrl().toString(), it, progressListener
                )
            })
            .build()
    }
}