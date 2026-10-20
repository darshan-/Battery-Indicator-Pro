package com.darshancomputing.BatteryIndicatorPro;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

/** Shared presentation for existing settings and alarm preferences; no preference values change. */
public abstract class ProductPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public void onViewCreated(View view, Bundle state) {
        super.onViewCreated(view, state);
        float density = getResources().getDisplayMetrics().density;
        int inset = Math.round(8 * density);
        getListView().setPadding(inset, inset, inset, Math.round(24 * density));
        getListView().setClipToPadding(false);
        setDivider(new ColorDrawable(getResources().getColor(R.color.overview_divider)));
        setDividerHeight(Math.max(1, Math.round(density)));
        removeUnusedIconSpace(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();
        removeUnusedIconSpace(getPreferenceScreen());
    }

    private void removeUnusedIconSpace(PreferenceGroup group) {
        if (group == null) return;
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference.getIcon() == null) preference.setIconSpaceReserved(false);
            if (preference instanceof PreferenceGroup)
                removeUnusedIconSpace((PreferenceGroup) preference);
        }
    }
}
