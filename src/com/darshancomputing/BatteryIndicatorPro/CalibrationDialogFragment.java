/*
    Copyright (c) 2026 Darshan Computing, LLC

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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Collections;

// Measures how the device reports current (unit and sign) while discharging with the screen on,
//  and offers the two-cell battery corrections. The host listens for RESULT_KEY on the
//  FragmentManager it shows this with, to tell the service to reload settings.
public class CalibrationDialogFragment extends DialogFragment {
    static final String TAG = "calibration";
    static final String RESULT_KEY = "calibration_saved";

    private static final long TICK_MS = 250;
    private static final int SKIP_TICKS = 4;   // Readings settle for about a second after unplugging
    private static final int SAMPLES = 32;     // About 8 seconds of readings

    private static final int STATE_INTRO       = 0;
    private static final int STATE_WAIT_UNPLUG = 1;
    private static final int STATE_MEASURING   = 2;
    private static final int STATE_RESULT      = 3;
    private static final int STATE_NO_CURRENT  = 4; // Nothing to calibrate; only the voltage fix applies

    private int state = STATE_INTRO;
    private boolean replugged;
    private boolean readsZero;
    private int ticks;
    private final ArrayList<Long> samples = new ArrayList<>();
    private int sign = 1;
    private int rawPerMa = Calibration.RAW_PER_MA_UA;
    private long drainMa;
    private boolean offerDualCurrent, offerDualVoltage; // Only when suggested or already on

    private SharedPreferences settings;
    private Resources res;
    private AlertDialog dialog;
    private TextView message, unitView, signView, drainView, dualCurrentHint, dualVoltageHint;
    private ProgressBar progress;
    private View resultCard;
    private CheckBox dualCurrent, dualVoltage;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tick;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        res = getResources();
        settings = context.getSharedPreferences(SettingsFragment.SETTINGS_FILE, Context.MODE_MULTI_PROCESS);

        CurrentHack.setContext(context);
        CurrentHack.setPreferFS(settings.getBoolean(SettingsFragment.KEY_CURRENT_HACK_PREFER_FS,
                                                    res.getBoolean(R.bool.default_prefer_fs_current_hack)));

        View view = getActivity().getLayoutInflater().inflate(R.layout.calibration_dialog, null);
        message = view.findViewById(R.id.calibration_message);
        progress = view.findViewById(R.id.calibration_progress);
        resultCard = view.findViewById(R.id.calibration_result);
        unitView = view.findViewById(R.id.calibration_unit);
        signView = view.findViewById(R.id.calibration_sign);
        drainView = view.findViewById(R.id.calibration_drain);
        dualCurrent = view.findViewById(R.id.calibration_dual_current);
        dualCurrentHint = view.findViewById(R.id.calibration_dual_current_hint);
        dualVoltage = view.findViewById(R.id.calibration_dual_voltage);
        dualVoltageHint = view.findViewById(R.id.calibration_dual_voltage_hint);
        progress.setMax(SAMPLES);

        offerDualCurrent = settings.getBoolean(Calibration.KEY_DUAL_CELL_CURRENT, false);
        dualCurrent.setChecked(offerDualCurrent);
        dualCurrentHint.setText(R.string.calibration_dual_current_hint_plain);
        setUpVoltageOption();

        if (CurrentHack.getRawCurrent() == null)
            state = STATE_NO_CURRENT;

        dialog = new AlertDialog.Builder(context)
            .setTitle(R.string.calibration_title)
            .setView(view)
            .setPositiveButton(R.string.calibration_start, null)
            .setNegativeButton(R.string.calibration_not_now, null)
            .create();
        // Taps outside would end a measurement the user is waiting on
        dialog.setCanceledOnTouchOutside(false);

        // Set here rather than on the builder so the positive button doesn't always dismiss
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> onPositive());
            render();
        });

        return dialog;
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(tick);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (state == STATE_WAIT_UNPLUG || state == STATE_MEASURING)
            handler.post(tick);
    }

    private void onPositive() {
        if (state == STATE_INTRO) {
            begin();
        } else if (state == STATE_RESULT || state == STATE_NO_CURRENT) {
            save();
            dismiss();
        }
    }

    private void begin() {
        samples.clear();
        ticks = 0;
        replugged = false;
        state = isPlugged() ? STATE_WAIT_UNPLUG : STATE_MEASURING;
        render();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void tick() {
        boolean plugged = isPlugged();

        if (state == STATE_WAIT_UNPLUG) {
            if (!plugged) {
                state = STATE_MEASURING;
                samples.clear();
                ticks = 0;
            }
        } else if (state == STATE_MEASURING) {
            if (plugged) {
                state = STATE_WAIT_UNPLUG;
                replugged = true;
            } else if (++ticks > SKIP_TICKS) {
                Long raw = CurrentHack.getRawCurrent();
                if (raw != null) samples.add(raw);

                if (ticks >= SKIP_TICKS + SAMPLES) {
                    finishMeasuring();
                    render();
                    return;
                }
            }
        } else {
            return;
        }

        render();
        handler.postDelayed(tick, TICK_MS);
    }

    private void finishMeasuring() {
        if (samples.size() < SAMPLES / 2) {
            state = STATE_NO_CURRENT;
            return;
        }

        ArrayList<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        long median = sorted.get(sorted.size() / 2);

        if (median == 0) {
            readsZero = true;
            state = STATE_NO_CURRENT;
            return;
        }

        sign = Calibration.detectSign(median);
        rawPerMa = Calibration.detectRawPerMa(Math.abs(median));
        drainMa = Math.abs(median) / rawPerMa;
        suggestDualCurrent();
        state = STATE_RESULT;
    }

    // Suggests the current fix when the charge counter implies about half the design capacity
    private void suggestDualCurrent() {
        Intent bi = batteryIntent();
        int percent = bi == null ? 0 : bi.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 /
            Math.max(1, bi.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        BatteryManager bm = (BatteryManager) getActivity().getSystemService(Context.BATTERY_SERVICE);
        long uah = UsageTracker.rawCounterToUah(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER), percent);
        long impliedMah = percent > 0 && uah > 0 ? uah * 100 / percent / 1000 : -1;

        int design = DesignCapacity.get(getActivity(), settings);

        if (impliedMah > 0 && design > 0 && impliedMah < design * 0.6) {
            dualCurrentHint.setText(res.getString(R.string.calibration_dual_current_hint, impliedMah, design));
            dualCurrent.setChecked(true);
            offerDualCurrent = true;
        }
    }

    private void setUpVoltageOption() {
        Intent bi = batteryIntent();
        int rawMv = bi == null ? 0 : bi.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        boolean enabled = settings.getBoolean(Calibration.KEY_DUAL_CELL_VOLTAGE, false);

        offerDualVoltage = enabled || rawMv > Calibration.DUAL_CELL_VOLTAGE_MV;
        dualVoltage.setChecked(offerDualVoltage);
        if (rawMv > 500)
            dualVoltageHint.setText(res.getString(R.string.calibration_dual_voltage_hint, rawMv / 1000.0));
        else
            dualVoltageHint.setText(R.string.pref_dual_cell_voltage_summary);
    }

    private void render() {
        if (dialog == null || message == null) return;

        boolean done = state == STATE_RESULT || state == STATE_NO_CURRENT;

        switch (state) {
        case STATE_INTRO:
            message.setText(R.string.calibration_intro);
            break;
        case STATE_WAIT_UNPLUG:
            message.setText(replugged ? R.string.calibration_replugged : R.string.calibration_unplug);
            break;
        case STATE_MEASURING:
            message.setText(R.string.calibration_measuring);
            break;
        case STATE_RESULT:
            message.setText(R.string.calibration_done);
            break;
        default:
            message.setText(readsZero ? R.string.calibration_zero : R.string.calibration_unsupported);
        }

        progress.setVisibility(state == STATE_MEASURING || state == STATE_WAIT_UNPLUG ? View.VISIBLE : View.GONE);
        progress.setIndeterminate(state == STATE_WAIT_UNPLUG);
        progress.setProgress(samples.size());

        resultCard.setVisibility(state == STATE_RESULT ? View.VISIBLE : View.GONE);
        if (state == STATE_RESULT) {
            unitView.setText(unitName(rawPerMa));
            signView.setText(sign == 1 ? R.string.calibration_sign_standard : R.string.calibration_sign_flipped);
            drainView.setText(res.getString(R.string.ma_value, drainMa));
        }

        // The two-cell fixes stay in Settings; here they're offered only when the readings suggest them
        int currentOption = state == STATE_RESULT && offerDualCurrent ? View.VISIBLE : View.GONE;
        int voltageOption = done && offerDualVoltage ? View.VISIBLE : View.GONE;
        dualCurrent.setVisibility(currentOption);
        dualCurrentHint.setVisibility(currentOption);
        dualVoltage.setVisibility(voltageOption);
        dualVoltageHint.setVisibility(voltageOption);

        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) {
            positive.setVisibility(state == STATE_INTRO || done ? View.VISIBLE : View.GONE);
            positive.setText(done ? R.string.calibration_save : R.string.calibration_start);
        }
        if (negative != null)
            negative.setText(state == STATE_INTRO ? R.string.calibration_not_now : R.string.cancel);
    }

    private void save() {
        SharedPreferences.Editor ed = settings.edit();

        if (state == STATE_RESULT) {
            ed.putBoolean(Calibration.KEY_CALIBRATED, true);
            ed.putInt(Calibration.KEY_SIGN, sign);
            ed.putInt(Calibration.KEY_RAW_PER_MA, rawPerMa);
            ed.putBoolean(Calibration.KEY_DUAL_CELL_CURRENT, dualCurrent.isChecked());
            // Calibration now covers sign and unit, so a manual multiplier would apply them twice
            ed.putString(SettingsFragment.KEY_CURRENT_HACK_MULTIPLIER, "1");
        }

        ed.putBoolean(Calibration.KEY_DUAL_CELL_VOLTAGE, dualVoltage.isChecked());
        ed.putBoolean(Calibration.KEY_PROMPTED, true);
        ed.commit(); // Synchronously, before the host messages the service

        Calibration.apply(settings);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());
        Toast.makeText(getActivity(), R.string.calibration_saved, Toast.LENGTH_SHORT).show();
    }

    private boolean isPlugged() {
        Intent bi = batteryIntent();
        return bi != null && bi.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
    }

    private Intent batteryIntent() {
        return getActivity().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                                              Context.RECEIVER_NOT_EXPORTED);
    }

    static int unitName(int rawPerMa) {
        if (rawPerMa == Calibration.RAW_PER_MA_MA) return R.string.calibration_unit_ma;
        if (rawPerMa == Calibration.RAW_PER_MA_NA) return R.string.calibration_unit_na;
        return R.string.calibration_unit_ua;
    }
}
