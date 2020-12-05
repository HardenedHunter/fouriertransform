package com.hardenedhunter.fouriertransform;

import android.bluetooth.BluetoothAdapter;

public interface ISettingsEventListener {
    void macSelectedEvent(BluetoothAdapter bluetoothAdapter, String mac);
}
