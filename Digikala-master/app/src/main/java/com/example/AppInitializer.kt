package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AppInitializer {
    fun initialize(context: Context) {
        // شروع سرویس روبیکا
        val intent = Intent(context, RubikaBotService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        Log.d("AppInitializer", "سرویس روبیکا راه‌اندازی شد")
    }
}