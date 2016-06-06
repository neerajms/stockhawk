package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    private static Context mContext;

    public static ArrayList quoteJsonToContentVals(String JSON, Context context) {
        mContext = context;
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject(
                        context.getResources().getString(R.string.query));
                int count = Integer.parseInt(jsonObject.getString(
                        context.getResources().getString(R.string.count)));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject(
                            context.getResources().getString(R.string.results))
                            .getJSONObject(context.getResources().getString(R.string.quote));
                    Log.d("JSONOBJECT::::",
                            jsonObject.getString(context.getResources().getString(R.string.symbol)));
                    if (!jsonObject.get(context.getResources().getString(R.string.bid)).equals(null)) {
                        batchOperations.add(buildBatchOperation(jsonObject));
                    } else {
                        Intent intent = new Intent(context.getResources()
                                .getString(R.string.invalid_stock_intent_filter));
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                } else {
                    resultsArray = jsonObject.getJSONObject(
                            context.getResources().getString(R.string.results))
                            .getJSONArray(context.getResources().getString(R.string.quote));

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, mContext.getResources().getString(R.string.string_to_json_failed) + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString(mContext.getResources().getString(R.string.change));
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(
                    mContext.getResources().getString(R.string.symbol)));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(
                    jsonObject.getString(mContext.getResources().getString(R.string.bid))));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString(mContext.getResources().getString(R.string.change_in_percent)), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }
}
