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

import android.content.Context;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;

public class AlarmRingtonePreference extends RingtonePreference {
    private Uri ringtone;
    private Context context;

    public AlarmRingtonePreference(Context c, AttributeSet attrs){
        super(c, attrs);
        context = c;
        Str.setResources(context.getResources());
    }

    @Override
    protected Uri onRestoreRingtone() {
        return ringtone;
    }

    public void setValue(String s) {
        String summary = Str.currently_set_to;

        if (s == null || s.equals("")) {
            ringtone = null;
            summary += Str.silent;
        } else {
            ringtone = Uri.parse(s);
            //System.out.println("..................................... ringtone: " + s);
            android.media.Ringtone r = android.media.RingtoneManager.getRingtone(context, ringtone);
            if (r == null) {
                ringtone = null;
                summary += Str.silent;
            } else {
                summary += r.getTitle(context);
            }
        }

        setSummary(summary);
    }
}
