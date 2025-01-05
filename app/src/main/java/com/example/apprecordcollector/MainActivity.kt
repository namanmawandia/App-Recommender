package com.example.apprecordcollector

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.work.WorkManager
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
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
import kotlin.math.sqrt

val appTimeMap: MutableMap<String, MutableList<Int>> = mutableMapOf()
var lastApp: String = ""

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnFetch = findViewById<Button>(R.id.btnFetch)
        val ivApp1 = findViewById<ImageView>(R.id.ivApp1)
        val ivApp2 = findViewById<ImageView>(R.id.ivApp2)
        val ivApp3 = findViewById<ImageView>(R.id.ivApp3)


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

        btnFetch.setOnClickListener{
            appTimeMap.clear()
            val workRequest = OneTimeWorkRequestBuilder<AppWorker>().build()
            WorkManager.getInstance(it.context).enqueue(workRequest)
            Log.d("OneTimeRequest", "onCreate: one time request done")

            var cosineSimVal:MutableMap<String, Int> = mutableMapOf()
            WorkManager.getInstance(it.context).getWorkInfoByIdLiveData(workRequest.id)
                .observe(this) { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    cosineSimVal = findCosine()
                    Log.d("OneTimeRequest", "Cosine similarity: ${cosineSimVal.isEmpty()}")
                    cosineSimVal= sortAndSet(cosineSimVal, ivApp1,ivApp2, ivApp3)
                }
            }
            copyCSVToDownloads(this,"app_usage_data.csv")
        }
    }

    private fun sortAndSet(cosineSimVal: MutableMap<String, Int>,
                           ivApp1: ImageView?,
                           ivApp2: ImageView?,
                           ivApp3: ImageView?): MutableMap<String, Int> {
        val sortedList = cosineSimVal.toList().sortedByDescending { (_,value)-> value }
        val sortedAppMap = sortedList.toMap().toMutableMap()
        ivApp1?.setImageDrawable(getAppIcon(this,sortedList[0].first))
        ivApp2?.setImageDrawable(getAppIcon(this,sortedList[1].first))
        ivApp3?.setImageDrawable(getAppIcon(this,sortedList[2].first))

        return sortedAppMap
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationIcon(applicationInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    fun findCosine():MutableMap<String,Int>{
        val cosineSimVal: MutableMap<String, Int> = mutableMapOf()
        Log.d("cosineSimilarity", "findCosine: ${lastApp}")
        val appA = appTimeMap[lastApp]
        val magA = sqrt(appA?.sumOf { it * it }?.toDouble()?:0.0)
        for((app,list) in appTimeMap){
            val dotProd = appA?.zip(list)?.sumOf{(a,b) -> a*b} ?:0
            val magB = sqrt(list.sumOf {it * it }.toDouble())
            cosineSimVal[app]= (
                    if (magA == 0.0 || magB == 0.0) 0
                    else
                        (dotProd / (magA * magB)).toInt()
                    )
        }
        Log.d("cosineSimilarity", "findCosine: $cosineSimVal")

        return cosineSimVal
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

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
            time - 1000 * 60 * 60 * 24 * 20, time)
        Log.d("doWork", "doWork: stats complete ${stats.isNotEmpty()}, ${stats.size}")
        if(stats.isNotEmpty()){
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            for(currentApp in sortedStats.filter { it.lastTimeUsed > 0 }){
                if(!isLaunchableApp(currentApp.packageName,applicationContext) || !(currentApp.lastTimeUsed>0))
                    continue
                if(appTimeMap.size==1) {
                    lastApp = currentApp.packageName
                    Log.d("doWork", "doWork: sorted stats size, ${lastApp}")
                }
                val appName = getAppNameFromPackageName(currentApp.packageName, applicationContext)
                Log.d("doWork", "doWork: ${currentApp.packageName}, $appName")

                cosineSimilarityInitilization(currentApp.packageName, currentApp.lastTimeUsed)
                Log.d("cosineSimilarity", "doWork: ${currentApp.packageName}  ${appTimeMap[currentApp.packageName]}")
                logAppUsage(applicationContext,currentApp.packageName, currentApp.lastTimeUsed, appName)
            }
        }
        return Result.success()
    }

    fun cosineSimilarityInitilization(packageName: String, lastUsed: Long){
        val formatedTime = formatTime(lastUsed)
        val hour = formatedTime.substring(11, 13).toInt()
        appTimeMap[packageName]?.let { timelist ->
            if(hour in timelist.indices) {
                timelist[hour] += 1
            }
            else
                Log.d("cosineSimilarity", "cosineSimilarityInitilization: Invalid Index $hour")
        }?:run {
            val list = MutableList(24){0}
            list[hour] += 1
            appTimeMap[packageName] = list
        }
    }

    fun isLaunchableApp(packageName: String, context: Context): Boolean {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return intent != null
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
        val sdf = SimpleDateFormat("dd:MM:yyyy:HH:mm:ss", Locale.getDefault())
            .apply { timeZone = TimeZone.getDefault() }
        val date = Date(timestamp)
        return sdf.format(date)
    }
}

