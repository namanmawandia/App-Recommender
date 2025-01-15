package com.example.apprecordcollector

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.apprecordcollector.Widget.Companion.updateAppWidget
import java.time.LocalTime
import kotlin.math.sqrt

var sortedList : List<Pair<String,Double>> = listOf()

class Widget: AppWidgetProvider() {
    @SuppressLint("InlinedApi")

    private val sharedPrefName = "widget_pref"
    private val sharedPrefKey = "widget_created"

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        val prefs = context.getSharedPreferences(sharedPrefName,Context.MODE_PRIVATE)
        val widgetCreated = prefs.getBoolean(sharedPrefKey,false)
        if(!widgetCreated){
            prefs.edit().putBoolean(sharedPrefKey,true).apply()
            Log.d("onEnabled", "onEnabled: Widget Created")
        }else{
            Log.d("onEnabled", "onEnabled: Only one widget allowed")
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)

        val prefs = context.getSharedPreferences(sharedPrefName,Context.MODE_PRIVATE)
        prefs.edit().putBoolean(sharedPrefKey,false).apply()
        Log.d("onDisabled", "onDisabled: Widget removed, flag reset")
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        lastApp.clear()
        cosineSimVal.clear()
        appTimeMap.clear()

        Log.d("onUpdate", "onUpdate: inside onUpdate")

        val worker =  OneTimeWorkRequestBuilder<AppWorker>().build()
        WorkManager.getInstance(context).enqueue(worker)

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

            Log.d("onUpdate", "updateAppWidget: Before assigning values $sortedList")
            views.setImageViewBitmap(ivApp1,getAppIcon(context,sortedList[0].first))
            views.setImageViewBitmap(ivApp2,getAppIcon(context,sortedList[1].first))
            views.setImageViewBitmap(ivApp3,getAppIcon(context,sortedList[2].first))

            val intent = Intent(context, Widget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val pendingIntent = PendingIntent.getBroadcast(context, 0,intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.ivApp3,pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId,views)

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

}

class mainActivityBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.AppRecordCollector.Worker_Complete") {

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName =  ComponentName(context, Widget:: class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            Log.d("onUpdate", "onReceive: $lastApp")
            cosineSimVal = findCosine()
            cosineSimVal.remove(lastApp[0])
            sortedList = cosineSimVal.toList().sortedByDescending { (_, value) -> value }
            Log.d("onUpdate", "Sorted List: $sortedList")
            cosineSimVal = sortedList.toMap().toMutableMap()

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, sortedList)
            }
        }
    }

    fun findCosine():MutableMap<String,Double>{
        val cosineSimVal: MutableMap<String, Double> = mutableMapOf<String, Double>().withDefault {0.0}
        Log.d("cosineSimilarityWidget", "findCosine: ${lastApp}")
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
        Log.d("cosineSimilarityWidget", "findCosine: $cosineSimVal")

        return cosineSimVal
    }
}
