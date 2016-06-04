package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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
        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra("tag").equals("add")) {
            args.putString("symbol", intent.getStringExtra("symbol"));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        res = 10;
        Log.v("STock task serv:", String.valueOf(res));
        res = stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
        Log.v("StockTask serv:", String.valueOf(res));
//        ProgressDialog progress = new ProgressDialog(this);
//        progress.setMessage("Downloading Music");
//        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progress.setIndeterminate(true);
//        progress.setProgress(0);
//        progress.show();
//        int jumpTime = 0;
//        while (stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args)) == 10) {
//            jumpTime = jumpTime + 5;
//            progress.setProgress(jumpTime);
//        }
        if (res == 0) {
            int widgetIDs[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), WidgetProvider.class));
            AppWidgetManager.getInstance(getApplication()).notifyAppWidgetViewDataChanged(widgetIDs, R.id.widget_listview);
            Log.v("update triggered act", "1");
        }
        Log.d("JBJBJBJ", String.valueOf(res));
        if (MyStocksActivity.mProgress != null) {
            MyStocksActivity.mProgress.dismiss();
        }
    }
}
