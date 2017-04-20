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
import com.example.redcross.app.utils.DeviceManager;
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
        BLEScan.beginScanning(true);

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
                String testDevice = "C";

                if (DeviceManager.instance.id.equals(testDevice)) {
                    ArrayList<float[]> devices = BLEScan.getNearbyDevices();
                    double[][] positions = new double[devices.size()][3];
                    double[] distances = new double[devices.size()];

                    if (devices.size() >= 2) {
                        for (int i = 0; i < devices.size(); i++) {
                            float[] data = devices.get(i);
                            positions[i][0] = (double) data[0];
                            positions[i][1] = (double) data[1];
                            positions[i][2] = (double) data[2];

                            distances[i] = BLEScan.getDistance1((double) data[3]);

                            Log.d("TEST0", "Have Device: " + String.valueOf(data[0]) + " " + String.valueOf(data[1]) + " " + String.valueOf(data[2]));
                        }


                        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                        Optimum optimum = solver.solve();

                        // the answer
                        double[] centroid = optimum.getPoint().toArray();
                        Log.d("TEST1", String.valueOf(centroid.length));
                        Log.d("TEST2", String.valueOf(centroid[0]) + " " + String.valueOf(centroid[1]) + " " + String.valueOf(centroid[2]));


                        // Send data to server
                        ServerConnection.instance.sendLocation((float) centroid[0], (float) centroid[1], (float) centroid[2]);
                    }
                } else {
                    ServerConnection.instance.sendLocation(DeviceManager.instance.x, DeviceManager.instance.y, DeviceManager.instance.z);
                }

            } finally {
                mHandler.postDelayed(mStatusChecker, 100);
            }
        }
    };
}
