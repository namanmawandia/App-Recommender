package com.example.apprecordcollector

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class Widget: AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
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

        }
    }
}