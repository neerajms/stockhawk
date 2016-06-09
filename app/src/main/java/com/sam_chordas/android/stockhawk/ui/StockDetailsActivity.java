package com.sam_chordas.android.stockhawk.ui;

import android.app.ProgressDialog;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
    Context mContext;
    ProgressDialog mProgressDialog;

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

        Intent intent = getIntent();
        mStockSymbol = intent.getExtras().get(getResources().getString(R.string.key_stock_symbol)).toString();

        //Set the stock symbol as the activity title
        setTitle(mStockSymbol);

        String query = "select * from yahoo.finance.historicaldata where symbol ='"
                + mStockSymbol + "' and startDate = " + startDate + " and endDate = " + currentDateAsString;

        uri = Uri.parse(baseUrl).buildUpon()
                .appendQueryParameter(query_key, query)
                .appendQueryParameter(search, search_val)
                .appendQueryParameter(dia, dia_val)
                .appendQueryParameter(env, env_val)
                .appendQueryParameter(call, call_val).build();

        AsyncTaskGraph asyncTaskGraph = new AsyncTaskGraph();
        asyncTaskGraph.execute(uri.toString());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public class AsyncTaskGraph extends AsyncTask<String, String, String> {
        private String LOG_TAG = StockDetailsActivity.class.getSimpleName();
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

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject jsonObject1 = jsonObject.getJSONObject(getString(R.string.query));
                    JSONObject jsonObject2 = jsonObject1.getJSONObject(getString(R.string.results));
                    JSONArray jsonArray = jsonObject2.getJSONArray(getString(R.string.quote));

                    int index = 0;
                    for (int i = jsonArray.length() - 1; i >= 0; i--) {
                        JSONObject jsonObject3 = jsonArray.getJSONObject(i);
                        entries.add(new Entry(Float.parseFloat(jsonObject3.getString(getString(R.string.adj_close))), index));
                        String date = jsonObject3.getString(getString(R.string.date)).substring(5);
                        labels.add(date);
                        index = index + 1;
                    }
                } catch (JSONException e) {
                    Log.v(LOG_TAG, "Error in parsing JSON");
                }
            } catch (IOException e) {
                Log.v(LOG_TAG, "Error in HTTP connection");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage(mContext.getResources().getString(R.string.loading_message));
            mProgressDialog.isIndeterminate();
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            LineDataSet lineDataSet = new LineDataSet(entries,
                    getString(R.string.stock_values));
            lineDataSet.setDrawCircles(false);
            lineDataSet.setDrawCubic(true);
            lineDataSet.setDrawFilled(true);
            lineDataSet.setFillColor(getColor(R.color.material_blue_500));
            lineDataSet.setColor(getColor(R.color.material_blue_500), 220);
            lineDataSet.setFillAlpha(220);
            lineDataSet.setDrawValues(false);

            YAxis yAxisLeft = chart.getAxisLeft();
            yAxisLeft.setTextColor(getColor(R.color.font_white));

            YAxis yAxisRight = chart.getAxisRight();
            yAxisRight.setTextColor(getColor(R.color.font_white));

            XAxis xAxis = chart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setAvoidFirstLastClipping(true);
            xAxis.setSpaceBetweenLabels(0);
            xAxis.setTextColor(getColor(R.color.font_white));
            xAxis.setSpaceBetweenLabels(2);

            LineData data = new LineData(labels, lineDataSet);
            chart.setDescription(getString(R.string.chart_description));
            chart.setData(data);
            chart.animateY(0);
            mProgressDialog.dismiss();

        }
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        mContext = context;
        return super.onCreateView(name, context, attrs);
    }
}

