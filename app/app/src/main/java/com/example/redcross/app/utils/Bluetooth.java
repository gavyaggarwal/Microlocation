package com.example.redcross.app.utils;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by gavya on 4/23/2017.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Bluetooth {
    public static Bluetooth instance = new Bluetooth();
    private Map<Character, Map<DataType, Object>> devices = new HashMap<>();
    private static final short APP_ID = 12124;
    private static final int SCAN_AGE_LIMIT = 1; // in seconds
    private static final byte MESSAGE_SIZE = 13;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;

    public enum DataType {
        X_COORDINATE,
        Y_COORDINATE,
        Z_COORDINATE,
        RSSI_VALUE,
        DEVICE_NAME,
        UPDATE_TIME
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void start() {
        startAdvertising();
        startScanning();

        Log.d("Bluetooth", "Advertising and Scanning Started");
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
        byte id = (byte) Device.instance.id.charAt(0);
        byte[] x = ByteBuffer.allocate(4).putFloat(Device.instance.x).array();
        byte[] y = ByteBuffer.allocate(4).putFloat(Device.instance.y).array();
        byte[] z = ByteBuffer.allocate(4).putFloat(Device.instance.z).array();
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
        Map<DataType, Object> map = new HashMap<DataType, Object>();
        Character id = Character.valueOf((char) m[0]);

        MovingAverage RSSI;
        if (devices.get(id) != null) {
            RSSI = (MovingAverage) devices.get(id).get(DataType.RSSI_VALUE);
        } else {
            RSSI = new MovingAverage(5000);
        };
        RSSI.add(rssi);

        byte[] x = {m[1], m[2], m[3], m[4]};
        byte[] y = {m[5], m[6], m[7], m[8]};
        byte[] z = {m[9], m[10], m[11], m[12]};
        map.put(DataType.DEVICE_NAME, id);
        map.put(DataType.X_COORDINATE, ByteBuffer.wrap(x).getFloat());
        map.put(DataType.Y_COORDINATE, ByteBuffer.wrap(y).getFloat());
        map.put(DataType.Z_COORDINATE, ByteBuffer.wrap(z).getFloat());
        map.put(DataType.UPDATE_TIME, new Date().getTime());
        map.put(DataType.RSSI_VALUE, RSSI);

        devices.put(id, map);
    }

    public ArrayList<Map<DataType, Object>> getNearbyDevices() {
        long now = new Date().getTime();
        ArrayList<Map<DataType, Object>> values = new ArrayList<>();
        Iterator<Map.Entry<Character, Map<DataType, Object>>> iter = devices.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Character, Map<DataType, Object>> entry = iter.next();
            if ((now - (long) entry.getValue().get(DataType.UPDATE_TIME)) / 1000 < SCAN_AGE_LIMIT) {
                values.add(entry.getValue());
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
                byte b[] = result.getScanRecord().getManufacturerSpecificData(APP_ID);
                if (b != null && b.length == MESSAGE_SIZE) {
                    parseMessage(b, (float) result.getRssi());
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
