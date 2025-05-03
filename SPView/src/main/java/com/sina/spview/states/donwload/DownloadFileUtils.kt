package com.sina.spview.states.donwload

import kotlinx.coroutines.channels.Channel

object DownloadFileUtils {
    val downloadApkChannel = Channel<DownloadProgressBarEvent>()

    fun calculatePercent(per: Int, bytesRead: Long, fileSize: Long): Int {
        return if (per == -1 && fileSize > 0) (bytesRead * 100 / fileSize).toInt() else per
    }
}