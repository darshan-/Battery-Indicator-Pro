/*
    Copyright (c) 2013-2017 Darshan-Josiah Barber

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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

public class BatteryInfoActivity extends FragmentActivity {
    private BatteryInfoPagerAdapter pagerAdapter;
    private ViewPager viewPager;

    private static final String LOG_TAG = "BatteryBot";

    public static final int PR_LVF_WRITE_STORAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PersistentFragment.getInstance(getSupportFragmentManager()); // Calling here ensures PF created before other Fragments?

        setContentView(R.layout.battery_info);

        pagerAdapter = new BatteryInfoPagerAdapter(getSupportFragmentManager());

        pagerAdapter.setContext(this);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        tabStrip.setTabIndicatorColor(0x33b5e5);

        viewPager.setCurrentItem(1);
        routeIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        //setIntent(intent); // Not done by system automatically
        routeIntent(intent);
    }

    private void routeIntent(Intent intent) {
        if (intent.hasExtra(BatteryInfoService.EXTRA_EDIT_ALARMS))
            viewPager.setCurrentItem(2);
        else if (intent.hasExtra(BatteryInfoService.EXTRA_CURRENT_INFO))
            viewPager.setCurrentItem(1);
    }

    @Override
    public void onStart() {
        super.onStart();

        pagerAdapter.setContext(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        pagerAdapter.setContext(null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && viewPager.getCurrentItem() != 1) {
            viewPager.setCurrentItem(1);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PR_LVF_WRITE_STORAGE: {
                LogViewFragment lvf = pagerAdapter.getLVF();

                if (lvf != null)
                    lvf.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    // Must be static in order to avoid leaking reference to outer class (Activity)
    public static class BatteryInfoPagerAdapter extends FragmentPagerAdapter {
        private Context context;
        private LogViewFragment logViewFragment;

        public BatteryInfoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setContext(Context c) {
            context = c;
        }

        public LogViewFragment getLVF() {
            return logViewFragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        // getItem() is apparently intended to always create new Fragments!
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new LogViewFragment();
                case 1:
                    return new CurrentInfoFragment();
                case 2:
                    return new AlarmsFragment();
                default:
                    return null;
            }
        }

        // instantiateItem(), on the other hand, either grabs a retained instance or creates a new one
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);

            if (position == 0)
                logViewFragment = (LogViewFragment) fragment;

            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (context == null)
                return null;

            Resources res = context.getResources();

            switch (position) {
                case 0:
                    return res.getString(R.string.tab_history).toUpperCase();
                case 1:
                    return res.getString(R.string.tab_current_info).toUpperCase();
                case 2:
                    return res.getString(R.string.alarm_settings).toUpperCase();
                default:
                    return null;
            }
        }
    }
}
