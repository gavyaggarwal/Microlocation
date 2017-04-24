package com.example.redcross.app;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.bluetooth.le.ScanFilter;
import android.os.Handler;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
