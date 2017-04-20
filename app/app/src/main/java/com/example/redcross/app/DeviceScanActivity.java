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

import java.nio.ByteBuffer;
import java.util.ArrayList;
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
    private static final int SCAN_INTERVAL_MS = 10000;

    private Handler scanHandler = new Handler();
    private List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
    private ScanSettings scanSettings;
    private boolean isScanning = false;
    private Map<Character, Pair<float[], Date>> devices = new HashMap<>();

    private static final byte APP_ID = (byte) 197;
    private static final int SCAN_AGE_LIMIT = 20; // in seconds

    public void beginScanning() {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        scanSettings = scanSettingsBuilder.build();

        ScanFilter.Builder scanFiltersBuilder = new ScanFilter.Builder();
        scanFiltersBuilder.setServiceUuid(ParcelUuid.fromString("c0000000-0000-0000-0000-000000000000"),
                ParcelUuid.fromString("01111111-1111-1111-1111-111111111111"));
        //scanFilters.add(scanFiltersBuilder.build());

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
            try {
                BluetoothDevice device = result.getDevice();
                int RSSI = result.getRssi();
                String address = device.getAddress();
                byte b[] = result.getScanRecord().getServiceData().values().iterator().next();
                if (b.length == 15 && b[0] == APP_ID) {
                    parseMessage(b, (float) RSSI);
                }
            } catch (Exception e) {
            }

            // do something with RSSI value
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            // a scan error occurred
        }
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
