/*
    Copyright (c) 2010 Josiah Barber (aka Darshan)

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
    public Service service;

    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        Class c = iBinder.getClass();
        System.out.println("............................. iBinder is a: " + c);
        //java.lang.reflect.Method[] methods = c.getMethods();
        //for (int i=0; i<methods.length; i++) {
        //    System.out.println("............................. iBinder has: " + methods[i].getName());
        //}
        
        try {
            java.lang.reflect.Method m = c.getMethod("getService", (Class[]) null);
            System.out.println("............................. Found method: " + m.getName());
            service = (Service) m.invoke(iBinder, (Object[]) null);
        } catch (Exception e) {
            System.out.println("............................. Couldn't find getService()");
            service = null;
            e.printStackTrace();
        }
            
        //service = ((ServiceBinder) iBinder).getService();
    }

    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }
}
