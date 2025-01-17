package com.example.apprecordcollector

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
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

            setAppLaunchOnClick(context,views, ivApp1, sortedList[0].first)
            setAppLaunchOnClick(context,views, ivApp2, sortedList[1].first)
            setAppLaunchOnClick(context,views, ivApp3, sortedList[2].first)

            appWidgetManager.updateAppWidget(appWidgetId,views)

        }

        private fun setAppLaunchOnClick(
            context: Context,
            views: RemoteViews,
            viewId: Int,
            packageName: String
        ) {
            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

            val pendingIntent = PendingIntent.getActivity(context,packageManager.hashCode(),
                launchIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(viewId,pendingIntent)
            Log.d("onUpdate", "setAppLaunchOnClick: Clickable set")

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

            sortedList = listOf()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName =  ComponentName(context, Widget:: class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            Log.d("onUpdate", "onReceive: $lastApp")
            Log.d("onUpdate", "appTimeMap Widget $appTimeMap")
            var cosineSimValWidget = cosineSimVal
            cosineSimValWidget.remove(lastApp[0])
            Log.d("onUpdate", "Cosine Similarity Widget $cosineSimValWidget")
            sortedList = cosineSimValWidget.toList().sortedByDescending { (_, value) -> value }
            Log.d("onUpdate", "Sorted List: $sortedList")
            cosineSimValWidget = sortedList.toMap().toMutableMap()

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, sortedList)
            }
        }
    }
}
