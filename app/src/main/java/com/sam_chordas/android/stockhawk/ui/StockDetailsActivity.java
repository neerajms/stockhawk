package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

//import com.example.sam_chordas.stockhawk.R;

public class StockDetailsActivity extends AppCompatActivity {

    String baseUrl = "https://query.yahooapis.com/v1/public/yql";
    String search = "format";
    String search_val = "json";
    String query_key = "q";
    String dia = "diagnostics";
    String dia_val = "true";
    String env = "env";
    String env_val = "store://datatables.org/alltableswithkeys";
    String call = "callback";
    String call_val = "";
    String mStockSymbol;
    String mDateLabelStart;
    String mDateLabelEnd;
    Uri uri;

    public StockDetailsActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat labelDateFormat = new SimpleDateFormat("dd-MM");

        String currentDateAsString = dateFormat.format(currentDate);
        mDateLabelEnd = labelDateFormat.format(currentDate);
        currentDateAsString = "'" + currentDateAsString + "'";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -60);
        String startDate = dateFormat.format(cal.getTime());
        mDateLabelStart = labelDateFormat.format(cal.getTime());
        startDate = "'" + startDate + "'";

        Log.v("start and end:", mDateLabelStart + " " + mDateLabelEnd);
        Log.d("DAte:::::", currentDateAsString + "   " + startDate);

        Intent intent = getIntent();
        mStockSymbol = intent.getExtras().get("stock_symbol").toString();
        String query = "select * from yahoo.finance.historicaldata where symbol ='"
                + mStockSymbol + "' and startDate = " + startDate + " and endDate = " + currentDateAsString;

        setTitle(mStockSymbol);
        uri = Uri.parse(baseUrl).buildUpon()
                .appendQueryParameter(query_key, query)
                .appendQueryParameter(search, search_val)
                .appendQueryParameter(dia, dia_val)
                .appendQueryParameter(env, env_val)
                .appendQueryParameter(call, call_val).build();
        Log.v("Url", uri.toString());
        AsyncTaskGraph asyncTaskGraph = new AsyncTaskGraph();
        asyncTaskGraph.execute(uri.toString());

//        Toast.makeText(this, mStockSymbol, Toast.LENGTH_SHORT).show();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public class AsyncTaskGraph extends AsyncTask<String, String, String> {
        LineChart chart = (LineChart) findViewById(R.id.chart);
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<String>();

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String response;
            try {
                URL url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    return null;
                }
                response = buffer.toString();
                Log.d("RESPONSE:::::", response);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject jsonObject1 = jsonObject.getJSONObject("query");
                    JSONObject jsonObject2 = jsonObject1.getJSONObject("results");
                    JSONArray jsonArray = jsonObject2.getJSONArray("quote");

                    int index = 0;
                    Log.v("Length Graph::",String.valueOf(jsonArray.length()));
                    for (int i = jsonArray.length()-1; i >= 0 ; i--) {
//                        if (jsonArray.length() > 7 && i == 0) {
//                            continue;
//                        }
                        JSONObject jsonObject3 = jsonArray.getJSONObject(i);
                        entries.add(new Entry(Float.parseFloat(jsonObject3.getString("Adj_Close")), index));
                        index = index + 1;
                    }
//                    String day = mDateLabelStart.substring(0, 2);
//                    int dayInt = Integer.parseInt(day);
//                    String monthStart = mDateLabelStart.substring(3, 5);
//                    String monthEnd = mDateLabelEnd.substring(3, 5);
//                    String month;
//                    if (monthEnd.equals(monthStart)) {
//                        month = monthStart;
//                    } else {
//                        month = monthEnd;
//                    }
//                    Date startDate = null;
//                    SimpleDateFormat labelDateFormat = new SimpleDateFormat("dd-MM");
//                    try {
//                        startDate = labelDateFormat.parse(mDateLabelStart);
//                    } catch (ParseException e) {

//                    }
                    String newDate = mDateLabelStart;
                    for (int i = 0; i < jsonArray.length(); i++) {
//                        if (jsonArray.length() > 7 && i == 0) {
//                            dayInt = dayInt + 1;
//                            continue;
//                        }
                        if (i == jsonArray.length() - 1){
                            labels.add("Yesterday");
                        }else {
                            labels.add(String.valueOf(i + 1));
                        }

//                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM");
//                        Calendar c = Calendar.getInstance();
//                        try {
//
//                        c.setTime(sdf.parse(newDate));
//                        }catch (ParseException e){
//
//                        }
//                        c.add(Calendar.DATE, 1);  // number of days to add
//                        newDate = sdf.format(c.getTime());
                    }

                } catch (JSONException e) {
                }
            } catch (IOException e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            LineDataSet lineDataSet = new LineDataSet(entries, "test values");
            lineDataSet.setDrawCircles(true);
            lineDataSet.setDrawValues(true);
            lineDataSet.setValueTextColor(R.color.chart_font_white);

            LineData data = new LineData(labels, lineDataSet);
            chart.setDescription("Stock Values");
            chart.setData(data);
            chart.animateY(0);

            super.onPostExecute(s);
        }
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {

        return super.onCreateView(name, context, attrs);
    }
}
