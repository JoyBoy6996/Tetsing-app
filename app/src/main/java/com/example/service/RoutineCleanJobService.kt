package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.HyperPulseDatabase
import com.example.data.PreferencesManager
import com.example.engine.BoostEngine
import com.example.engine.CleanerEngine
import com.example.engine.JunkCategory
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoutineCleanJobService : JobService() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val JOB_ID = 3001
        const val NOTIFICATION_CHANNEL_ID = "hyperpulse_routine_clean_channel"
        const val NOTIFICATION_ID = 3002
        private const val TAG = "RoutineCleanJobService"

        fun schedule(context: Context) {
            try {
                val prefs = PreferencesManager(context)
                if (!prefs.routineCleanEnabled.value) {
                    cancel(context)
                    return
                }

                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(context, RoutineCleanJobService::class.java)

                val intervalDays = prefs.routineCleanIntervalDays.value.coerceAtLeast(1)
                val intervalMillis = intervalDays * 24L * 60L * 60L * 1000L

                val builder = JobInfo.Builder(JOB_ID, component).apply {
                    setPeriodic(intervalMillis)
                    try {
                        setPersisted(true)
                    } catch (_: Exception) { }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && prefs.routineCleanIdleOnly.value) {
                        setRequiresDeviceIdle(true)
                    }
                    if (prefs.routineCleanChargingOnly.value) {
                        setRequiresCharging(true)
                    }
                }

                val result = scheduler.schedule(builder.build())
                if (result == JobScheduler.RESULT_SUCCESS) {
                    Log.d(TAG, "Routine clean job scheduled successfully (every $intervalDays day(s))")
                } else {
                    Log.w(TAG, "Failed to schedule routine clean job")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error scheduling routine clean job: ${e.message}")
            }
        }

        fun cancel(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            scheduler?.cancel(JOB_ID)
            Log.d(TAG, "Routine clean job cancelled")
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "Executing scheduled low-usage routine clean task")

        scope.launch {
            try {
                val db = HyperPulseDatabase.getDatabase(applicationContext)
                val shizukuManager = ShizukuManager(applicationContext)
                val boostEngine = BoostEngine(applicationContext, db.boostDao(), shizukuManager)
                val cleanerEngine = CleanerEngine(applicationContext, db.boostDao())
                val prefs = PreferencesManager(applicationContext)

                // 1. Process Memory Optimization
                val boostResult = boostEngine.executeBoost("Scheduled Low-Usage Routine")

                // 2. Cache & Temporary Residual Clean
                val scanResult = cleanerEngine.scanJunk()
                val targetCategories = setOf(JunkCategory.CACHE, JunkCategory.TEMP_LOGS)
                val storageFreedBytes = cleanerEngine.cleanSelected(targetCategories, scanResult.items)
                val storageFreedMb = (storageFreedBytes / (1024 * 1024)).coerceAtLeast(0)

                val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
                val summary = "Cleaned ${boostResult.ramFreedMb} MB RAM, ${storageFreedMb} MB storage on $timeStr"
                prefs.setLastRoutineCleanSummary(summary)

                postCompletionNotification(boostResult.ramFreedMb, storageFreedMb)
                Log.i(TAG, "Routine clean completed: $summary")
            } catch (e: Exception) {
                Log.e(TAG, "Error executing routine clean", e)
            } finally {
                jobFinished(params, false)
            }
        }

        return true // Work is running asynchronously on background scope
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceJob.cancel()
        Log.w(TAG, "Routine clean job interrupted (e.g., device no longer idle)")
        return true // Reschedule if condition was interrupted
    }

    private fun postCompletionNotification(ramFreedMb: Long, storageFreedMb: Long) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Routine Maintenance",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Summaries of scheduled low-usage clean tasks"
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Routine Clean Complete")
            .setContentText("Freed $ramFreedMb MB RAM and $storageFreedMb MB storage during idle time")
            .setSmallIcon(com.example.R.drawable.ic_stat_boost)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
