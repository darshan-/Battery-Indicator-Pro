/*
    Copyright (c) 2013-2026 Darshan Computing, LLC

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

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class BatteryInfoActivity extends AppCompatActivity {
//public class BatteryInfoActivity extends FragmentActivity {
    private BatteryInfoPagerAdapter pagerAdapter;
    private ViewPager viewPager;
    private NavigationMotionView navigationMotion;
    private final ViewGroup[] navigationItems = new ViewGroup[3];
    private final ColorDrawable actionBarBackground = new ColorDrawable();
    private View navigationBar, contentRoot;
    private VelocityTracker pagerVelocity;
    private boolean motionEnabled = true;
    private int scrollState = ViewPager.SCROLL_STATE_IDLE, lastBackground;
    private float pageProgress, releaseVelocity;

    //private static final String LOG_TAG = "BatteryBot";

    public static final int PR_POST_NOTIFICATIONS = 1;

    private void fixAPI35EdgeToEdgeLayout() {
        View root = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        // View root = findViewById(android.R.id.content);

        // TypedValue tv = new TypedValue();
        // int actionBarHeight = 0;
        // if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
        //     actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        // }

        // final int abHeight = actionBarHeight;
        // ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
        //         Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        //         v.setPadding(
        //                      bars.left,
        //                      bars.top + abHeight,
        //                      bars.right,
        //                      bars.bottom
        //                      );
        //         return insets;
        //     });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //setTheme(android.R.style.Theme_DeviceDefault);
        setTheme(R.style.bi_main_theme);
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PR_POST_NOTIFICATIONS);
        }

        getSupportActionBar().setElevation(0);
        PersistentFragment.getInstance(getSupportFragmentManager()); // Calling here ensures PF created before other Fragments?

        setContentView(R.layout.battery_info);

        fixAPI35EdgeToEdgeLayout();

        BatteryInfoService.setUpChannels(this);

        pagerAdapter = new BatteryInfoPagerAdapter(getSupportFragmentManager());

        pagerAdapter.setContext(this);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);
        navigationBar = findViewById(R.id.navigation_shell);
        contentRoot = findViewById(android.R.id.content);
        navigationMotion = (NavigationMotionView) findViewById(R.id.navigation_motion);
        getSupportActionBar().setBackgroundDrawable(actionBarBackground);

        setUpBottomNav();
        refreshMotionSettings();

        viewPager.setCurrentItem(0);
        routeIntent(getIntent());
        updateBottomNav(viewPager.getCurrentItem());
    }

    // Indexed by pager position
    private static final int[] NAV_ITEM_IDS = {R.id.nav_now, R.id.nav_history, R.id.nav_alarms};

    private void setUpBottomNav() {
        for (int i = 0; i < NAV_ITEM_IDS.length; i++) {
            final int page = i;
            ViewGroup item = (ViewGroup) findViewById(NAV_ITEM_IDS[i]);
            navigationItems[i] = item;
            item.setOnClickListener(v -> navigateTo(page));
            item.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) refreshMotionSettings();
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN ||
                    event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float slop = 10 * getResources().getDisplayMetrics().density;
                    boolean inside = event.getX() >= -slop && event.getX() <= v.getWidth() + slop &&
                                     event.getY() >= -slop && event.getY() <= v.getHeight() + slop;
                    navigationMotion.press(page, inside);
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                           event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    navigationMotion.press(page, false);
                }
                return false; // Native click, cancellation and accessibility handling remain intact.
            });
        }
        navigationMotion.bind(navigationItems);

        viewPager.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                refreshMotionSettings();
                releaseVelocity = 0;
                if (pagerVelocity != null) pagerVelocity.recycle();
                pagerVelocity = null;
            }
            if (pagerVelocity == null) pagerVelocity = VelocityTracker.obtain();
            pagerVelocity.addMovement(event);
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                pagerVelocity.computeCurrentVelocity(1000);
                releaseVelocity = event.getActionMasked() == MotionEvent.ACTION_CANCEL ? 0 :
                    -pagerVelocity.getXVelocity() / Math.max(1, viewPager.getWidth());
                pagerVelocity.recycle();
                pagerVelocity = null;
            }
            return false; // ViewPager continues to own finger tracking, fling and interruption.
        });

        viewPager.setPageTransformer(false, (page, position) -> {
            float distance = motionEnabled ? Math.min(1, Math.abs(position)) : 0;
            page.setAlpha(1 - 0.16f * distance);
            page.setScaleX(1 - 0.012f * distance);
            page.setScaleY(1 - 0.012f * distance);
        });

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float offset, int pixels) {
                pageProgress = position + offset;
                if (scrollState == ViewPager.SCROLL_STATE_DRAGGING)
                    navigationMotion.track(pageProgress);
                renderNavigation(pageProgress);
            }

            @Override
            public void onPageSelected(int position) {
                updateBottomNav(position);
                if (scrollState != ViewPager.SCROLL_STATE_DRAGGING)
                    navigationMotion.moveTo(position, motionEnabled);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    releaseVelocity = 0;
                    navigationMotion.beginTracking(pageProgress);
                } else if (scrollState == ViewPager.SCROLL_STATE_DRAGGING) {
                    navigationMotion.endTracking(viewPager.getCurrentItem(), releaseVelocity);
                }
                scrollState = state;
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    updateBottomNav(viewPager.getCurrentItem());
                    navigationMotion.moveTo(viewPager.getCurrentItem(), motionEnabled);
                }
            }
        });
    }

    private void refreshMotionSettings() {
        motionEnabled = ValueAnimator.areAnimatorsEnabled() &&
            Settings.Global.getFloat(getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) > 0;
        navigationMotion.setMotionEnabled(motionEnabled);
    }

    private void navigateTo(int page) {
        refreshMotionSettings();
        navigationMotion.moveTo(page, motionEnabled);
        viewPager.setCurrentItem(page, motionEnabled);
    }

    private void updateBottomNav(int selected) {
        SpannableString title = new SpannableString(getString(R.string.app_full_name));
        title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        getSupportActionBar().setTitle(title);

        for (int i = 0; i < NAV_ITEM_IDS.length; i++) {
            ViewGroup item = navigationItems[i];
            android.widget.TextView label = (android.widget.TextView) item.getChildAt(1);
            label.setTypeface(null, i == selected ? Typeface.BOLD : Typeface.NORMAL);
            item.setSelected(i == selected);
        }
        if (scrollState == ViewPager.SCROLL_STATE_IDLE) renderNavigation(selected);
    }

    private void renderNavigation(float position) {
        float home = 1 - Math.min(1, Math.max(0, position));
        Resources res = getResources();
        int on = ColorUtils.blendARGB(res.getColor(R.color.accent), res.getColor(R.color.overview_green), home);
        int off = ColorUtils.blendARGB(res.getColor(R.color.secondary_text), res.getColor(R.color.overview_muted), home);
        int background = ColorUtils.blendARGB(res.getColor(R.color.windowBackground), res.getColor(R.color.overview_background), home);
        if (background != lastBackground) {
            navigationBar.setBackgroundColor(background);
            contentRoot.setBackgroundColor(background);
            actionBarBackground.setColor(background);
            getWindow().setStatusBarColor(background);
            getWindow().setNavigationBarColor(background);
            lastBackground = background;
        }
        navigationMotion.setPillColor(ColorUtils.blendARGB(res.getColor(R.color.ring_track), res.getColor(R.color.overview_track), home));
        for (int i = 0; i < navigationItems.length; i++) {
            float weight = 1 - Math.min(1, Math.abs(i - position));
            int color = ColorUtils.blendARGB(off, on, weight);
            ((android.widget.ImageView) navigationItems[i].getChildAt(0)).setColorFilter(color);
            ((android.widget.TextView) navigationItems[i].getChildAt(1)).setTextColor(color);
        }
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
            navigateTo(2);
        else if (intent.hasExtra(BatteryInfoService.EXTRA_CURRENT_INFO))
            navigateTo(0);
    }

    @Override
    public void onStart() {
        super.onStart();

        pagerAdapter.setContext(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshMotionSettings();
        navigationMotion.moveTo(viewPager.getCurrentItem(), false);
        updateBottomNav(viewPager.getCurrentItem());
    }

    @Override
    public void onStop() {
        super.onStop();

        pagerAdapter.setContext(null);
        navigationMotion.finish();
        if (pagerVelocity != null) {
            pagerVelocity.recycle();
            pagerVelocity = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && viewPager.getCurrentItem() != 0) {
            navigateTo(0);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

        @Override
        public int getCount() {
            return 3;
        }

        // getItem() is apparently intended to always create new Fragments!
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new CurrentInfoFragment();
                case 1:
                    return new LogViewFragment();
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

            if (position == 1)
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
                    return res.getString(R.string.tab_current_info).toUpperCase();
                case 1:
                    return res.getString(R.string.tab_history).toUpperCase();
                case 2:
                    return res.getString(R.string.alarm_settings).toUpperCase();
                default:
                    return null;
            }
        }
    }
}
