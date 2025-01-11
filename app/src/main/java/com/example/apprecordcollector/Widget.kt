package com.example.apprecordcollector

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.RemoteViews
import androidx.core.content.ContextCompat.registerReceiver
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.apprecordcollector.lastApp
import com.example.apprecordcollector.appTimeMap

class Widget: AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val worker =  OneTimeWorkRequestBuilder<AppWorker>().build()
        WorkManager.getInstance(context).enqueue(worker)

        val receiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context, intent: Intent) {
                if(intent.action == "com.AppRecordCollector.Worker_Complete")
                {
                    // use lastApp and appTimeMap
                }
            }
        }

        val filter = IntentFilter("com.AppRecordCollector.Worker_Complete")
//        context.registerReceiver( filter,receiver)


        for(appWidgetId in appWidgetIds)
        {
            updateAppWidget(context,appWidgetManager,appWidgetId)
        }
    }
    companion object{
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int)
        {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val ivApp1 = R.id.ivApp1
            val ivApp2 = R.id.ivApp2
            val ivApp3 = R.id.ivApp3




            val intent = Intent(context, Widget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val pendingIntent = PendingIntent.getBroadcast(context, 0,intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            views

        }
    }
}