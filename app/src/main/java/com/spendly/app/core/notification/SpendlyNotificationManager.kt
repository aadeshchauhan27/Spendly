package com.spendly.app.core.notification

import android.Manifest
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
import com.spendly.app.MainActivity
import com.spendly.app.R
import com.spendly.app.domain.model.formatPaiseToRupees

object SpendlyNotificationManager {

    const val CHANNEL_ID_SPENDING = "spendly_spending_alerts"
    const val CHANNEL_NAME_SPENDING = "Spending Summaries & Alerts"

    const val NOTIFICATION_ID_DAILY = 1001
    const val NOTIFICATION_ID_WEEKLY = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SPENDING,
                CHANNEL_NAME_SPENDING,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily and weekly spending summaries to help you stay on budget"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDailySpendingAlert(
        context: Context,
        spentPaise: Long,
        transactionCount: Int = 0
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = formatPaiseToRupees(spentPaise)
        val title = if (spentPaise > 0) "Daily Spending: $formattedAmount" else "No Spending Today 🎉"
        val message = if (spentPaise > 0) {
            if (transactionCount > 0) {
                "You've spent $formattedAmount today across $transactionCount transactions. Tap to review."
            } else {
                "You've spent $formattedAmount today. Tap to see detailed breakdown."
            }
        } else {
            "Zero expenses recorded today! Great job saving."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SPENDING)
            .setSmallIcon(R.drawable.ic_notification_spendly)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY, notification)
    }

    fun showWeeklySpendingAlert(
        context: Context,
        spentPaise: Long,
        transactionCount: Int = 0
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = formatPaiseToRupees(spentPaise)
        val title = "Weekly Spending: $formattedAmount"
        val message = if (transactionCount > 0) {
            "You spent $formattedAmount over the last 7 days across $transactionCount transactions. Tap to see insights."
        } else {
            "You spent $formattedAmount over the last 7 days. Tap to check your category breakdown."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SPENDING)
            .setSmallIcon(R.drawable.ic_notification_spendly)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WEEKLY, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
