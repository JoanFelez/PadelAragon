package com.padelaragon.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.padelaragon.shared.domain.notification.NotificationEvent
import java.util.concurrent.TimeUnit

/** Periodic poll: sync + change detection for favourite groups, posting one notification per event. */
class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as PadelAragonApp).multiLeagueContainer
        val events = container.resultNotifier.check()
        if (events.isNotEmpty()) postNotifications(applicationContext, events)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "match_results"
        private const val WORK_NAME = "result_notifications"

        /** Schedules the periodic job when the toggle is on, cancels it otherwise. */
        fun reschedule(context: Context, enabled: Boolean, intervalHours: Int) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(intervalHours.toLong(), TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(CHANNEL_ID, "Resultados", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Nuevos resultados y cambios de clasificación de tus grupos favoritos"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun hasPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        @SuppressLint("MissingPermission") // hasPermission() is checked first
        private fun postNotifications(context: Context, events: List<NotificationEvent>) {
            if (!hasPermission(context)) return
            ensureChannel(context)
            val tap = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val manager = NotificationManagerCompat.from(context)
            events.forEach { event ->
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(event.title)
                    .setContentText(event.message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(event.message))
                    .setContentIntent(tap)
                    .setAutoCancel(true)
                    .build()
                runCatching { manager.notify(event.hashCode(), notification) }
            }
        }
    }
}
