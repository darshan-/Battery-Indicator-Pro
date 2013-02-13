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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

public class BatteryInfoActivity extends FragmentActivity {
    private Resources res;
    private BatteryInfoPagerAdapter pagerAdapter;
    private ViewPager viewPager;
    private static CurrentInfoFragment currentInfoFragment;

    static {
        android.os.Debug.startMethodTracing();
        Logger.l("BIA static block");
        currentInfoFragment = new CurrentInfoFragment();
        Logger.l("instantiated CIF; static block finished");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.l("BIA.onCreate() start");
        super.onCreate(savedInstanceState);
        Logger.l("called super.onC()");
        currentInfoFragment.biServiceIntent = new Intent(this, BatteryInfoService.class);
        Logger.l("instantiated Intent");
        startService(currentInfoFragment.biServiceIntent);
        Logger.l("called startService()");
        //bindService(currentInfoFragment.biServiceIntent, currentInfoFragment.serviceConnection, 0);

        res = getResources();
        Logger.l("got resources");

        setContentView(R.layout.battery_info);
        Logger.l("set content view");

        pagerAdapter = new BatteryInfoPagerAdapter(getSupportFragmentManager());
        Logger.l("instantiated pager adapter");
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);
        Logger.l("set pager adapter");

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        tabStrip.setTabIndicatorColor(0x33b5e5);
        Logger.l("BIA.onCreate() finish");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && viewPager.getCurrentItem() > 0) {
            viewPager.setCurrentItem(0); // TODO: Or to current - 1?
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public static class BatteryInfoPagerAdapter extends FragmentPagerAdapter {
        public BatteryInfoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 1; // TODO
        }

        // TODO: Put Fragment types and page titles in Arrays or Map or something.
        @Override
        public Fragment getItem(int position) {
            return currentInfoFragment;
            /*if (position == 0) return currentInfoFragment;
              else               return new LogViewFragment();*/
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) return "Current Info".toUpperCase(); // TODO: Translatable
            else               return "History".toUpperCase();      // TODO: Translatable
        }
    }
}
