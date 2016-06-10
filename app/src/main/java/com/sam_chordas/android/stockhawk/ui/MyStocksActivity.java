package com.sam_chordas.android.stockhawk.ui;

import android.app.Dialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.sam_chordas.android.stockhawk.widget.WidgetDataProvider;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private static Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private static Context mContext;
    private Cursor mCursor;
    private boolean isConnected;
    private Bundle mSavedInstanceState;
    private NetorkReceiver mNetworkReceiver;
    public static ProgressDialog mProgress;

    public MyStocksActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isConnected = isInternetOn(this);

        mNetworkReceiver = new NetorkReceiver();
        registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (savedInstanceState == null && isConnected) {
            mProgress = new ProgressDialog(this);
            mProgress.setCancelable(false);
            mProgress.setMessage(this.getResources().getString(R.string.loading_message));
            mProgress.isIndeterminate();
        }

        mSavedInstanceState = savedInstanceState;
        mContext = this;
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        setContentView(R.layout.activity_my_stocks);

        SharedPreferences sharedPreferences = getSharedPreferences(
                getString(R.string.app_shared_preference),
                Context.MODE_PRIVATE);
        Utils.showPercent = sharedPreferences.getBoolean(
                getString(R.string.key_is_percentage_app), true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        if (isConnected) {
                            Cursor c = mCursorAdapter.getCursor();
                            c.moveToPosition(position);
                            Intent intent = new Intent(getApplicationContext(), StockDetailsActivity.class);
                            intent.putExtra(mContext.getResources().getString(R.string.key_stock_symbol),
                                    c.getString(c.getColumnIndex(QuoteColumns.SYMBOL)));
                            startActivity(intent);
                        } else {
                            networkToast();
                        }
                    }
                }));
        recyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    //Showing dialog to input stock symbol
                    //Use of dialog fragment helps in retaining dialog on rotation
                    StockInputDialogFrgment dialogFrgment = new StockInputDialogFrgment();
                    dialogFrgment.show(getSupportFragmentManager(),getString(R.string.tag_dialog_fragment));
                } else {
                    networkToast();
                }
            }
        });


        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = getString(R.string.periodic);
            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }

    //Dialog fragment to retain stock input dialog on rotation
    public static class StockInputDialogFrgment extends DialogFragment{

        public StockInputDialogFrgment(){}

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

           return new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                    .content(R.string.content_test)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .autoDismiss(true)
                    .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog dialog, CharSequence input) {
                            // On FAB click, receive user input. Make sure the stock doesn't already exist
                            // in the DB and proceed accordingly
                            String inputCaps = input.toString().toUpperCase();
                            inputCaps = inputCaps.replaceAll("\\s", "");
                            Cursor c = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                    new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                    new String[]{inputCaps}, null);
                            if (c.getCount() != 0) {
                                Toast toast =
                                        Toast.makeText(mContext,
                                                getString(R.string.stock_already_saved),
                                                Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                toast.show();
                                return;
                            } else {
                                // Add the stock to DB
                                mServiceIntent.putExtra(
                                        mContext.getResources().getString(R.string.tag),
                                        mContext.getResources().getString(R.string.add));
                                mServiceIntent.putExtra(mContext.getResources()
                                                .getString(R.string.key_stock_symbol),
                                        inputCaps);
                                mContext.startService(mServiceIntent);
                            }
                        }
                    })
                    .build();
        }
    }

    public class NetorkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isInternetOn(context)) {
                isConnected = true;
                // The intent service is for executing immediate pulls from the Yahoo API
                // GCMTaskService can only schedule tasks, they cannot execute immediately
                mServiceIntent = new Intent(context, StockIntentService.class);
                if (mSavedInstanceState == null) {
                    if (mProgress != null) {
                        mProgress.show();
                    }
                    // Run the initialize task service so that some stocks appear upon an empty database
                    mServiceIntent.putExtra(getString(R.string.tag), getString(R.string.init));
                    startService(mServiceIntent);
                }
            } else {
                isConnected = false;
                networkToast();
            }
        }
    }

    public boolean isInternetOn(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNetworkReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(getString(R.string.invalid_stock_intent_filter)));
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), getString(R.string.stock_not_found),
                    Toast.LENGTH_SHORT).show();
        }
    };

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                mContext.getResources().getString(R.string.app_shared_preference), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(mContext.getResources().getString(R.string.key_is_percentage_widget),
                WidgetDataProvider.mIsPercentWidget);
        editor.putBoolean(mContext.getResources().getString(R.string.key_is_percentage_app),
                Utils.showPercent);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

}
