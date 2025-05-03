package com.sina.spview.donwload

data class DownloadProgressBarEvent(var downloadID: Int, var percent: Int, var bytesRead: Long, var contentLength: Long)
