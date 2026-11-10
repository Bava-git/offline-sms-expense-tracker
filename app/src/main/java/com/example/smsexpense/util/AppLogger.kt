package com.example.smsexpense.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private fun file(ctx: Context): File = File(ctx.getExternalFilesDir(null), "logs/app.log").apply { parentFile?.mkdirs() }
    fun log(ctx: Context, tag: String, msg: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        file(ctx).appendText("$ts [$tag] $msg\n")
        android.util.Log.d(tag, msg)
    }
    fun read(ctx: Context): String = runCatching { file(ctx).readText() }.getOrDefault("")
    fun clear(ctx: Context) = runCatching { file(ctx).writeText("") }
}
