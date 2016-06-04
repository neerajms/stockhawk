package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

/**
 * Created by neeraj on 2/6/16.
 */
public class WidgetBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
//        StockTaskService stockTaskService = new StockTaskService();
//        Bundle args = new Bundle();
        if (intent.getAction().equals("com.sam_chordas.android.stockhawk.widget.TRIGGER_ALARM")) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock =
                    powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "partial_lock");
            wakeLock.acquire();

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);
            ComponentName widgetComponentName = new ComponentName(context, WidgetProvider.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            Intent serviceIntent = new Intent(context, StockIntentService.class);
            serviceIntent.putExtra("tag", "periodic");
            context.startService(serviceIntent);
            wakeLock.release();
        }else if (intent.getAction().equals("com.sam_chordas.android.stockhawk.widget.CHANGE_CURRENCY")){
            WidgetDataProvider.mIsPercentWidget = !WidgetDataProvider.mIsPercentWidget;
            Log.v("Percentchane:","changed");
            int widgetIDs[] = AppWidgetManager
                    .getInstance(context)
                    .getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
            AppWidgetManager.getInstance(context)
                    .notifyAppWidgetViewDataChanged(widgetIDs, R.id.widget_listview);
        }
//        stockTaskService.onRunTask(new TaskParams("periodic"));
//        appWidgetManager.updateAppWidget(widgetComponentName,remoteViews);
//        appWidgetManager.notifyAppWidgetViewDataChanged(
//                appWidgetManager.getAppWidgetIds(widgetComponentName), R.id.widget_listview);

    }
}
