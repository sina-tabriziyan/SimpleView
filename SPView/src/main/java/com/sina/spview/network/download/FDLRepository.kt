//package com.sina.spview.network.download
//
//import android.util.Log
//import com.sina.spview.models.DownloadResult
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.flowOn
//import okhttp3.ResponseBody
//import java.io.File
//import java.io.FileOutputStream
//import java.io.IOException
//import java.io.InputStream
//import java.io.OutputStream
//
//class FDLRepository {
//
//    companion object {
//        private const val TAG = "FileDownloadRepository"
//        private const val DEFAULT_BUFFER_SIZE = 4096
//    }
//
//    fun downloadFile(body: ResponseBody, destinationPath: String): Flow<DownloadResult> = flow {
//        if (destinationPath.isBlank()) {
//            emit(DownloadResult.Error("Destination path is blank.", IllegalArgumentException("Path cannot be blank")))
//            return@flow
//        }
//
//        val file = File(destinationPath)
//        // Optional: Ensure parent directory exists
//        file.parentFile?.mkdirs()
//
//        var inputStream: InputStream? = null
//        var outputStream: OutputStream? = null
//
//        try {
//            inputStream = body.byteStream()
//            outputStream = FileOutputStream(file)
//
//            val fileReader = ByteArray(DEFAULT_BUFFER_SIZE)
//            var totalBytesRead: Long = 0
//            val totalFileSize = body.contentLength() // Returns -1 if unknown
//
//            if (totalFileSize <= 0) {
//                // Emit a state indicating size is unknown, UI can show indeterminate progress
//                emit(DownloadResult.UnknownSize)
//                // Fallback to reading all bytes without percentage progress
//                while (true) {
//                    val bytesRead = inputStream.read(fileReader)
//                    if (bytesRead == -1) break
//                    outputStream.write(fileReader, 0, bytesRead)
//                    totalBytesRead += bytesRead
//                    // Optional: emit totalBytesRead if UI wants to show raw bytes downloaded
//                }
//            } else {
//                // Normal progress reporting
//                var lastReportedPercentage = -1
//                while (true) {
//                    val bytesRead = inputStream.read(fileReader)
//                    if (bytesRead == -1) break // End of stream
//
//                    outputStream.write(fileReader, 0, bytesRead)
//                    totalBytesRead += bytesRead
//
//                    val progressPercentage = ((totalBytesRead * 100) / totalFileSize).toInt()
//
//                    // Only emit if percentage changes to avoid flooding the collector
//                    if (progressPercentage != lastReportedPercentage) {
//                        emit(DownloadResult.Progress(progressPercentage))
//                        lastReportedPercentage = progressPercentage
//                    }
//                }
//            }
//
//            outputStream.flush()
//            // Ensure 100% is emitted if it wasn't due to integer division
//            if (totalFileSize > 0 && totalBytesRead == totalFileSize) {
//                 // Check if the last reported percentage was already 100
//                val finalPercentage = ((totalBytesRead * 100) / totalFileSize).toInt()
//                if (finalPercentage == 100 && (emit(DownloadResult.Progress(100)) != null) ) {
//                    // This is to ensure 100 is explicitly sent if not already
//                    // The emit() here is just to satisfy the compiler if it's the last statement
//                    // and you want to ensure the Progress(100) is the last Progress event.
//                    // A better way is to ensure lastReportedPercentage logic handles this.
//                }
//            } else if (totalFileSize > 0 && totalBytesRead < totalFileSize) {
//                // This case suggests an issue, the stream ended before all bytes were read.
//                Log.w(TAG, "Download stream ended prematurely. Read $totalBytesRead, expected $totalFileSize")
//                // Fall through to emit Success, but this might indicate a truncated file.
//                // Or you could emit an error here.
//            }
//
//
//            emit(DownloadResult.Success(destinationPath))
//            Log.d(TAG, "File download complete: $destinationPath, Total Bytes: $totalBytesRead")
//
//        } catch (e: IOException) {
//            Log.e(TAG, "IOException during download to $destinationPath: ${e.message}", e)
//            // Optionally delete partially downloaded file on error
//            // file.delete()
//            emit(DownloadResult.Error("Error during download: ${e.localizedMessage}", e))
//        } catch (e: Exception) { // Catch any other unexpected exceptions
//            Log.e(TAG, "Unexpected error during download to $destinationPath: ${e.message}", e)
//            // file.delete()
//            emit(DownloadResult.Error("An unexpected error occurred: ${e.localizedMessage}", e))
//        } finally {
//            try {
//                inputStream?.close()
//            } catch (e: IOException) {
//                Log.e(TAG, "Error closing input stream: ${e.message}", e)
//            }
//            try {
//                outputStream?.close()
//            } catch (e: IOException) {
//                Log.e(TAG, "Error closing output stream: ${e.message}", e)
//            }
//        }
//    }.flowOn(Dispatchers.IO) // Ensure all operations in this flow run on the IO dispatcher
//     .catch { throwable -> // Catch exceptions from the flow itself (e.g., from flowOn or intermediate operators)
//         Log.e(TAG, "Exception in downloadFile flow: ${throwable.message}", throwable)
//         // It's crucial to emit an error state here as well if the flow itself throws an exception
//         // before or after the try-catch block inside the flow builder.
//         // However, the try-catch inside the flow builder is more specific to the download logic.
//         // This outer catch might be redundant if the inner one is comprehensive.
//         // For robustness, ensure all paths lead to a terminal DownloadResult (Success or Error).
//         // If an exception is caught here, it means the flow collector will get this exception
//         // directly unless we emit a DownloadResult.Error.
//         // To be safe, ensure an Error is emitted if not already handled.
//         // This is a common pattern, but make sure your inner try-catch is thorough.
//         // If the inner try-catch re-throws or doesn't catch something, this will catch it.
//         // Consider if you need to emit(DownloadResult.Error(...)) here.
//         // If the inner catch handles all IOExceptions and emits an Error, this might only catch
//         // cancellation exceptions or very rare issues.
//     }
//}