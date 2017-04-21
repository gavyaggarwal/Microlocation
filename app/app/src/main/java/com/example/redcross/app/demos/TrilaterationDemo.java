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

    private double[] performTrilateration(double[][] positions, double[] distances) {
        double x = 1e-7;
        double y = 1e-7;
        double z = 1e-7;

        double eta = 0.01;
        double confidence = 1.0;

        for (int i = 0; i < 1000; i++) {
            double error = 0;
            double gradx = 0;
            double grady = 0;
            double gradz = 0;
            for (int j = 0; j < positions.length; j++) {
                double dx = x - positions[j][0];
                double dy = y - positions[j][1];
                double dz = z - positions[j][2];
                double rad = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double distanceError = rad - distances[j];
                gradx += 2 * confidence * distanceError / rad * dx;
                grady += 2 * confidence * distanceError / rad * dy;
                gradz += 2 * confidence * distanceError / rad * dz;
                error += confidence * distanceError * distanceError;
            }
            x -= gradx * eta;
            y -= grady * eta;
            z -= gradz * eta;
        }
        return new double[]{x, y, z};
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                String testDevice = "A";

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

                            //Log.d("TEST0", "Have Device: " + String.valueOf(data[0]) + " " + String.valueOf(data[1]) + " " + String.valueOf(data[2]));
                        }


                        //NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                        //Optimum optimum = solver.solve();

                        // the answer
                        //double[] centroid = optimum.getPoint().toArray();
                        double[] centroid = performTrilateration(positions, distances);
                        //Log.d("TEST1", String.valueOf(centroid.length));
                        Log.d("TEST2", String.valueOf(centroid[0]) + " " + String.valueOf(centroid[1]) + " " + String.valueOf(centroid[2]));
                        Log.d("TEST1", String.valueOf(distances[0]) + " " + String.valueOf(distances[1]));


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
