package com.example.redcross.app.demos;

import android.content.Context;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import android.os.Handler;
import android.util.Log;

import com.example.redcross.app.NonLinearLeastSquaresSolver;
import com.example.redcross.app.TrilaterationFunction;
import com.example.redcross.app.utils.BluetoothManager;
import com.example.redcross.app.utils.DeviceManager;
import com.example.redcross.app.utils.ServerConnection;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by gavya on 4/19/2017.
 */

public class TrilaterationDemo {
    private Handler mHandler = new Handler();

    public TrilaterationDemo() {
        Log.d("Trilateration", "Started");

        // Start BLE advertising
        BluetoothManager.instance.start();

        // Start BLE scanning
        //BLEScan = new DeviceScanActivity();
        //BLEScan.beginScanning(true);

        // Get nearby devices, locations, and RSSI values from Annie
        // Array of device id (A, B, C, D, E) and location (x, y, z) and rssi
        mStatusChecker.run();

        // Map RSSI values to distances, meters (Abirami)

        // Determine position of this device from distances and locations of other devices (Kabir)

    }

    private double getDistance(double rssi) {
        return 0.5717 * Math.exp(-0.0798 * rssi) / 100;
    };

    private double[] performTrilateration(double[][] positions, double[] distances, double x, double y, double z) {
        // Perform a gradient descent to optimize the following objective function
        //$$S = \sum^{n}_{i=1}{c_i(\sqrt{(x-x_i)^2 + (y-y_i)^2 + (z-z_i)^2} - d_i)^2} + \sqrt{(x-x_{old})^2 + (y-y_{old})^2 + (z-z_{old})^2}$$
        
        double xold = x;
        double yold = y;
        double zold = z;
        if (x == 0) {
            x = 1e-7;
        }
        if (y == 0) {
            y = 1e-7;
        }
        if (z == 0) {
            z = 1e-7;
        }

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
            double dx = x - xold;
            double dy = y - yold;
            double dz = z - zold;
            double rad = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (rad != 0) {
                gradx += 2 / rad * dx;
                grady += 2 / rad * dy;
                gradz += 2 / rad * dz;
                error += rad;
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
                String testDevice = "C";

                if (DeviceManager.instance.id.equals(testDevice)) {
                    ArrayList<Map<BluetoothManager.DataType, Object>> devices = BluetoothManager.instance.getNearbyDevices();
                    double[][] positions = new double[devices.size()][3];
                    double[] distances = new double[devices.size()];

                    if (devices.size() >= 2) {
                        for (int i = 0; i < devices.size(); i++) {
                            Map<BluetoothManager.DataType, Object> data = devices.get(i);
                            positions[i][0] = ((Float)data.get(BluetoothManager.DataType.X_COORDINATE)).doubleValue();
                            positions[i][1] = ((Float)data.get(BluetoothManager.DataType.Y_COORDINATE)).doubleValue();
                            positions[i][2] = ((Float)data.get(BluetoothManager.DataType.Z_COORDINATE)).doubleValue();

                            distances[i] = getDistance(((Float)data.get(BluetoothManager.DataType.RSSI_VALUE)).doubleValue());

                            //Log.d("TEST0", "Have Device: " + String.valueOf(data[0]) + " " + String.valueOf(data[1]) + " " + String.valueOf(data[2]));
                        }


                        //NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                        //Optimum optimum = solver.solve();

                        // the answer
                        //double[] centroid = optimum.getPoint().toArray();
                        double[] centroid = performTrilateration(positions, distances, DeviceManager.instance.x, DeviceManager.instance.y, DeviceManager.instance.z);
                        //Log.d("TEST1", String.valueOf(centroid.length));
                        Log.d("TEST1", String.valueOf(distances[0]) + " " + String.valueOf(distances[1]));


                        DeviceManager.instance.x = (float) centroid[0];
                        DeviceManager.instance.y = (float) centroid[1];
                        DeviceManager.instance.z = (float) centroid[2];

                        Log.d("TEST2", String.valueOf(DeviceManager.instance.x) + " " + String.valueOf(DeviceManager.instance.y) + " " + String.valueOf(DeviceManager.instance.z));

                        // Send data to server
                    }
                }
                ServerConnection.instance.sendLocation(DeviceManager.instance.x, DeviceManager.instance.y, DeviceManager.instance.z);

            } finally {
                mHandler.postDelayed(mStatusChecker, 100);
            }
        }
    };
}
