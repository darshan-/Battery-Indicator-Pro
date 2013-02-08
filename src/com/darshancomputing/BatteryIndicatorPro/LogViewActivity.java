/*
    Copyright (c) 2010-2013 Darshan-Josiah Barber

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

import android.content.res.Resources;
import android.os.Bundle;

import android.support.v4.app.FragmentActivity;

public class LogViewActivity extends FragmentActivity {
    private Resources res;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();

        setContentView(R.layout.log_view_fragment_activity);

        // Stranglely disabled by default for API level 14+
        if (res.getBoolean(R.bool.api_level_14_plus))
            getActionBar().setHomeButtonEnabled(true);

        setWindowSubtitle(res.getString(R.string.log_view_activity_subtitle));

    }

    private void setWindowSubtitle(String subtitle) {
        if (res.getBoolean(R.bool.long_activity_names))
            setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
        else
            setTitle(subtitle);
    }
}
