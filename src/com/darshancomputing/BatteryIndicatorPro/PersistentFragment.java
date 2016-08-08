/*
    Copyright (c) 2009-2016 Darshan-Josiah Barber

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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class PersistentFragment extends Fragment {
    private Intent biServiceIntent;
    private Messenger serviceMessenger;
    private final MessageHandler messageHandler = new MessageHandler();
    private final Messenger messenger = new Messenger(messageHandler);
    private BatteryInfoService.RemoteConnection serviceConnection;
    private boolean serviceConnected;
    private CurrentInfoFragment cif;
    private LogViewFragment lvf;

    public static final String FRAG_TAG = "pfrag";

    public SharedPreferences settings;
    public SharedPreferences sp_store;
    public Resources res;
    public Str str;

    private void bindService() {
        if (! serviceConnected) {
            getActivity().getApplicationContext().bindService(biServiceIntent, serviceConnection, 0);
            serviceConnected = true;
        }
    }

    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message incoming) {
            if (! serviceConnected) {
                //Log.i(LOG_TAG, "serviceConected is false; ignoring message: " + incoming);
                return;
            }

            switch (incoming.what) {
            case BatteryInfoService.RemoteConnection.CLIENT_SERVICE_CONNECTED:
                serviceMessenger = incoming.replyTo;
                sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_REGISTER_CLIENT);
                break;
            case BatteryInfoService.RemoteConnection.CLIENT_BATTERY_INFO_UPDATED:
                if (cif != null)
                    cif.batteryInfoUpdated(incoming.getData());

                if (lvf != null)
                    lvf.batteryInfoUpdated();

                break;
            default:
                super.handleMessage(incoming);
            }
        }
    }

    @Override
    public void onAttach(android.app.Activity a) {
        super.onAttach(a);

        updateResources();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        serviceConnection = new BatteryInfoService.RemoteConnection(messenger);

        biServiceIntent = new Intent(getActivity(), BatteryInfoService.class);
        getActivity().startService(biServiceIntent);
        bindService();

        settings = getActivity().getSharedPreferences(SettingsActivity.SETTINGS_FILE, Context.MODE_MULTI_PROCESS);
        sp_store = getActivity().getSharedPreferences(SettingsActivity.SP_STORE_FILE, Context.MODE_MULTI_PROCESS);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceConnected) {
            getActivity().getApplicationContext().unbindService(serviceConnection);
            serviceConnected = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_REGISTER_CLIENT);
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        updateResources();
    }

    private void updateResources() {
        res = getActivity().getResources();
        str = new Str(res);
    }

    // Public API starts here for use by BatteryInfoActivity and any of its Fragments

    public static PersistentFragment getInstance(FragmentManager fm) {
        PersistentFragment pfrag = (PersistentFragment) fm.findFragmentByTag(FRAG_TAG);

        if (pfrag == null) {
            pfrag = new PersistentFragment();
            fm.beginTransaction().add(pfrag, FRAG_TAG).commit();
        }

        return pfrag;
    }

    public void setCIF(CurrentInfoFragment f) {
        cif = f;
    }

    public void setLVF(LogViewFragment f) {
        lvf = f;
    }

    public void sendServiceMessage(int what) {
        if (serviceMessenger == null)
            return;

        Message outgoing = Message.obtain();
        outgoing.what = what;
        outgoing.replyTo = messenger;
        try { serviceMessenger.send(outgoing); } catch (android.os.RemoteException e) {}
    }

    public void closeApp() {
        SharedPreferences.Editor editor = sp_store.edit();
        editor.putBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, false);
        editor.commit();

        getActivity().finishActivity(1);

        if (serviceConnected) {
            getActivity().getApplicationContext().unbindService(serviceConnection);
            getActivity().stopService(biServiceIntent);
            serviceConnected = false;
        }

        getActivity().finish();
    }
}
