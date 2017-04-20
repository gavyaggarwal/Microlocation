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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private List<Integer> RSSIvals = new ArrayList<>();
    private int txPower;

    public void beginScanning() {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        scanSettings = scanSettingsBuilder.build();

        ScanFilter.Builder scanFiltersBuilder = new ScanFilter.Builder();
        scanFiltersBuilder.setServiceUuid(ParcelUuid.fromString("a0000000-0000-0000-0000-000000000000"),
                ParcelUuid.fromString("01111111-1111-1111-1111-111111111111"));
        scanFilters.add(scanFiltersBuilder.build());

        scanHandler.post(scanRunnable);
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

            int RSSI = result.getRssi();
            RSSIvals.add(RSSI);
            txPower = result.getScanRecord().getTxPowerLevel();
            BluetoothDevice remDevice = result.getDevice();
            ServerConnection.instance.sendDebug("RSSI Constant", RSSI);
            String address = remDevice.getAddress();
            Log.d("Found Device", "{Device: " + address + ", RSSI Strength: " + RSSI +"}" );

            // do something with RSSI value
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
            RSSIvals.remove(RSSIvals.size()-1);
            cutoff--;
        }
    }

    private double getDistance1(double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
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
}
