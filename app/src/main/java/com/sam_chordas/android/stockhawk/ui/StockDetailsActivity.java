package com.sam_chordas.android.stockhawk.ui;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

    private String baseUrl = "https://query.yahooapis.com/v1/public/yql";
    private String search = "format";
    private String search_val = "json";
    private String query_key = "q";
    private String dia = "diagnostics";
    private String dia_val = "true";
    private String env = "env";
    private String env_val = "store://datatables.org/alltableswithkeys";
    private String call = "callback";
    private String call_val = "";
    private String mStockSymbol;

    private Uri uri;
    private Context mContext;
    private ProgressDialog mProgressDialog;
    private ArrayList<String> mLabels;
    private ArrayList<String> mEntriesString;
    private ArrayList<Entry> mEntries;
    private LineChart mChart;
    private Bundle mSavedInstanceState;
    private NetorkReceiver mNetworkReceiver;

    public StockDetailsActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNetworkReceiver = new NetorkReceiver();
        registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mSavedInstanceState = savedInstanceState;
        mEntriesString = new ArrayList<String>();
        mLabels = new ArrayList<String>();
        mEntries = new ArrayList<>();
        mChart = (LineChart) findViewById(R.id.chart);

        if (savedInstanceState != null){
            mStockSymbol = savedInstanceState.getString(getString(R.string.symbol));
            //Set the stock symbol as the activity title
            setTitle(mStockSymbol);
            mLabels = savedInstanceState.getStringArrayList(getString(R.string.labels));
            mEntriesString = savedInstanceState.getStringArrayList(getString(R.string.entries));
            for (int index = 0; index < mEntriesString.size(); index++) {
                mEntries.add(new Entry(Float.parseFloat(mEntriesString.get(index).toString()), index));
            }
            populateChart();
        }else {
            Intent intent = getIntent();
            mStockSymbol = intent.getExtras().get(getResources().getString(R.string.key_stock_symbol)).toString();
            //Set the stock symbol as the activity title
            setTitle(mStockSymbol);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    public class NetorkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isInternetOn(context)) {
                if (mSavedInstanceState == null) {
                    fetchData();
                }
            } else {
                networkToast();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNetworkReceiver);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    public boolean isInternetOn(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            return false;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(getString(R.string.entries), mEntriesString);
        outState.putStringArrayList(getString(R.string.labels), mLabels);
        outState.putString(getString(R.string.symbol), mStockSymbol);
        super.onSaveInstanceState(outState);
    }

    public void fetchData() {
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String currentDateAsString = dateFormat.format(currentDate);
        currentDateAsString = "'" + currentDateAsString + "'";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -60);
        String startDate = dateFormat.format(cal.getTime());
        startDate = "'" + startDate + "'";

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
    }

    public class AsyncTaskGraph extends AsyncTask<String, String, String> {
        private String LOG_TAG = StockDetailsActivity.class.getSimpleName();

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
                    String tempEntry;
                    for (int i = jsonArray.length() - 1; i >= 0; i--) {
                        JSONObject jsonObject3 = jsonArray.getJSONObject(i);
                        tempEntry = jsonObject3.getString(getString(R.string.adj_close));
                        mEntries.add(new Entry(Float.parseFloat(tempEntry), index));
                        mEntriesString.add(tempEntry);
                        String date = jsonObject3.getString(getString(R.string.date)).substring(5);
                        mLabels.add(date);
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
            populateChart();
            mProgressDialog.dismiss();

        }
    }

    public void populateChart() {
        LineDataSet lineDataSet = new LineDataSet(mEntries,
                getString(R.string.stock_values));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawCubic(true);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillColor(getResources().getColor(R.color.material_blue_500));
        lineDataSet.setColor(getResources().getColor(R.color.material_blue_500), 220);
        lineDataSet.setFillAlpha(220);
        lineDataSet.setDrawValues(false);

        YAxis yAxisLeft = mChart.getAxisLeft();
        yAxisLeft.setTextColor(getResources().getColor(R.color.font_white));

        YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setTextColor(getResources().getColor(R.color.font_white));

        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setSpaceBetweenLabels(0);
        xAxis.setTextColor(getResources().getColor(R.color.font_white));
        xAxis.setSpaceBetweenLabels(2);

        LineData data = new LineData(mLabels, lineDataSet);
        mChart.setDescription(getString(R.string.chart_description));
        mChart.setData(data);
        mChart.animateY(0);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        mContext = context;
        return super.onCreateView(name, context, attrs);
    }
}

