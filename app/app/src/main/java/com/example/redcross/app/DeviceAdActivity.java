package com.example.redcross.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;

import com.example.redcross.app.utils.DeviceManager;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.UUID;

import cz.msebera.android.httpclient.entity.ByteArrayEntity;

import static android.R.attr.value;
import static java.security.AccessController.getContext;

/**
 * Activity for advertising to other BLE devices.
 */

@TargetApi(21)
public class DeviceAdActivity extends ListActivity {
    private static final int AD_INTERVAL_MS = 10000;

    private Handler adHandler = new Handler();
    private AdvertiseSettings adSettings;
    private AdvertiseData adData;
    private boolean isAdvertising = false;

    private static final byte APP_ID = (byte) 197;

    public DeviceAdActivity() {
        beginAdvertising(true);
    }

    private byte[] getMessage() {
        byte tag = 0;
        byte id = (byte) DeviceManager.instance.id.charAt(0);
        byte[] x = ByteBuffer.allocate(4).putFloat(DeviceManager.instance.x).array();
        byte[] y = ByteBuffer.allocate(4).putFloat(DeviceManager.instance.y).array();
        byte[] z = ByteBuffer.allocate(4).putFloat(DeviceManager.instance.z).array();
        byte[] m = {APP_ID, tag, id, x[0], x[1], x[2], x[3], y[0], y[1], y[2], y[3], z[0], z[1], z[2], z[3]};
        return m;
    }

    public void beginAdvertising(boolean continuous) {
        byte[] message = getMessage();

        ParcelUuid pUuid = new ParcelUuid(UUID.fromString(DeviceManager.instance.id + "0000000-0000-0000-0000-000000000000"));

        adSettings = new AdvertiseSettings.Builder()
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
                .build();

        adData = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .setIncludeTxPowerLevel(false)
                //.addServiceUuid( pUuid )
                .addServiceData( pUuid, message )
                .build();

        if (continuous) {
            BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter(). getBluetoothLeAdvertiser() ;
            advertiser.startAdvertising(adSettings, adData, adCallback);
            Log.d("BLEAd", "In Progress");
        }
        else {
            adHandler.post(adRunnable);
        }

    }

    private Runnable adRunnable = new Runnable() {
        @Override
        public void run() {
            BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter(). getBluetoothLeAdvertiser() ;

            if (isAdvertising) {
                advertiser.stopAdvertising(adCallback);
                Log.d("BLEAd", "Ended");
            } else {
//                scanner.startScan(scanFilters, scanSettings, scanCallback);
                advertiser.startAdvertising(adSettings, adData, adCallback);
                Log.d("BLEAd", "In Progress");
            }

            isAdvertising = !isAdvertising;

            adHandler.postDelayed(this, AD_INTERVAL_MS);
        }
    };


    private AdvertiseCallback adCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d( "BLEAd", "Advertising onStartSuccess ");
        };

        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d( "BLEAd", "Advertising onStartFailure: " + errorCode );
        };
    };

}
