package com.binbo.glvideo.core.utils

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.PowerManager
import android.os.VibratorManager
import android.view.WindowManager
import com.binbo.glvideo.core.GLVideo.Core.context

val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
val powerManager by lazy { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
val displayManager by lazy { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
val activityManager by lazy { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
val clipboardManager by lazy { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
val vibratorManager by lazy { context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager }
val notificationManager by lazy { context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager }
val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }