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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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
    private AdvertiseData adScanResponse;
    private boolean isAdvertising = false;

    public void beginLocationListening(Activity c) {
        Log.d( "LOC_DEB", "Start listening");

        InfoManager testData = new InfoManager();
        testData.deviceLocation(c, this);
    }

    public void beginAdvertising(Float message) {
        Log.d( "BLE AD", "Advertising gets called ");
        ByteBuffer.allocate(4).putFloat(message).array();
        byte[] byteMess = new byte[4];

        ParcelUuid pUuid = new ParcelUuid(UUID.fromString(DeviceManager.instance.id + "0000000-0000-0000-0000-000000000000"));

//        InfoManager testData = new InfoManager();
//        String locData = testData.deviceLocation(c);

        Log.d("par id", pUuid.toString());
        Log.d( "BLE AD", "Advertising uuid set up ");
        adSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
                .build();

        adData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid( pUuid )
                .addServiceData( pUuid, byteMess)
                .build();
        Log.d( "BLE AD", "Advertising setup Success ");

        adHandler.post(adRunnable);
    }

    private Runnable adRunnable = new Runnable() {
        @Override
        public void run() {
            BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter(). getBluetoothLeAdvertiser() ;

            if (isAdvertising) {
                advertiser.stopAdvertising(adCallback);
                Log.d("BLE Ad", "Ended");
            } else {
//                scanner.startScan(scanFilters, scanSettings, scanCallback);
                advertiser.startAdvertising(adSettings, adData, adScanResponse, adCallback);
                Log.d("BLE Ad", "In Progress");
            }

            isAdvertising = !isAdvertising;

            adHandler.postDelayed(this, AD_INTERVAL_MS);
        }
    };


    private AdvertiseCallback adCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d( "BLE AD", "Advertising onStartSuccess ");
        };

        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d( "BLE AD", "Advertising onStartFailure: " + errorCode );
        };
    };

}
