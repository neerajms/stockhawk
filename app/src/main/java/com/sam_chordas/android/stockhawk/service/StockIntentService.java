package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.widget.WidgetProvider;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

    public int res;

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra(getResources().getString(R.string.tag))
                .equals(getResources().getString(R.string.add))) {
            args.putString(getResources().getString(R.string.key_stock_symbol),
                    intent.getStringExtra(getResources().getString(R.string.key_stock_symbol)));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        res = stockTaskService.onRunTask(new TaskParams(
                intent.getStringExtra(getResources().getString(R.string.tag)),
                args));

        if (res == 0) {
            int widgetIDs[] = AppWidgetManager.getInstance(getApplication())
                    .getAppWidgetIds(new ComponentName(getApplication(), WidgetProvider.class));
            AppWidgetManager.getInstance(getApplication())
                    .notifyAppWidgetViewDataChanged(widgetIDs, R.id.widget_listview);
        }
        if (MyStocksActivity.mProgress != null) {
            MyStocksActivity.mProgress.dismiss();
        }
    }
}
