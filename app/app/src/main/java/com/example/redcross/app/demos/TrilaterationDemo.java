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
import com.example.redcross.app.utils.ServerConnection;

import java.util.ArrayList;

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
        BLEScan.beginScanning(false);

        // Get nearby devices, locations, and RSSI values from Annie
        // Array of device id (A, B, C, D, E) and location (x, y, z) and rssi
        mStatusChecker.run();

        // Map RSSI values to distances, meters (Abirami)

        // Determine position of this device from distances and locations of other devices (Kabir)

    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                ArrayList<float[]> devices = BLEScan.getNearbyDevices();
                double[][] positions = new double[devices.size()][2];
                double[] distances = new double[devices.size()];

                if (devices.size() > 2) {
                    for (int i = 0; i < devices.size(); i++) {
                        float[] data = devices.get(i);
                        positions[i][0] = (double) data[0];
                        positions[i][1] = (double) data[1];

                        distances[i] = BLEScan.getDistance1((double) data[3]);
                    }


                    NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                    Optimum optimum = solver.solve();

                    // the answer
                    double[] centroid = optimum.getPoint().toArray();
                    Log.d("TEST1", String.valueOf(centroid.length));


                    // Send data to server
                    ServerConnection.instance.sendLocation((float) centroid[0], (float) centroid[1], (float) centroid[2]);
                }

            } finally {
                mHandler.postDelayed(mStatusChecker, 100);
            }
        }
    };
}
