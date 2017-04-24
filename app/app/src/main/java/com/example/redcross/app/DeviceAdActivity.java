package com.example.redcross.app;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.redcross.app.utils.DeviceManager;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Activity for advertising to other BLE devices.
 */

@TargetApi(21)
public class DeviceAdActivity extends ListActivity {


}
