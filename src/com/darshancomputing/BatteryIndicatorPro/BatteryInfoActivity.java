/*
    Copyright (c) 2013-2016 Darshan-Josiah Barber

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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

import android.support.v13.app.FragmentPagerAdapter;

public class BatteryInfoActivity extends FragmentActivity {
    private BatteryInfoPagerAdapter pagerAdapter;
    private ViewPager viewPager;
    public static CurrentInfoFragment currentInfoFragment;
    public static LogViewFragment logViewFragment;
    private long startMillis;

    public Resources res;
    public Str str;
    public SharedPreferences settings;
    public SharedPreferences sp_store;

    private static final String LOG_TAG = "BatteryBot";

    public static final int PR_LVF_WRITE_STORAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        startMillis = System.currentTimeMillis();

        res = getResources();
        str = new Str(res);
        loadSettingsFiles();

        super.onCreate(savedInstanceState); // Recreates Fragments, so only call after doing necessary setup

        setContentView(R.layout.battery_info);

        pagerAdapter = new BatteryInfoPagerAdapter(getFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        tabStrip.setTabIndicatorColor(0x33b5e5);

        viewPager.setCurrentItem(1);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof CurrentInfoFragment)
            currentInfoFragment = (CurrentInfoFragment) fragment;
        if (fragment instanceof LogViewFragment)
            logViewFragment = (LogViewFragment) fragment;
    }

    public void loadSettingsFiles() {
        settings = getSharedPreferences(SettingsActivity.SETTINGS_FILE, Context.MODE_MULTI_PROCESS);
        sp_store = getSharedPreferences(SettingsActivity.SP_STORE_FILE, Context.MODE_MULTI_PROCESS);
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
                logViewFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    public class BatteryInfoPagerAdapter extends FragmentPagerAdapter {
        public BatteryInfoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 2; // TODO
        }

        // TODO: Put Fragment types and page titles in Arrays or Map or something.
        @Override
        public Fragment getItem(int position) {
            if (position == 1) {
                if (currentInfoFragment == null)
                    currentInfoFragment = new CurrentInfoFragment();
                return currentInfoFragment;
            } else {
                if (logViewFragment == null)
                    logViewFragment = new LogViewFragment();
                return logViewFragment;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 1) return res.getString(R.string.tab_current_info).toUpperCase();
            else               return res.getString(R.string.tab_history).toUpperCase();
        }
    }
}
