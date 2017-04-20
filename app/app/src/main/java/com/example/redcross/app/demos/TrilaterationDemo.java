package com.example.redcross.app.demos;

import android.content.Context;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import android.os.Handler;
import android.util.Log;

import com.example.redcross.app.DeviceAdActivity;
import com.example.redcross.app.DeviceScanActivity;
import com.example.redcross.app.NonLinearLeastSquaresSolver;
import com.example.redcross.app.TrilaterationFunction;

/**
 * Created by gavya on 4/19/2017.
 */

public class TrilaterationDemo {

    private DeviceScanActivity BLEScan;
    private Handler mHandler = new Handler();

    public TrilaterationDemo(Context context) {
        Log.d("Trilateration", "Started");

        // Start BLE advertising
        DeviceAdActivity BLEAd = new DeviceAdActivity();

        // Start BLE scanning
        BLEScan = new DeviceScanActivity();
        BLEScan.beginScanning();

        // Get nearby devices, locations, and RSSI values from Annie
        // Array of device id (A, B, C, D, E) and location (x, y, z) and rssi
        mStatusChecker.run();

        // Map RSSI values to distances, meters (Abirami)

        // Determine position of this device from distances and locations of other devices (Kabir)
        // fake af test LOL
        double[][] positions = new double[][] { { 5.0, -6.0 }, { 13.0, -15.0 }, { 21.0, -3.0 }, { 12.4, -21.2 } };
        double[] distances = new double[] { 8.06, 13.97, 23.32, 15.31 };

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        Optimum optimum = solver.solve();

        // the answer
        double[] centroid = optimum.getPoint().toArray();


        // Send data to server
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d("TEST", BLEScan.getNearbyDevices().toString());
            } finally {
                mHandler.postDelayed(mStatusChecker, 100);
            }
        }
    };
}
