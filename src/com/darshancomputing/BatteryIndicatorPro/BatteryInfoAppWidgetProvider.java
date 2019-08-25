/*
    Copyright (c) 2013-2015 Darshan Computing, LLC

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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/*
 * Each widget type needs its own AppWidgetProvider class in order to show up in the widget list.
 *
 * This is the base class that other widget types can simply extend.
 *
 * For historical reasons, this base class is directly used as the AppWidgetProvider for the Circle widget,
 *  and this can't be changed without breaking existing installations.
 */

public class BatteryInfoAppWidgetProvider extends AppWidgetProvider {
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        BatteryInfoService.onWidgetUpdate(context, appWidgetManager, appWidgetIds);
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        BatteryInfoService.onWidgetDeleted(context, appWidgetIds);
    }

    //public void onEnabled(Context context) {
    //    BatteryInfoService.onWidgetEnabled(context);
    //}

    //public void onDisabled(Context context) {
    //    BatteryInfoService.onWidgetDisabled(context);
    //}

    //public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appwidgetManager,
    //                                      int appWidgetId, Bundle newOptions) {
    //}
}
