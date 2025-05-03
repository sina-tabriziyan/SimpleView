package com.sina.spview.donwload

interface ProgressListener {
    fun update(url: String?, percent: Int, byteRead: Long, contentLength: Long, done: Boolean)
}