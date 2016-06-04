package com.sam_chordas.android.stockhawk.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.StockDetailsActivity;

/**
 * Created by neeraj on 17/5/16.
 */
public class WidgetProvider extends AppWidgetProvider {

//    private static AppWidgetManager mAppWidgetManager;
//    private static int[] mAppWidgetIds;
//    private static Context mContext;


    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Intent intent = new Intent(context,WidgetBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent,0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context,WidgetBroadcastReceiver.class);
        intent.setAction("com.sam_chordas.android.stockhawk.widget.TRIGGER_ALARM");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent,0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 3,20000,pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

//        String symbol = intent.getExtras().getString("stock_symbol");
//        Intent intentDetails = new Intent(context, StockDetailsActivity.class);
//        intentDetails.putExtra("stock_symbol",symbol);
//        intentDetails.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intentDetails);
//        if (intent.getAction().equals("com.sam_chordas.android.stockhawk.widget.CHANGE_CURRENCY")){
//            WidgetDataProvider.mIsPercent = !WidgetDataProvider.mIsPercent;
//            Log.v("Percentchane:","changed");
//            int widgetIDs[] = AppWidgetManager
//                    .getInstance(context)
//                    .getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
//            AppWidgetManager.getInstance(context)
//                    .notifyAppWidgetViewDataChanged(widgetIDs, R.id.widget_listview);
//        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
//        mAppWidgetManager = appWidgetManager;
//        mAppWidgetIds = appWidgetIds;
        for (int widgetId : appWidgetIds) {
            RemoteViews mView = initViews(context, appWidgetManager, widgetId);
            Intent intent = new Intent(context, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            mView.setOnClickPendingIntent(R.id.widget_frame, pendingIntent);

            Intent changeCurrencyIntent = new Intent(context,WidgetBroadcastReceiver.class);
            changeCurrencyIntent.setAction("com.sam_chordas.android.stockhawk.widget.CHANGE_CURRENCY");
            PendingIntent changeCurrencyPendingIntent =
                    PendingIntent.getBroadcast(context,0,changeCurrencyIntent,0);
            mView.setOnClickPendingIntent(R.id.change_widget,changeCurrencyPendingIntent);

//            Intent pendIntent = new Intent(context, StockDetailsActivity.class);
//            PendingIntent pendingIntent1 = PendingIntent.getActivity(context,0,pendIntent,0);
//            mView.setPendingIntentTemplate(R.id.widget_list_item,pendingIntent1);
            Intent itemIntent = new Intent(context, StockDetailsActivity.class);
            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(itemIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//            PendingIntent pendingIntentItem = PendingIntent.getBroadcast(context,0,itemIntent,PendingIntent.FLAG_UPDATE_CURRENT);
            mView.setPendingIntentTemplate(R.id.widget_listview,clickPendingIntentTemplate);
            setRemoteAdapter(context, mView);
            appWidgetManager.updateAppWidget(widgetId, mView);
//            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_listview);
        }
    }

//    public static void updateWidget(Context context){
//        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(mAppWidgetIds,R.id.widget_listview);
//    }

    private RemoteViews initViews(Context context,
                                  AppWidgetManager widgetManager, int widgetId) {

        RemoteViews mView = new RemoteViews(context.getPackageName(),
                R.layout.widget_layout);

        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        mView.setRemoteAdapter(widgetId, R.id.widget_listview, intent);

        return mView;
    }

    private void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(R.id.widget_listview,
                new Intent(context, WidgetService.class));
    }

    public void changeCurrency(){

    }
}
