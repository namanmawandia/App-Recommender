package com.example.apprecordcollector

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalTime
import kotlin.math.sqrt

class Widget: AppWidgetProvider() {
    @SuppressLint("InlinedApi")
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val worker =  OneTimeWorkRequestBuilder<AppWorker>().build()
        WorkManager.getInstance(context).enqueue(worker)

        var sortedList : List<Pair<String,Double>> = listOf()
        val receiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context, intent: Intent) {
                if(intent.action == "com.AppRecordCollector.Worker_Complete")
                {
                    cosineSimVal = findCosine()
                    cosineSimVal.remove(lastApp[0])
                    Log.d("OneTimeRequest", "Cosine similarity: ${cosineSimVal.isEmpty()}")
                    sortedList = cosineSimVal.toList().sortedByDescending { (_,value)-> value }
                    cosineSimVal = sortedList.toMap().toMutableMap()
                }
            }
        }

        val filter = IntentFilter("com.AppRecordCollector.Worker_Complete")
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)


        for(appWidgetId in appWidgetIds)
        {
            updateAppWidget(context,appWidgetManager,appWidgetId, sortedList)
        }
    }
    companion object{
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            sortedList: List<Pair<String, Double>>)
        {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val ivApp1 = R.id.ivApp1
            val ivApp2 = R.id.ivApp2
            val ivApp3 = R.id.ivApp3

            views.setImageViewBitmap(ivApp1,getAppIcon(context,sortedList[0].first))
            views.setImageViewBitmap(ivApp2,getAppIcon(context,sortedList[1].first))
            views.setImageViewBitmap(ivApp3,getAppIcon(context,sortedList[2].first))


            val intent = Intent(context, Widget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val pendingIntent = PendingIntent.getBroadcast(context, 0,intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            views

        }

        fun getAppIcon(context: Context, packageName: String): Bitmap {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val drawable = packageManager.getApplicationIcon(applicationInfo)
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }
    }

    fun findCosine():MutableMap<String,Double>{
        val cosineSimVal: MutableMap<String, Double> = mutableMapOf<String, Double>().withDefault {0.0}
        Log.d("cosineSimilarity", "findCosine: ${lastApp}")
        for(lapp in lastApp) {
            val appA = appTimeMap[lapp]
            val time = LocalTime.now().hour
            appA!![time] = appA[time]*(3-lastApp.indexOf(lapp))
            val magA = sqrt(appA.sumOf { it * it }.toDouble())
            for ((app, list) in appTimeMap) {
                val dotProd = appA.zip(list).sumOf { (a, b) -> a * b * 1.0 }
                val magB = sqrt(list.sumOf { it * it }.toDouble())
                cosineSimVal[app] = cosineSimVal.getValue(app) +
                        (if (magA == 0.0 || magB == 0.0) 0.0
                        else (dotProd * 1.0) / (magA * magB))
            }
        }
        cosineSimVal.forEach{(key,value)-> cosineSimVal[key] = value/3.0}
        Log.d("cosineSimilarity", "findCosine: $cosineSimVal")

        return cosineSimVal
    }
}