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
    private boolean isScanning = false;
    private Map<Character, Pair<float[], Date>> devices = new HashMap<>();
    private List<Integer> RSSIvals = new ArrayList<>();
    private int txPower;
    private static final byte APP_ID = (byte) 197;




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
