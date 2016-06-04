package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

/**
 * Created by neeraj on 2/6/16.
 */
public class WidgetBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(
                context.getResources().getString(R.string.intent_action_trigger_alarm))) {
            PowerManager powerManager =
                    (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock =
                    powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "partial_lock");
            wakeLock.acquire();
            Intent serviceIntent = new Intent(context, StockIntentService.class);
            serviceIntent.putExtra("tag", "periodic");
            context.startService(serviceIntent);
            wakeLock.release();
        } else if (intent.getAction().equals(
                context.getResources().getString(R.string.intent_action_change_currency))) {
            WidgetDataProvider.mIsPercentWidget = !WidgetDataProvider.mIsPercentWidget;
            Log.v("Percentchane:", "changed");
            int widgetIDs[] = AppWidgetManager
                    .getInstance(context)
                    .getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
            AppWidgetManager.getInstance(context)
                    .notifyAppWidgetViewDataChanged(widgetIDs, R.id.widget_listview);
        }
    }
}
