package com.itsmartreach.libzm;

import android.bluetooth.BluetoothDevice;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


import android.bluetooth.BluetoothDevice;

import android.bluetooth.IBluetoothA2dp;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Alan on 16/4/17.
 */
public class A2dpManager {

    private IBluetoothA2dp mService = null;

    public A2dpManager() {

        try {
            Class<?>  classServiceManager = Class.forName("android.os.ServiceManager");
            Method methodGetService = classServiceManager.getMethod("getService", String.class);
            IBinder binder = null;
            try {
                binder = (IBinder) methodGetService.invoke(null, "bluetooth_a2dp");
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            mService = IBluetoothA2dp.Stub.asInterface(binder);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } /*catch (InvocationTargetException e) {
                e.printStackTrace();
            }*/
    }

    public boolean connect(BluetoothDevice device) {
        if (mService == null || device == null) {
            return false;
        }
        try {
            mService.connect(device);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean disconnect(BluetoothDevice device) {
        if (mService == null || device == null) {
            return false;
        }
        try {
            mService.disconnect(device);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
