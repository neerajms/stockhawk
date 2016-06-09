package com.sam_chordas.android.stockhawk.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Intent intent = new Intent(context, WidgetBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        //For updating the widget every hour even if the app is not open
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetBroadcastReceiver.class);
        intent.setAction(context.getResources().getString(R.string.intent_action_trigger_alarm));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000 * 3, 3600000, pendingIntent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        //For storing user preference on widget deletion which can be restored next time
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getResources().getString(R.string.app_shared_preference), context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getResources().getString(R.string.key_is_percentage_widget),
                WidgetDataProvider.mIsPercentWidget);
        editor.commit();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int widgetId : appWidgetIds) {
            RemoteViews mView = initViews(context, appWidgetManager, widgetId);
            Intent intent = new Intent(context, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            mView.setOnClickPendingIntent(R.id.widget_frame, pendingIntent);

            //For switching between change in terms of value and percentage
            Intent changeCurrencyIntent = new Intent(context, WidgetBroadcastReceiver.class);
            changeCurrencyIntent.setAction(
                    context.getResources().getString(R.string.intent_action_change_currency));
            PendingIntent changeCurrencyPendingIntent =
                    PendingIntent.getBroadcast(context, 0, changeCurrencyIntent, 0);
            mView.setOnClickPendingIntent(R.id.change_widget, changeCurrencyPendingIntent);

            //Opens the graph corresponding to the item selected
            Intent itemIntent = new Intent(context, StockDetailsActivity.class);
            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(itemIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mView.setPendingIntentTemplate(R.id.widget_listview, clickPendingIntentTemplate);

            setRemoteAdapter(context, mView);
            appWidgetManager.updateAppWidget(widgetId, mView);
        }
    }

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
}
