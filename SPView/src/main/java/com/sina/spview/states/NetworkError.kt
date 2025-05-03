/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import com.google.gson.annotations.SerializedName

data class NetworkError(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    val data: NetworkStatus
)
