package com.example.redcross.app;

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

import com.example.redcross.app.utils.BluetoothMessage;
import com.example.redcross.app.utils.Device;
import com.example.redcross.app.utils.MovingAverage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Bluetooth {
    public static Bluetooth instance = new Bluetooth();
    private Map<Character, Map<DataType, Object>> devices = new HashMap<>();
    private static final short APP_ID = 12124;
    private static final int SCAN_AGE_LIMIT = 3; // in seconds
    private long updateTime = 0;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;

    public enum DataType {
        LOCATION,
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

        Log.d("LastMessage", String.valueOf((int) message[1]) + " " + String.valueOf((int) message[2]) + " " + String.valueOf((int) message[3]) + " " + String.valueOf((int) message[4]));
        Log.d("LastMessage", String.valueOf((int) message[9]) + " " + String.valueOf((int) message[10]) + " " + String.valueOf((int) message[11]) + " " + String.valueOf((int) message[12]));
        Log.d("LastMessage", Device.instance.location.toString());


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
        if (System.currentTimeMillis() - updateTime > 1000) {
            stopAdvertising();
            startAdvertising();
            updateTime = System.currentTimeMillis();
        }
    }

    private byte[] createMessage() {
        Device dev = Device.instance;
        return new BluetoothMessage(dev.location, dev.id.charAt(0)).encode();
    }

    private void parseMessage(byte[] bytes, float rssi) {
        Map<DataType, Object> map = new HashMap<DataType, Object>();
        BluetoothMessage message = new BluetoothMessage(bytes);
        Character id = Character.valueOf(message.dev);

        MovingAverage RSSI;
        if (devices.get(id) != null) {
            RSSI = (MovingAverage) devices.get(id).get(DataType.RSSI_VALUE);
        } else {
            RSSI = new MovingAverage(5000);
        };
        RSSI.add(rssi);

        if (message.loc.isInvalid()) {
            Log.d("errPars", Device.instance.id + " " + Device.instance.location + " location parse bad: " + message.loc + " " + message.dev);
//            stopScanning();
//            startScanning();
        }
        else {
            map.put(DataType.DEVICE_NAME, id);
            map.put(DataType.LOCATION, message.loc);
            map.put(DataType.UPDATE_TIME, new Date().getTime());
            map.put(DataType.RSSI_VALUE, RSSI);

            devices.put(id, map);
            Log.d("errPars", Device.instance.id + " " + Device.instance.location + " location parse good: " + message.loc + " " + message.dev);
        }

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
                if (b != null && b.length == BluetoothMessage.SIZE) {
                    parseMessage(b.clone(), (float) result.getRssi());
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
