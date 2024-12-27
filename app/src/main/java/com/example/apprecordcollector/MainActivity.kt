package com.example.apprecordcollector

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnFetch = findViewById<Button>(R.id.btnFetch)

        btnStart.setOnClickListener{
            scheduleAppUsageWorker(it.context)
        }

        btnStop.setOnClickListener{
            WorkManager.getInstance(it.context).cancelAllWorkByTag("AppWorkerStop")
        }

        btnFetch.setOnClickListener{

        }

    }

    fun scheduleAppUsageWorker(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<AppWorker>(15, TimeUnit.MINUTES)
            .addTag("AppWorkerStop").build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

class AppWorker(context: Context, workerParam:WorkerParameters): Worker(context,workerParam){
    override fun doWork(): Result {
        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time)

        if(stats.isNotEmpty()){
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            val currentApp = sortedStats[0]
            logAppUsage(currentApp.packageName, currentApp.lastTimeUsed)
        }
        return Result.success()
    }

    private fun logAppUsage(packageName: String, lastUsed: Long) {
        // Log app usage (you can save this to a database or send it to a server)
        val formattedTime = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUsed))
        android.util.Log.d("AppUsageWorker", "App Opened: $packageName at $formattedTime")
    }
}

