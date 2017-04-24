package com.example.redcross.app.utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.os.ParcelUuid;
import android.util.Log;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created by gavya on 4/14/2017.
 */

public class BluetoothManager {
    public static BluetoothManager instance = new BluetoothManager();
    private Map<Character, Pair<float[], Date>> devices = new HashMap<>();
    private static final short APP_ID = 12124;
    private static final int SCAN_AGE_LIMIT = 1; // in seconds
    private static final byte MESSAGE_SIZE = 13;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void start() {
        byte[] message = createMessage();

        ParcelUuid pUuid = new ParcelUuid(UUID.fromString(DeviceManager.instance.id + "0000000-0000-0000-0000-000000000000"));

        AdvertiseSettings adSettings = new AdvertiseSettings.Builder()
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
                .build();

        AdvertiseData adData = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .setIncludeTxPowerLevel( false )
                .addManufacturerData(APP_ID, message)
                //.addServiceData( pUuid, message )
                .build();

        advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        advertiser.startAdvertising(adSettings, adData, advertiseCallback);

        Log.d("Bluetooth", "Advertising Started");


        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        ScanSettings scanSettings = scanSettingsBuilder.build();

        scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        scanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallback);
        Log.d("Bluetooth", "Scanning Started");
    }

    public void stop() {
        advertiser.stopAdvertising(advertiseCallback);
        scanner.stopScan(scanCallback);
    }

    private byte[] createMessage() {
        byte[] message = new byte[MESSAGE_SIZE];
        byte id = (byte) DeviceManager.instance.id.charAt(0);
        byte[] x = ByteBuffer.allocate(4).putFloat(DeviceManager.instance.x).array();
        byte[] y = ByteBuffer.allocate(4).putFloat(DeviceManager.instance.y).array();
        byte[] z = ByteBuffer.allocate(4).putFloat(DeviceManager.instance.z).array();
        message[0] = id;
        for (int i = 0; i < 4; i++) {
            message[i + 1] = x[i];
        }
        for (int i = 0; i < 4; i++) {
            message[i + 5] = y[i];
        }
        for (int i = 0; i < 4; i++) {
            message[i + 9] = z[i];
        }
        return message;
    }

    private void parseMessage(byte[] m, float rssi) {
        Character id = Character.valueOf((char) m[0]);
        Log.d("Bluetooth", "{Device: " + id.toString() + ", RSSI Strength: " + rssi + "}" );
        byte[] x = {m[1], m[2], m[3], m[4]};
        byte[] y = {m[5], m[6], m[7], m[8]};
        byte[] z = {m[9], m[10], m[11], m[12]};
        float[] values = new float[4];
        values[0] = ByteBuffer.wrap(x).getFloat();
        values[1] = ByteBuffer.wrap(y).getFloat();
        values[2] = ByteBuffer.wrap(z).getFloat();
        values[3] = rssi;
        devices.put(id, new Pair<float[], Date>(values, new Date()));
    }

    public ArrayList<float[]> getNearbyDevices() {
        Date now = new Date();
        ArrayList<float[]> values = new ArrayList<>();
        Iterator<Map.Entry<Character, Pair<float[], Date>>> iter = devices.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Character, Pair<float[], Date>> entry = iter.next();
            if ((now.getTime() - entry.getValue().second.getTime()) / 1000 < SCAN_AGE_LIMIT) {
                values.add(entry.getValue().first);
            } else {
                iter.remove();
            }
        }
        return values;
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d("Bluetooth", "Advertising onStartSuccess");
        };

        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d("Bluetooth", "Advertising onStartFailure: " + errorCode);
        };
    };

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            try {
                int RSSI = result.getRssi();
                byte b[] = result.getScanRecord().getManufacturerSpecificData(APP_ID);
                if (b != null && b.length == MESSAGE_SIZE) {
                    parseMessage(b, (float) RSSI);
                }
            } catch (Exception e) {
                Log.d("Bluetooth", "Error Parsing Scan Result: " + e.toString());
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("Bluetooth", "Scanning onScanFailed: " + errorCode);
        }
    };
}
