/*
    Copyright (c) 2009-2020 Darshan Computing, LLC

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

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {
    private static final int[] HAS_LINKS = {R.id.changelog, R.id.faq, R.id.website,
                                            R.id.other_apps,
                                            R.id.open_source, R.id.acknowledgments,
                                            R.id.translations, R.id.contact};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setElevation(0);
        }

        setContentView(R.layout.help);

        setTitle(getResources().getString(R.string.help_activity_subtitle));

        TextView tv;
        MovementMethod linkMovement = LinkMovementMethod.getInstance();

        for (int i=0; i < HAS_LINKS.length; i++) {
            tv = (TextView) findViewById(HAS_LINKS[i]);
            tv.setMovementMethod(linkMovement);
            tv.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        }

        tv = (TextView) findViewById(R.id.version);
        try {
            tv.setText(getResources().getString(R.string.app_full_name) + " " +
                       getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (Exception e) {
            tv.setText("...");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
