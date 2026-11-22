package com.yg.pvtodo.notifications

import android.content.Context
import androidx.work.*
import com.yg.pvtodo.db.AppDatabase
import com.yg.pvtodo.papers.PaperFetcher
import com.yg.pvtodo.papers.PaperRepository
import java.util.concurrent.TimeUnit

class DailyPaperWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val repo = PaperRepository(db.paperDao(), PaperFetcher())
        return try {
            val fresh = repo.refresh()
            if (fresh.isNotEmpty() && NotificationHelper.canNotify(applicationContext)) {
                NotificationHelper.notify(applicationContext, fresh.size, fresh.first().title)
            }
            Result.success()
        } catch (e: Exception) { Result.retry() }
    }

    companion object {
        fun schedule(ctx: Context, hour: Int, minute: Int, intervalMinutes: Long) {
            val now = java.util.Calendar.getInstance()
            val next = (now.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                if (before(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val req = PeriodicWorkRequestBuilder<DailyPaperWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("daily_papers", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
