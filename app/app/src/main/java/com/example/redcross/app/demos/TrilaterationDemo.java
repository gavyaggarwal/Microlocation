package com.example.redcross.app.demos;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.example.redcross.app.utils.Bluetooth;
import com.example.redcross.app.utils.Device;
import com.example.redcross.app.utils.MovingAverage;
import com.example.redcross.app.utils.Sensors;
import com.example.redcross.app.utils.Server;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by gavya on 4/19/2017.
 */

public class TrilaterationDemo {
    private Handler mHandler = new Handler();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TrilaterationDemo(Context context) {
        Log.d("Trilateration", "Started");

        // Turn on sensors
        Sensors.instance.setContext(context);


        // Start bluetooth advertising + scanning
        Bluetooth.instance.start();
        // Periodically perform trilateration
        mStatusChecker.run();
    }

    private double getDistance(double rssi) {
        return 0.5717 * Math.exp(-0.0798 * rssi) / 100;
    };

    private double[] performTrilateration(double[][] positions, double[] distances, double x, double y, double z, double ypres) {
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
                double confidence = 1.0 / distances[j];
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
                // 0.1 is normalization constant
                gradx += 0.1 * 2 / rad * dx;
                grady += 0.1 * 2 / rad * dy;
                gradz += 0.1 * 2 / rad * dz;
                error += rad;
            }
            grady += 2 * (y - ypres) * 3;   // Pressure is fairly accurate, give it a strong influence

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
                String freeDevice = "E";
                double y_barometer = Sensors.instance.estimatedHeight();
                Server.instance.sendDebug("Estimated Height Change", (float) y_barometer);
                if (Device.instance.id.equals(freeDevice)) {
                    ArrayList<Map<Bluetooth.DataType, Object>> devices = Bluetooth.instance.getNearbyDevices();
                    double[][] positions = new double[devices.size()][3];
                    double[] distances = new double[devices.size()];


                    for (int i = 0; i < devices.size(); i++) {
                        Map<Bluetooth.DataType, Object> data = devices.get(i);
                        positions[i][0] = ((Float)data.get(Bluetooth.DataType.X_COORDINATE)).doubleValue();
                        positions[i][1] = ((Float)data.get(Bluetooth.DataType.Y_COORDINATE)).doubleValue();
                        positions[i][2] = ((Float)data.get(Bluetooth.DataType.Z_COORDINATE)).doubleValue();

                        MovingAverage rssi = (MovingAverage) data.get(Bluetooth.DataType.RSSI_VALUE);

                        distances[i] = getDistance((double) rssi.average());

                        //Log.d("TEST0", "Have Device: " + String.valueOf(data[0]) + " " + String.valueOf(data[1]) + " " + String.valueOf(data[2]));
                    }


                    //NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                    //Optimum optimum = solver.solve();

                    // the answer
                    //double[] centroid = optimum.getPoint().toArray();
                    double[] centroid = performTrilateration(positions, distances, Device.instance.x, Device.instance.y, Device.instance.z, y_barometer);



                    Device.instance.x = (float) centroid[0];
                    Device.instance.y = (float) centroid[1];
                    Device.instance.z = (float) centroid[2];

                    Log.d("Current Location", String.valueOf(Device.instance.x) + " " + String.valueOf(Device.instance.y) + " " + String.valueOf(Device.instance.z));

                    Bluetooth.instance.updateMessage();

                    //Server.instance.sendDebug("Anchored", 0);
                } else {
                    //Server.instance.sendDebug("Anchored", 1);
                }
                Server.instance.sendLocation(Device.instance.x, Device.instance.y, Device.instance.z);

            } finally {
                mHandler.postDelayed(mStatusChecker, 50);
            }
        }
    };
}
