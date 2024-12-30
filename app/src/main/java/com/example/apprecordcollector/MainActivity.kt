package com.example.apprecordcollector

import android.app.AppOpsManager
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val btnStart = findViewById<Button>(R.id.btnStart)
//        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnFetch = findViewById<Button>(R.id.btnFetch)

        if(!hasUsageStatsPermission(this)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
            if(hasUsageStatsPermission(this)) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"Permission Not granted",Toast.LENGTH_SHORT).show()
            }
        }else{
            Log.d("onCreate", "onCreate: permission for UsageStats already granted")
        }

//        btnStart.setOnClickListener{
//            scheduleAppUsageWorker(it.context)
//            Toast.makeText(this, "App Data collection started", Toast.LENGTH_SHORT).show()
//        }
//
//        btnStop.setOnClickListener{
//            WorkManager.getInstance(it.context).cancelAllWorkByTag("AppWorkerStop")
//            Toast.makeText(this, "App data collection stopped", Toast.LENGTH_SHORT).show()
//        }

        btnFetch.setOnClickListener{
            val workRequest = OneTimeWorkRequestBuilder<AppWorker>().build()
            Log.d("OneTimeRequest", "onCreate: one time request created")
            WorkManager.getInstance(it.context).enqueue(workRequest)
            Log.d("OneTimeRequest", "onCreate: one time request done")
            copyCSVToDownloads(this,"app_usage_data.csv")
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

//    fun scheduleAppUsageWorker(context: Context) {
//        val workRequest = PeriodicWorkRequestBuilder<AppWorker>(15, TimeUnit.MINUTES)
//            .addTag("AppWorkerStop").build()
//        WorkManager.getInstance(context).enqueue(workRequest)
//    }

    fun copyCSVToDownloads(context: Context, fileName: String) {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            val sourceFile = File(context.filesDir, fileName)
            contentResolver.openOutputStream(it)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            println("File saved to Downloads using MediaStore: $uri")
            Toast.makeText(this, "File saved to downloads", Toast.LENGTH_SHORT).show()
        } ?: println("Error saving file to Downloads, NUll URI")
    }

}

class AppWorker(context: Context, workerParam:WorkerParameters): Worker(context,workerParam){
    override fun doWork(): Result {
        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 60 * 60 * 24, time)
        Log.d("doWork", "doWork: stats complete ${stats.isNotEmpty()}, ${stats.size}")
        if(stats.isNotEmpty()){
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            for(currentApp in sortedStats.filter { isAllowedApp(it.packageName,applicationContext) }){
                val appName = getAppNameFromPackageName(currentApp.packageName, applicationContext)
                Log.d("doWork", "doWork: ${currentApp.packageName}, $appName")
                logAppUsage(applicationContext,currentApp.packageName, currentApp.lastTimeUsed, appName)
            }
        }
        return Result.success()
    }

    fun isSystemApp(packageName: String, context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false // Treat unknown apps as non-system apps
        }
    }

    fun isLaunchableApp(packageName: String, context: Context): Boolean {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return intent != null
    }

    fun isAllowedApp(packageName: String, context: Context): Boolean {
        return !isSystemApp(packageName, context) && isLaunchableApp(packageName, context)
    }

    fun getAppNameFromPackageName(packageName: String, context: Context): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown App"
        }
    }

    fun logAppUsage(context: Context, packageName: String, lastUsed: Long, appName: String) {
        val formattedTime = formatTime(lastUsed)

        val file = File(context.filesDir, "app_usage_data.csv")
        Log.d("logAppUsage", "logAppUsage: file define complete")
        
        if (!file.exists()) {
            try {
                // Create the file and add headers
                val fileOutputStream = FileOutputStream(file, true)
                val writer = OutputStreamWriter(fileOutputStream)
                writer.append("Package Name,Last Used,App Name\n")  // CSV header row
                writer.close()
                Log.d("logAppUsage", "file writing successful")
            } catch (e: Exception) {
                Log.e("logAppUsage", "Error creating file: $e")
            }
        }

        // Append app usage data to the CSV file
        try {
            val fileOutputStream = FileOutputStream(file, true)
            val writer = OutputStreamWriter(fileOutputStream)
            Log.d("logAppUsage", "logAppUsage: File define for append successful")
            writer.append("$packageName,$formattedTime, $appName\n")  // CSV data row
            writer.close()
            Log.d("logAppUsage", "Logged app usage: $packageName at $formattedTime")
        } catch (e: Exception) {
            Log.e("logAppUsage", "Error writing to file: $e")
        }
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())  // 24-hour format
        val date = Date(timestamp)
        return sdf.format(date)
    }
}

