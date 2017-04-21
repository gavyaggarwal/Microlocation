package com.example.redcross.app;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.bluetooth.le.BluetoothLeScanner;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.Pair;

import com.example.redcross.app.utils.DeviceManager;
import com.example.redcross.app.utils.ServerConnection;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Activity for scanning and displaying available BLE devices.
 */

@TargetApi(21)
public class DeviceScanActivity extends ListActivity {
    private static final int SCAN_INTERVAL_MS = 3000;

    private Handler scanHandler = new Handler();
    private List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
    private ScanSettings scanSettings;
    private boolean isScanning = false;
    private Map<Character, Pair<float[], Date>> devices = new HashMap<>();
    private List<Integer> RSSIvals = new ArrayList<>();
    private int txPower;
    private static final byte APP_ID = (byte) 197;
    private static final int SCAN_AGE_LIMIT = 1; // in seconds

    public void beginScanning(boolean continuous) {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        scanSettings = scanSettingsBuilder.build();

        ScanFilter.Builder scanFiltersBuilder = new ScanFilter.Builder();
        scanFiltersBuilder.setServiceUuid(ParcelUuid.fromString("c0000000-0000-0000-0000-000000000000"),
                ParcelUuid.fromString("01111111-1111-1111-1111-111111111111"));
        //scanFilters.add(scanFiltersBuilder.build());

        if (continuous) {
            BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            scanner.startScan(scanFilters, scanSettings, scanCallback);
            Log.d("BLEScan", "In Progress");
        }
        else {
            scanHandler.post(scanRunnable);
        }

    }

    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

            if (isScanning) {
                scanner.stopScan(scanCallback);
                Log.d("BLEScan", "TxPower:" + String.valueOf(txPower));
                Log.d("BLEScan", "Average of vals:" + String.valueOf(getAverage(RSSIvals)));
                Collections.sort(RSSIvals);
                stripOutliers();
                double avgRSSI = getAverage(RSSIvals);
                ServerConnection.instance.sendDebug("RSSI Avg. Value", (float) avgRSSI);
                Log.d("BLEScan", "Average of middle 80%:" + String.valueOf(avgRSSI));
                Log.d("BLEScan", "Distance Prediction 1:" + String.valueOf(getDistance1(avgRSSI)));
                Log.d("BLEScan", "Distance Prediction 2:" + String.valueOf(getDistance2(avgRSSI)));
                Log.d("BLEScan", "Ended");
                RSSIvals.clear();
            } else {
                scanner.startScan(scanFilters, scanSettings, scanCallback);
                Log.d("BLEScan", "In Progress");
            }

            isScanning = !isScanning;

            scanHandler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);


            try {
                int RSSI = result.getRssi();
                byte b[] = result.getScanRecord().getServiceData().values().iterator().next();
                if (b.length == 15 && b[0] == APP_ID) {
                    parseMessage(b, (float) RSSI);
                }

                RSSIvals.add(RSSI);
                txPower = result.getScanRecord().getTxPowerLevel();
                BluetoothDevice remDevice = result.getDevice();
                ServerConnection.instance.sendDebug("RSSI Constant", RSSI);
                String address = remDevice.getAddress();
                //Log.d("Found Device", "{Device: " + address + ", RSSI Strength: " + RSSI +"}" );

            } catch (Exception e) {
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            // a scan error occurred
        }
    };

    private double getAverage(List<Integer> nums) {
        double sum = 0;
        if(!nums.isEmpty()) {
            for (Integer num : nums) {
                sum += num;
            }
            return sum / nums.size();
        }
        return sum;
    };

    private void stripOutliers() {
        int cutoff = RSSIvals.size() / 10;
        while (cutoff > 0) {
            RSSIvals.remove(0);
            RSSIvals.remove(RSSIvals.size() - 1);
            cutoff--;
        }
    }

    public double getDistance1(double rssi) {
        return 0.5717 * Math.exp(-0.0798 * rssi) / 100;
    };

    private double getDistance2(double rssi) {
        /*
        * RSSI = TxPower - 10 * n * lg(d)
        * n = 2 (in free space)
        *
        * d = 10 ^ ((TxPower - RSSI) / (10 * n))
        */

        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));

    };

    private void parseMessage(byte[] m, float rssi) {
        Character id = Character.valueOf((char) m[2]);
        //Log.d("Found Device", "{Device: " + id.toString() + ", RSSI Strength: " + rssi + "}" );
        byte[] x = {m[3], m[4], m[5], m[6]};
        byte[] y = {m[7], m[8], m[9], m[10]};
        byte[] z = {m[11], m[12], m[13], m[14]};
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

}
