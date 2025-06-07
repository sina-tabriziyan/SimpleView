package com.sina.spview.media

import java.io.File

interface IAudioRecorder {
    fun start(outputFile: File, fileName: String): String
    fun stop()
}