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

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.app.Service;

public class PluginServiceConnection implements ServiceConnection {
    public Object service;

    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        try {
            Class<?> c = iBinder.getClass();
            java.lang.reflect.Method m = c.getMethod("getService", (Class[]) null);
            service = m.invoke(iBinder, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
            service = null;
        }
    }

    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }
}
