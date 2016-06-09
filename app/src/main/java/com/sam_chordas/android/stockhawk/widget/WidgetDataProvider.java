package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

public class WidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private Cursor mCursor;
    private Context mContext;
    public static boolean mIsPercentWidget = true;

    WidgetDataProvider(Context context, Intent intent) {
        mContext = context;
    }

    @Override
    public void onCreate() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                mContext.getResources().getString(R.string.app_shared_preference),
                Context.MODE_PRIVATE);
        mIsPercentWidget = sharedPreferences.getBoolean(
                mContext.getResources().getString(R.string.key_is_percentage_widget), true);
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        final long identityToken = Binder.clearCallingIdentity();
        mCursor = mContext.getContentResolver().query(
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{
                        QuoteColumns._ID,
                        QuoteColumns.SYMBOL,
                        QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE,
                        QuoteColumns.CHANGE,
                        QuoteColumns.ISUP
                },
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == AdapterView.INVALID_POSITION ||
                mCursor == null || !mCursor.moveToPosition(position)) {
            return null;
        }

        RemoteViews views = new RemoteViews(mContext.getPackageName(),
                R.layout.list_item_quote_widget);

        views.setTextViewText(R.id.stock_symbol,
                mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL)));
        views.setTextViewText(R.id.bid_price,
                mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE)));

        if (mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.ISUP)) == 1) {
            views.setInt(R.id.change,
                    mContext.getResources().getString(R.string.string_set_background_resource),
                    R.drawable.percent_change_pill_green);
        } else {
            views.setInt(R.id.change,
                    mContext.getResources().getString(R.string.string_set_background_resource),
                    R.drawable.percent_change_pill_red);
        }

        if (mIsPercentWidget) {
            views.setTextViewText(R.id.change,
                    mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
        } else {
            views.setTextViewText(R.id.change,
                    mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE)));
        }

        //For opening the graph on clicking a stock on the widget
        final Intent fillInIntent = new Intent();
        fillInIntent.putExtra(mContext.getResources().getString(R.string.key_stock_symbol),
                mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL)));
        views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            final int QUOTES_ID_COL = 0;
            return mCursor.getLong(QUOTES_ID_COL);
        }
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}