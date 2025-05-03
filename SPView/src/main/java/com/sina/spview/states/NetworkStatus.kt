/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import com.google.gson.annotations.SerializedName

data class NetworkStatus(
    @SerializedName("status")
    val status: Int
)
