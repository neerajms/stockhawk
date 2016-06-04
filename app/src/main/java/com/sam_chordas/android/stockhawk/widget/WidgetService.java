package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by neeraj on 17/5/16.
 */
public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        WidgetDataProvider widgetDataProvider = new WidgetDataProvider(
                getApplicationContext(),intent);
        return widgetDataProvider;
    }
    public void changeCurrency(){

    }
}
