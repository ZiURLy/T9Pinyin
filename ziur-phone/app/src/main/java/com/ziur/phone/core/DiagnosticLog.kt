package com.ziur.phone.core

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Private rotating diagnostic log. Contact data stays on the device. */
object DiagnosticLog {
    private const val TAG = "ZiURPhone"
    private const val MAX_BYTES = 512 * 1024
    @Volatile private var logFile: File? = null
    @Volatile private var ready = false

    fun init(context: Context) {
        logFile = File(context.filesDir, "ziur-diagnostics.log")
        ready = true
        i("LOGGER", "initialized")
    }

    @Synchronized fun i(event: String, message: String) = write("I", event, message, null)
    @Synchronized fun w(event: String, message: String) = write("W", event, message, null)
    @Synchronized fun e(event: String, message: String, error: Throwable? = null) = write("E", event, message, error)

    @Synchronized
    private fun write(level: String, event: String, message: String, error: Throwable?) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$stamp $level/$event: $message" +
            (error?.let { "\n" + Log.getStackTraceString(it) } ?: "") + "\n"
        when (level) {
            "E" -> Log.e(TAG, line)
            "W" -> Log.w(TAG, line)
            else -> Log.i(TAG, line)
        }
        if (!ready) return
        runCatching {
            val file = logFile ?: return
            if (file.exists() && file.length() > MAX_BYTES) {
                val previous = File(file.parentFile, "ziur-diagnostics.previous.log")
                previous.delete()
                file.renameTo(previous)
            }
            file.appendText(line)
        }.onFailure { Log.e(TAG, "Failed writing diagnostic log", it) }
    }

    fun read(): String = runCatching {
        val file = logFile
        val previous = File(file?.parentFile, "ziur-diagnostics.previous.log")
            .takeIf { it.exists() }?.readText().orEmpty()
        val current = file?.takeIf { it.exists() }?.readText().orEmpty()
        (previous + current).takeLast(24000)
    }.getOrElse { "读取日志失败: " + it.message }
}
