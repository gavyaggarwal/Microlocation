package com.google.sample.echo;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by gavya on 4/23/2017.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Bluetooth {
    public static Bluetooth instance = new Bluetooth();
    public char deviceID;
    public double myLatency = 0;
    public double theirLatency = 0;
    private static final short APP_ID = 12124;
    private static final byte MESSAGE_SIZE = 9;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void start() {
        startAdvertising();
        startScanning();

        Log.d("Gavy", "Advertising and Scanning Started");
    }

    public void stop() {
        stopAdvertising();
        stopScanning();
    }

    public void startAdvertising() {
        byte[] message = createMessage();

        AdvertiseSettings adSettings = new AdvertiseSettings.Builder()
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
                .build();

        AdvertiseData adData = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .setIncludeTxPowerLevel( false )
                .addManufacturerData(APP_ID, message)
                .build();

        advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        advertiser.startAdvertising(adSettings, adData, advertiseCallback);
    }

    public void startScanning() {
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        scanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallback);
    }

    public void stopAdvertising() {
        advertiser.stopAdvertising(advertiseCallback);
    }

    public void stopScanning() {
        scanner.stopScan(scanCallback);
    }

    public void updateMessage() {
        stopAdvertising();
        startAdvertising();
    }

    private byte[] createMessage() {
        byte[] message = new byte[MESSAGE_SIZE];
        byte id = (byte) deviceID;
        byte[] l = ByteBuffer.allocate(8).putDouble(myLatency).array();
        message[0] = id;
        for (int i = 0; i < 8; i++) {
            message[i + 1] = l[i];
        }
        return message;
    }

    private void parseMessage(byte[] m, float rssi) {
        Character id = Character.valueOf((char) m[0]);

        byte[] l = {m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8]};
        theirLatency = ByteBuffer.wrap(l).getDouble();
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d("Gavy", "Advertising onStartSuccess");
        };

        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d("Gavy", "Advertising onStartFailure: " + errorCode);
        };
    };

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            try {
                byte b[] = result.getScanRecord().getManufacturerSpecificData(APP_ID);
                if (b != null && b.length == MESSAGE_SIZE) {
                    parseMessage(b, (float) result.getRssi());
                }
            } catch (Exception e) {
                Log.d("Gavy", "Error Parsing Scan Result: " + e.toString());
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("Gavy", "Scanning onScanFailed: " + errorCode);
        }
    };
}
