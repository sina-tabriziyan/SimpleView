package com.sina.spview.models

//sealed class DownloadResult {
//    data class Progress(val percentage: Int) : DownloadResult()
//    data class Success(val filePath: String) : DownloadResult()
//    data class Error(val message: String, val exception: Throwable? = null) : DownloadResult()
//    data object UnknownSize : DownloadResult()
//}



// --- Sealed Class for Download States ---
sealed class DownloadResult {
    /** Indicates the download is in progress. */
    data class Progress(val percentage: Int) : DownloadResult()

    /** Indicates the download started but the total size is unknown. UI can show indeterminate progress. */
    object StartedUnknownSize : DownloadResult()

    /** Indicates the download completed successfully. */
    data class Success(val filePath: String, val totalBytes: Long) : DownloadResult()

    /** Indicates the download failed. */
    data class Error(val message: String, val cause: Throwable? = null) : DownloadResult()

    /** Indicates the download was explicitly cancelled. */
    object Cancelled : DownloadResult()
}
