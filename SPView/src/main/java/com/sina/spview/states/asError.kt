/**
 * Created by ST on 1/8/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states


fun NetworkError.asError() = Error(
    code = code,
    message = message,
    data = data.asStatus()
)