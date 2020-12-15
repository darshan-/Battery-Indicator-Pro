/*
    Copyright (c) 2013-2020 Darshan Computing, LLC

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

public class BatteryInfoActivity extends AppCompatActivity {
//public class BatteryInfoActivity extends FragmentActivity {
    private BatteryInfoPagerAdapter pagerAdapter;
    private ViewPager viewPager;

    public BatteryLevel bl;

    //private static final String LOG_TAG = "BatteryBot";

    public static final int PR_LVF_WRITE_STORAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //setTheme(android.R.style.Theme_DeviceDefault);
        setTheme(R.style.bi_main_theme);
        super.onCreate(savedInstanceState);


        PersistentFragment.getInstance(getSupportFragmentManager()); // Calling here ensures PF created before other Fragments?

        setContentView(R.layout.battery_info);

        pagerAdapter = new BatteryInfoPagerAdapter(getSupportFragmentManager());

        pagerAdapter.setContext(this);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);

        //PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        //tabStrip.setTabIndicatorColor(0x33b5e5);
        //tabStrip.setTabIndicatorColor(0xbb3388);
        //tabStrip.setTabIndicatorColor(0x039be5);
        //tabStrip.setTabIndicatorColor(0x3c91c6);

        viewPager.setCurrentItem(1);
        routeIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        tabStrip.setTabIndicatorColor(Str.accent_color);
        System.out.println(".....................------------ Str.accent_color: " + Str.accent_color);

        int c = getSharedPreferences(SettingsActivity.SETTINGS_FILE, Context.MODE_MULTI_PROCESS)
            .getInt(SettingsActivity.KEY_UI_COLOR, Str.def_ui_color);
        //tabStrip.setTabIndicatorColor(c);

        bl = BatteryLevel.getInstance(this, PersistentFragment.getInstance(getSupportFragmentManager())
                                      .res.getInteger(R.integer.bl_inSampleSize));
        bl.setColor(c);
    }


    // Actually, Activity is re-created.  If I ever want to handle day-night configuration change myself,
    //  Then I'll want this.  But right now it's pointless; it's called before Activity is there yet, so
    //    I can't do anything with the views, and the views will pull in correct colors and everything anyway,
    //    since it's recreated.
    //
    // @Override
    // public void setTheme(int themeResId) {
    //     super.setTheme(themeResId);
    // }

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
    private static class BatteryInfoPagerAdapter extends FragmentPagerAdapter {
        private Context context;
        private LogViewFragment logViewFragment;

        BatteryInfoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setContext(Context c) {
            context = c;
        }

        LogViewFragment getLVF() {
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
