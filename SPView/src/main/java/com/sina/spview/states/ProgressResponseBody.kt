/**
 * Created by ST on 5/3/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import com.sina.spview.donwload.DownloadFileUtils
import com.sina.spview.donwload.ProgressListener
import kotlinx.coroutines.channels.Channel
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException

class ProgressResponseBody(
    private val url: String, private val responseBody: ResponseBody,
    private val progressListener: ProgressListener
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null
    private var totalBytesTemp: Long = 0
    private val isDone: (Long) -> Boolean = { x -> x == -1L }

    override fun contentLength(): Long = responseBody.contentLength()

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource as BufferedSource
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (!isDone(bytesRead)) bytesRead else 0

                // Update progress when a certain threshold is reached or download is complete
                if (totalBytesTemp == 0L || totalBytesRead - totalBytesTemp >= 150000 || isDone(
                        bytesRead
                    )
                ) {
                    totalBytesTemp = totalBytesRead
                    calculatePercent(contentLength(), totalBytesRead)
                }

                return bytesRead
            }
        }
    }

    private fun calculatePercent(contentLength: Long, bytesRead: Long) {
        if (contentLength > 0) {
            val per: Int = DownloadFileUtils.calculatePercent(-1, bytesRead, contentLength)
            progressListener.update(url, per, bytesRead, contentLength, isDone(bytesRead))
        } else {
            progressListener.update(url, -1, bytesRead, contentLength, isDone(bytesRead))
        }
    }
}



