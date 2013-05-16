/*
    Copyright (c) 2013 Darshan-Josiah Barber

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

package com.darshancomputing.BatteryIndicatorPro;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class BatteryInfoAppWidgetProvider extends AppWidgetProvider {
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];

            Intent mainWindowIntent = new Intent(context, BatteryInfoActivity.class);
            PendingIntent mainWindowPendingIntent = PendingIntent.getActivity(context, 0, mainWindowIntent, 0);

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.battery_info_app_widget);
            rv.setOnClickPendingIntent(R.id.widget_layout, mainWindowPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
    }
}
