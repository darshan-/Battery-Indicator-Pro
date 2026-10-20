/*
    Copyright (c) 2009-2026 Darshan Computing, LLC

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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
//import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

public class CurrentInfoFragment extends Fragment {
    private static PersistentFragment pfrag;

    private static final Intent batteryUseIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    private static final IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

    // How often the current (mA) tile refreshes while the screen is visible
    private static final int CURRENT_REFRESH_MS = 5000;
    private static final int TILES_PER_ROW = 3;

    private View view, details_view;
    private AlertDialog details_dialog;
    private Button battery_use_b;
    private RingGaugeView ring_gauge;
    private TextView tv_level, tv_time_remaining, tv_until_what, tv_status_chip;
    private TextView tv_temperature, tv_health, tv_voltage, tv_current, tv_current_label, tv_details_summary;
    private View usage_card, usage_bar_on, usage_bar_off;
    private TextView tv_usage_title, tv_usage_total, tv_usage_on, tv_usage_off;
    private LinearLayout tiles_container;
    private LevelChartView level_chart;
    private TextView tv_chart_empty;
    private LogDatabase logDatabase;

    // Tile keys, in display order
    private static final int TILE_CAPACITY = 0, TILE_CYCLES = 1, TILE_CURRENT = 2,
        TILE_TEMPERATURE = 3, TILE_VOLTAGE = 4, TILE_CONDITION = 5, TILE_CHARGING_MODE = 6, TILE_COUNT = 7;
    private final String[] tileValues = new String[TILE_COUNT];
    private final TextView[] tileValueViews = new TextView[TILE_COUNT];
    private String shownTileKeys = ""; // Which tiles are currently laid out, to rebuild only when that changes

    private BatteryInfo info = new BatteryInfo();
    //private CurrentHack currentHack;

    public static boolean awaitingNotificationUnblock;
    public static boolean showingNotificationBlockDialog;

    public static boolean awaitingUnoptimization;
    public static boolean showingBatteryOptimizedDialog;

    //private static final String LOG_TAG = "BatteryBot";

    private final Handler mHandler = new Handler();
    private final Runnable mARefresher = new Runnable() {
        public void run() {
            refreshCurrent();
            mHandler.postDelayed(mARefresher, CURRENT_REFRESH_MS);
        }
    };

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.current_info, container, false);
        details_view = inflater.inflate(R.layout.overview_details, container, false);

        ring_gauge = (RingGaugeView) view.findViewById(R.id.ring_gauge);
        tv_level = (TextView) view.findViewById(R.id.level);
        tv_time_remaining = (TextView) view.findViewById(R.id.time_remaining);
        tv_until_what = (TextView) view.findViewById(R.id.until_what);
        tv_status_chip = (TextView) view.findViewById(R.id.status_chip);

        tv_temperature = (TextView) view.findViewById(R.id.overview_temperature);
        tv_health = (TextView) view.findViewById(R.id.overview_health_value);
        tv_voltage = (TextView) view.findViewById(R.id.overview_voltage);
        tv_current = (TextView) view.findViewById(R.id.overview_current);
        tv_current_label = (TextView) view.findViewById(R.id.overview_current_label);
        tv_details_summary = (TextView) details_view.findViewById(R.id.details_summary);

        usage_card = details_view.findViewById(R.id.usage_card);
        usage_bar_on = details_view.findViewById(R.id.usage_bar_on);
        usage_bar_off = details_view.findViewById(R.id.usage_bar_off);
        tv_usage_title = (TextView) details_view.findViewById(R.id.usage_title);
        tv_usage_total = (TextView) details_view.findViewById(R.id.usage_total);
        tv_usage_on = (TextView) details_view.findViewById(R.id.usage_on);
        tv_usage_off = (TextView) details_view.findViewById(R.id.usage_off);
        View usage_bar = details_view.findViewById(R.id.usage_bar);
        usage_bar.setClipToOutline(true); // Keeps the bar segments inside the rounded track

        tiles_container = (LinearLayout) details_view.findViewById(R.id.tiles);
        shownTileKeys = "";

        level_chart = (LevelChartView) details_view.findViewById(R.id.level_chart);
        tv_chart_empty = (TextView) details_view.findViewById(R.id.chart_empty);

        battery_use_b = (Button) view.findViewById(R.id.battery_use_b);

        view.findViewById(R.id.battery_details).setOnClickListener(detailsListener);

        bindButtons();
        view.getViewTreeObserver().addOnGlobalLayoutListener(this::fitOverviewGauge);

        if (! androidx.core.app.NotificationManagerCompat.from(getActivity()).areNotificationsEnabled() &&
            ! showingNotificationBlockDialog)
        {
            showingNotificationBlockDialog = true;
            DialogFragment df = new NotificationsDisabledDialogFragment();
            df.setTargetFragment(this, 0);
            df.show(getFragmentManager(), "TODO: What is this string for?3");
        }

        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations("com.darshancomputing.BatteryIndicatorPro") &&
            !showingBatteryOptimizedDialog)
        {
            showingBatteryOptimizedDialog = true;
            DialogFragment df = new BatteryOptimizedDialogFragment();
            df.setTargetFragment(this, 0);
            df.show(getFragmentManager(), "TODO: What is this string for?4");
        }

        return view;
    }

    // Keep the action visible on compact phones; large font sizes can still scroll.
    private void fitOverviewGauge() {
        if (view == null || view.getHeight() == 0) return;
        android.widget.ScrollView scroll = (android.widget.ScrollView) view;
        View content = scroll.getChildAt(0);
        View gauge = view.findViewById(R.id.gauge_group);
        float levelSize = Math.min(64 * pfrag.res.getDisplayMetrics().scaledDensity, gauge.getWidth() * 0.30f);
        if (levelSize > 0 && Math.abs(tv_level.getTextSize() - levelSize) > 0.5f)
            tv_level.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, levelSize);
        int overflow = battery_use_b.getBottom() + content.getPaddingBottom() - scroll.getHeight();
        int minimum = (int) (168 * pfrag.res.getDisplayMetrics().density);
        if (overflow > 0 && gauge.getHeight() > minimum) {
            ViewGroup.LayoutParams params = gauge.getLayoutParams();
            params.width = params.height = Math.max(minimum, gauge.getHeight() - overflow);
            gauge.setLayoutParams(params);
        }
    }

    @Override
    public void onDestroyView() {
        if (details_dialog != null) {
            details_dialog.dismiss();
            details_dialog = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logDatabase.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pfrag = PersistentFragment.getInstance(getFragmentManager());
        logDatabase = new LogDatabase(getActivity());

        CurrentHack.setContext(getActivity());

        getChildFragmentManager().setFragmentResultListener(CalibrationDialogFragment.RESULT_KEY, this,
                                                            (requestKey, result) -> onCalibrationSaved());

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Before the battery intent is loaded below, so voltage and current use current corrections
        CurrentHack.setPreferFS(pfrag.settings.getBoolean(SettingsFragment.KEY_CURRENT_HACK_PREFER_FS,
                                                          pfrag.res.getBoolean(R.bool.default_prefer_fs_current_hack)));
        Calibration.apply(pfrag.settings);

        pfrag.setCIF(this);
        pfrag.sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_REGISTER_CLIENT);

        if (awaitingNotificationUnblock) {
            awaitingNotificationUnblock = false;
            pfrag.sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_CANCEL_NOTIFICATION_AND_RELOAD_SETTINGS);
        }

        Intent bc_intent = getActivity().registerReceiver(null, batteryChangedFilter, Context.RECEIVER_NOT_EXPORTED);
        info.load(bc_intent);
        info.load(pfrag.sp_service);
        handleUpdatedBatteryInfo();

        // Reading current through BatteryManager is cheap, so the tile always refreshes while visible
        mHandler.postDelayed(mARefresher, CURRENT_REFRESH_MS);

        maybePromptCalibration();
    }

    // Offers calibration once, after the notification and battery-optimization prompts are out of the way
    private void maybePromptCalibration() {
        if (pfrag.settings.getBoolean(Calibration.KEY_PROMPTED, false) ||
            pfrag.settings.getBoolean(Calibration.KEY_CALIBRATED, false) ||
            CurrentHack.getRawCurrent() == null ||
            getChildFragmentManager().findFragmentByTag(CalibrationDialogFragment.TAG) != null)
            return;

        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        if (!androidx.core.app.NotificationManagerCompat.from(getActivity()).areNotificationsEnabled() ||
            !pm.isIgnoringBatteryOptimizations(getActivity().getPackageName()))
            return;

        pfrag.settings.edit().putBoolean(Calibration.KEY_PROMPTED, true).apply();
        new CalibrationDialogFragment().show(getChildFragmentManager(), CalibrationDialogFragment.TAG);
    }

    private void onCalibrationSaved() {
        pfrag.sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_RELOAD_SETTINGS);

        // Reload with the new voltage correction; current is read fresh on each refresh
        Intent bc_intent = getActivity().registerReceiver(null, batteryChangedFilter, Context.RECEIVER_NOT_EXPORTED);
        info.load(bc_intent);
        handleUpdatedBatteryInfo();
    }

    @Override
    public void onStop() {
        super.onStop();

        mHandler.removeCallbacks(mARefresher);
        pfrag.setCIF(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.menu_help).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            mStartActivity(SettingsActivity.class);

            return true;
        }

        if (item.getItemId() == R.id.menu_close) {
            DialogFragment df = new ConfirmCloseDialogFragment();
            // Setting target to this leaks the Fragment, but that's sort of good, as it allows pressing Okay
            //  to work even if the screen rotates.  Even if it rotates many times back and forth, only the
            //  first Fragment is leaked, which will do the closing if Okay is pressed.  Once the dialog is
            //  gone (even if canceled), then the it and the leaked Fragment will be garbage collected.
            df.setTargetFragment(this, 0);
            df.show(getFragmentManager(), "TODO: What is this string for?2");

            return true;
        }

        if (item.getItemId() == R.id.menu_help) {
            mStartActivity(HelpActivity.class);

            return true;
        }

        if (item.getItemId() == R.id.menu_rate_and_review) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                                         Uri.parse("market://details?id=com.darshancomputing.BatteryIndicatorPro")));
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Sorry, can't launch Market!", Toast.LENGTH_SHORT).show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class NotificationsDisabledDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                .setTitle(pfrag.res.getString(R.string.notifications_disabled))
                .setMessage(pfrag.res.getString(R.string.notifications_disabled_message))
                .setPositiveButton(pfrag.res.getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            final Intent i = new Intent();
                            i.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(i);

                            CurrentInfoFragment.awaitingNotificationUnblock = true;
                            CurrentInfoFragment.showingNotificationBlockDialog = false;

                            di.cancel();
                        }
                    })
                .setNegativeButton(pfrag.res.getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            CurrentInfoFragment.awaitingNotificationUnblock = false;
                            CurrentInfoFragment.showingNotificationBlockDialog = false;

                            di.cancel();
                        }
                    })
                .create();
        }
    }

    public static class BatteryOptimizedDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                .setTitle(pfrag.res.getString(R.string.battery_optimized))
                .setMessage(pfrag.res.getString(R.string.battery_optimized_message))
                .setPositiveButton(pfrag.res.getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            final Intent i = new Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:" + getActivity().getPackageName()));
                            try {
                                startActivity(i);
                            } catch (android.content.ActivityNotFoundException e) {
                                // Some devices don't support that request, and this is better than nothing
                                final Intent i2 = new Intent();
                                i2.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                i2.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                try {
                                    startActivity(i2);
                                } catch (android.content.ActivityNotFoundException e2) {
                                    // And on truly broken devices, I guess just opening root of system settings is worth doing?
                                    // Inspired by https://github.com/kontalk/androidclient/commit/be78119687940545d3613ae0d4280f4068125f6a
                                    final Intent i3 = new Intent();
                                    i3.setAction(android.provider.Settings.ACTION_SETTINGS);
                                    i3.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i3.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(i3);
                                }
                            }

                            CurrentInfoFragment.awaitingUnoptimization = true;
                            CurrentInfoFragment.showingBatteryOptimizedDialog = false;

                            di.cancel();
                        }
                    })
                .setNegativeButton(pfrag.res.getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            CurrentInfoFragment.awaitingUnoptimization = false;
                            CurrentInfoFragment.showingBatteryOptimizedDialog = false;

                            di.cancel();
                        }
                    })
                .create();
        }
    }

    public static class ConfirmCloseDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                .setTitle(pfrag.res.getString(R.string.confirm_close))
                .setMessage(pfrag.res.getString(R.string.confirm_close_hint))
                .setPositiveButton(pfrag.res.getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            pfrag.closeApp();
                            di.cancel();
                        }
                    })
                .setNegativeButton(pfrag.res.getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            di.cancel();
                        }
                    })
                .create();
        }
    }

    public void batteryInfoUpdated(Bundle bundle) {
        info.loadBundle(bundle);
        handleUpdatedBatteryInfo();
    }

    private void handleUpdatedBatteryInfo() {
        android.content.res.Resources res = pfrag.res;

        ring_gauge.setLevel(info.percent);
        ring_gauge.setLevelColor(res.getColor(info.percent <= 15 ? R.color.overview_low : R.color.overview_green));
        tv_level.setText(res.getString(R.string.percent_value, info.percent));
        refreshPrediction();

        String status = Str.statuses[info.last_status];
        if (info.last_status == BatteryInfo.STATUS_CHARGING)
            status += " " + Str.pluggeds[info.last_plugged];

        tv_status_chip.setText(status);
        view.findViewById(R.id.gauge_group).setContentDescription(
            res.getString(R.string.overview_gauge_description, info.percent) + ", " + status);

        refreshUsageCard();
        refreshTiles();
        refreshOverview();
        refreshChart();
        if (details_dialog != null && details_dialog.isShowing())
            tv_details_summary.setText(buildDetailsText());
    }

    private void refreshPrediction() {
        android.content.res.Resources res = pfrag.res;
        if (info.status == BatteryInfo.STATUS_FULLY_CHARGED) {
            tv_time_remaining.setText(R.string.overview_fully_charged);
            tv_until_what.setText(R.string.overview_ready);
        } else if (info.prediction.what == BatteryInfo.Prediction.NONE) {
            tv_time_remaining.setText(R.string.overview_estimating);
            tv_until_what.setText(R.string.overview_estimate_pending);
        } else {
            BatteryInfo.RelativeTime time = info.prediction.last_rtime;
            String duration;
            if (time.days > 0)
                duration = res.getString(R.string.overview_time_days, time.days, time.hours);
            else if (time.hours > 0)
                duration = res.getString(R.string.overview_time_hours, time.hours, time.minutes);
            else
                duration = res.getString(R.string.overview_time_minutes, time.minutes);
            tv_time_remaining.setText(res.getString(
                info.prediction.what == BatteryInfo.Prediction.UNTIL_CHARGED ?
                    R.string.overview_time_to_full : R.string.overview_time_left, duration));
            tv_until_what.setText(R.string.overview_estimate_caption);
        }
    }

    private void refreshOverview() {
        android.content.res.Resources res = pfrag.res;
        tv_temperature.setText(tileValues[TILE_TEMPERATURE]);
        tv_health.setText(info.health == BatteryManager.BATTERY_HEALTH_GOOD ?
            res.getString(R.string.overview_health_good) : Str.healths[info.health]);
        tv_voltage.setText(info.voltage > 500 ?
            res.getString(R.string.overview_voltage_value, info.voltage / 1000.0) :
            res.getString(R.string.overview_not_reported));
        updateOverviewCurrent(currentMa());
    }

    private void updateOverviewCurrent(Long current) {
        tv_current_label.setText(info.status == BatteryInfo.STATUS_CHARGING ? R.string.tile_charge_rate :
            isUnplugged() || info.status == BatteryInfo.STATUS_DISCHARGING ? R.string.tile_drain_rate : R.string.tile_current);
        tv_current.setText(current == null ? pfrag.res.getString(R.string.overview_not_reported) :
            pfrag.res.getString(R.string.ma_value, Math.abs(current)));
    }

    private void refreshChart() {
        long now = System.currentTimeMillis();
        level_chart.setData(logDatabase.getLevelHistory(now - LevelChartView.WINDOW_MS), now, info.percent, info.status);
        tv_chart_empty.setVisibility(level_chart.hasEnoughData() ? View.GONE : View.VISIBLE);
    }

    private void refreshUsageCard() {
        if (info.charge_counter_uah < 0) { // Device doesn't report charge, so there's nothing to show
            usage_card.setVisibility(View.GONE);
            return;
        }

        android.content.res.Resources res = pfrag.res;
        usage_card.setVisibility(View.VISIBLE);

        int title;
        if (info.last_status == BatteryInfo.STATUS_UNPLUGGED) title = R.string.usage_title_used;
        else if (info.last_status == BatteryInfo.STATUS_CHARGING) title = R.string.usage_title_added;
        else title = R.string.usage_title_other;
        tv_usage_title.setText(title);

        long total = info.screen_on_drain_uah + info.screen_off_drain_uah;
        tv_usage_total.setText(res.getString(R.string.mah_value, Math.abs(total) / 1000));

        // Bar segments are proportional to how much charge moved in each screen state
        long on = Math.abs(info.screen_on_drain_uah), off = Math.abs(info.screen_off_drain_uah);
        setWeight(usage_bar_on, on);
        setWeight(usage_bar_off, off);

        tv_usage_on.setText(legendLine(res, R.string.usage_legend_on, info.screen_on_drain_uah, info.screen_on_ms));
        tv_usage_off.setText(legendLine(res, R.string.usage_legend_off, info.screen_off_drain_uah, info.screen_off_ms));
    }

    private static void setWeight(View v, long weight) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        lp.weight = weight;
        v.setLayoutParams(lp);
    }

    private static String legendLine(android.content.res.Resources res, int resId, long drain_uah, long ms) {
        long mins = ms / 60000;
        String s = res.getString(resId, mins / 60, mins % 60);

        if (ms >= MIN_MS_FOR_AVERAGE)
            s = res.getString(R.string.usage_legend_avg, s, Math.abs(drain_uah) * 3600 / ms);

        return s;
    }

    private void refreshTiles() {
        android.content.res.Resources res = pfrag.res;
        java.util.Arrays.fill(tileValues, null);

        int design = designCapacityMah();
        if (info.est_capacity_uah > 0 && design > 0)
            tileValues[TILE_CAPACITY] = res.getString(R.string.percent_value, (int) (info.est_capacity_uah / 10 / design));
        else if (info.est_capacity_uah > 0)
            tileValues[TILE_CAPACITY] = res.getString(R.string.mah_value, info.est_capacity_uah / 1000);

        if (info.cycle_count >= 0)
            tileValues[TILE_CYCLES] = String.valueOf(info.cycle_count);

        Long current = currentMa();
        if (current != null)
            // Vendors disagree on the sign, so show the magnitude and let the label say which way it flows
            tileValues[TILE_CURRENT] = res.getString(R.string.ma_value, Math.abs(current));

        boolean convertF = pfrag.settings.getBoolean(SettingsFragment.KEY_CONVERT_F,
                                                     res.getBoolean(R.bool.default_convert_to_fahrenheit));
        tileValues[TILE_TEMPERATURE] = Str.formatTemp(info.temperature, convertF);

        if (info.voltage > 500)
            tileValues[TILE_VOLTAGE] = Str.formatVoltage(info.voltage);

        tileValues[TILE_CONDITION] = Str.healths[info.health];

        String[] states = res.getStringArray(R.array.charging_states);
        if (info.charging_state > 0 && info.charging_state < states.length)
            tileValues[TILE_CHARGING_MODE] = states[info.charging_state];

        StringBuilder keys = new StringBuilder(isUnplugged() ? "u:" : "p:"); // Current tile's label depends on this
        for (int i = 0; i < TILE_COUNT; i++)
            if (tileValues[i] != null) keys.append(i).append(',');

        if (!keys.toString().equals(shownTileKeys)) {
            shownTileKeys = keys.toString();
            buildTiles();
        }

        for (int i = 0; i < TILE_COUNT; i++)
            if (tileValueViews[i] != null && tileValues[i] != null)
                tileValueViews[i].setText(tileValues[i]);
    }

    // Lays out the available tiles in rows, so missing data leaves no empty gaps
    private void buildTiles() {
        int[] labels = {(designCapacityMah() > 0 ? R.string.tile_health : R.string.tile_capacity), R.string.tile_cycles,
                        (isUnplugged() ? R.string.tile_drain_rate : R.string.tile_charge_rate),
                        R.string.tile_temperature, R.string.tile_voltage,
                        R.string.tile_condition, R.string.tile_charging_mode};
        int gap = (int) (8 * pfrag.res.getDisplayMetrics().density);
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        tiles_container.removeAllViews();
        java.util.Arrays.fill(tileValueViews, null);

        LinearLayout row = null;
        int inRow = 0;

        for (int i = 0; i < TILE_COUNT; i++) {
            if (tileValues[i] == null) continue;

            if (row == null || inRow == TILES_PER_ROW) {
                row = new LinearLayout(getActivity());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                              ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.topMargin = gap;
                tiles_container.addView(row, rlp);
                equalizeTileHeights(row);
                inRow = 0;
            }

            View tile = inflater.inflate(R.layout.stat_tile, row, false);
            ((TextView) tile.findViewById(R.id.tile_label)).setText(labels[i]);
            tileValueViews[i] = (TextView) tile.findViewById(R.id.tile_value);

            LinearLayout.LayoutParams tlp = (LinearLayout.LayoutParams) tile.getLayoutParams();
            if (inRow > 0) tlp.leftMargin = gap;
            row.addView(tile, tlp);
            inRow++;
        }

        // Pad the last row with invisible spacers so its tiles keep the same width as the others
        while (row != null && inRow < TILES_PER_ROW) {
            View spacer = new View(getActivity());
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, 1, 1);
            slp.leftMargin = gap;
            row.addView(spacer, slp);
            inRow++;
        }
    }

    // After layout, stretches every tile in the row to the tallest one (a value can wrap to two lines)
    private static void equalizeTileHeights(final LinearLayout row) {
        row.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                int tallest = 0;
                for (int i = 0; i < row.getChildCount(); i++)
                    tallest = Math.max(tallest, row.getChildAt(i).getMeasuredHeight());

                for (int i = 0; i < row.getChildCount(); i++) {
                    View child = row.getChildAt(i);
                    if (child.getMinimumHeight() != tallest && child.getMeasuredHeight() < tallest)
                        child.setMinimumHeight(tallest);
                }
            }
        });
    }

    // Current in mA, honoring the "prefer average" setting; null if the device doesn't report it
    private Long currentMa() {
        Long current = null;
        if (pfrag.settings.getBoolean(SettingsFragment.KEY_PREFER_CURRENT_AVG_IN_MAIN_WINDOW, false))
            current = CurrentHack.getAvgCurrent();
        if (current == null)
            current = CurrentHack.getCurrent();
        return current;
    }

    // Design capacity from settings, else from the system's power profile; -1 if neither
    private int designCapacityMah() {
        return DesignCapacity.get(getActivity(), pfrag.settings);
    }

    // Averages over a few seconds are wildly off, so only show one after this long
    private static final long MIN_MS_FOR_AVERAGE = 5 * 60 * 1000;

    private static String usageLine(android.content.res.Resources res, int resId, long drain_uah, long ms) {
        long mins = ms / 60000;
        // Positive drain means charge was used; negative means it was added (while charging)
        String amount = res.getString(drain_uah >= 0 ? R.string.usage_used : R.string.usage_added,
                                      Math.abs(drain_uah) / 1000);

        if (ms >= MIN_MS_FOR_AVERAGE) {
            // mAh per hour is average mA
            long ma = Math.abs(drain_uah) * 3600 / ms;
            amount = res.getString(R.string.usage_avg, amount, ma);
        }

        return res.getString(resId, mins / 60, mins % 60, amount);
    }

    private String buildDetailsText() {
        android.content.res.Resources res = pfrag.res;
        StringBuilder sb = new StringBuilder();

        if (info.charge_counter_uah < 0) {
            sb.append(res.getString(R.string.details_no_counter));
        } else {
            long totalMs = info.screen_on_ms + info.screen_off_ms;
            long totalDrain = info.screen_on_drain_uah + info.screen_off_drain_uah;
            long totalMins = totalMs / 60000;

            sb.append(res.getString(R.string.details_since, totalMins / 60, totalMins % 60)).append('\n');
            sb.append(res.getString(totalDrain >= 0 ? R.string.details_used : R.string.details_added,
                                    Math.abs(totalDrain) / 1000)).append('\n');
            sb.append(usageLine(res, R.string.details_screen_on, info.screen_on_drain_uah, info.screen_on_ms)).append('\n');
            sb.append(usageLine(res, R.string.details_screen_off, info.screen_off_drain_uah, info.screen_off_ms)).append("\n\n");

            sb.append(res.getString(R.string.details_charge_now, info.charge_counter_uah / 1000)).append('\n');

            if (info.est_capacity_uah > 0) {
                sb.append(res.getQuantityString(R.plurals.details_est_capacity, info.est_capacity_samples,
                                                info.est_capacity_uah / 1000, info.est_capacity_samples)).append('\n');

                int design = designCapacityMah();
                if (design > 0)
                    sb.append(res.getString(R.string.details_health, design, info.est_capacity_uah / 10 / design)).append('\n');
                else
                    sb.append(res.getString(R.string.details_no_design)).append('\n');
            } else {
                sb.append(res.getString(R.string.details_no_estimate)).append('\n');
            }
        }

        if (info.cycle_count >= 0)
            sb.append('\n').append(res.getString(R.string.details_cycles, info.cycle_count));

        String[] states = res.getStringArray(R.array.charging_states);
        if (info.charging_state > 0 && info.charging_state < states.length)
            sb.append('\n').append(res.getString(R.string.details_charging_mode, states[info.charging_state]));

        if (info.charge_counter_uah >= 0)
            sb.append("\n\n").append(res.getString(R.string.details_estimate_note));

        return sb.toString();
    }

    private final OnClickListener detailsListener = new OnClickListener() {
        public void onClick(View v) {
            if (details_dialog != null && details_dialog.isShowing()) return;
            if (details_view.getParent() instanceof ViewGroup)
                ((ViewGroup) details_view.getParent()).removeView(details_view);
            tv_details_summary.setText(buildDetailsText());
            refreshChart();
            details_dialog = new AlertDialog.Builder(getActivity())
                .setTitle(pfrag.res.getString(R.string.battery_details))
                .setView(details_view)
                .setPositiveButton(pfrag.res.getString(android.R.string.ok), null)
                .show();
        }
    };

    private void refreshCurrent() {
        Long current = currentMa();
        updateOverviewCurrent(current);

        // Availability changed (rare), so the tile grid needs rebuilding
        if ((current == null) != (tileValues[TILE_CURRENT] == null)) {
            refreshTiles();
            return;
        }

        if (current != null && tileValueViews[TILE_CURRENT] != null)
            tileValueViews[TILE_CURRENT].setText(pfrag.res.getString(R.string.ma_value, Math.abs(current)));
    }

    private boolean isUnplugged() {
        return info.status == BatteryInfo.STATUS_UNPLUGGED;
    }

    /* Battery Use */
    private final OnClickListener buButtonListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                startActivity(batteryUseIntent);
            } catch (Exception e) {
                battery_use_b.setEnabled(false);
            }
        }
    };

    private void mStartActivity(Class<?> c) {
        ComponentName comp = new ComponentName(getActivity().getPackageName(), c.getName());
        //startActivity(new Intent().setComponent(comp));
        startActivityForResult(new Intent().setComponent(comp), 1);
        //getActivity().finish();
    }

    private void bindButtons() {
        if (getActivity().getPackageManager().resolveActivity(batteryUseIntent, 0) == null) {
            battery_use_b.setEnabled(false); /* TODO: change how the disabled button looks */
        } else {
            battery_use_b.setOnClickListener(buButtonListener);
        }
    }
}
