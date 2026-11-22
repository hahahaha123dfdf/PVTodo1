package com.yg.pvtodo

import android.app.Application
import com.yg.pvtodo.notifications.NotificationHelper
import com.yg.pvtodo.notifications.DailyPaperWorker
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        DailyPaperWorker.schedule(this, 9, 0, TimeUnit.HOURS.toMinutes(24))
    }
}
