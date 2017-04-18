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
import java.util.List;

/**
 * Activity for scanning and displaying available BLE devices.
 */

@TargetApi(21)
public class DeviceScanActivity extends ListActivity {
    private static final int SCAN_INTERVAL_MS = 10000;

    private Handler scanHandler = new Handler();
    private List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
    private ScanSettings scanSettings;
    private boolean isScanning = false;

    public void beginScanning() {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        scanSettings = scanSettingsBuilder.build();

        ScanFilter.Builder scanFiltersBuilder = new ScanFilter.Builder();
        // scanFiltersBuilder.setDeviceAddress("F8:95:C7:06:81:F8");
        scanFiltersBuilder.setServiceUuid(ParcelUuid.fromString("254a134e-d9fe-4952-bc0b-8b22f87d96d7"));
        scanFilters.add(scanFiltersBuilder.build());

        scanHandler.post(scanRunnable);
    }

    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

            if (isScanning) {
                scanner.stopScan(scanCallback);
                Log.d("BLE Scan", "Ended");
            } else {
                scanner.startScan(scanFilters, scanSettings, scanCallback);
//                scanner.startScan(scanCallback);
                Log.d("BLE Scan", "In Progress");
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
            BluetoothDevice remDevice = result.getDevice();
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

}
