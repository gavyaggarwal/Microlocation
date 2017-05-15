package com.example.redcross.app.demos;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.example.redcross.app.utils.Bluetooth;
import com.example.redcross.app.utils.Device;
import com.example.redcross.app.utils.Point;
import com.example.redcross.app.utils.MovingAverage;
import com.example.redcross.app.utils.Sensors;
import com.example.redcross.app.utils.Server;

import java.util.ArrayList;
import java.util.HashMap;
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
        if (rssi > -85.5){
            return 2.0747 * Math.exp(-0.0549 * rssi) / 100;
        } else {
            return 37.044 * Math.exp(-0.0288 * rssi) / 100;
        }
    };

    private Point performTrilateration(Point[] positions, double[] distances, Point lastLocation, double ypres) {
        // Perform a gradient descent to optimize the following objective function
        //$$S = \sum^{n}_{i=1}{c_i(\sqrt{(x-x_i)^2 + (y-y_i)^2 + (z-z_i)^2} - d_i)^2} + \sqrt{(x-x_{old})^2 + (y-y_{old})^2 + (z-z_{old})^2}$$

        Point loc = new Point(lastLocation);
        if (loc.x == 0) {
            loc.x = 1e-7f;
        }
        if (loc.y == 0) {
            loc.y = 1e-7f;
        }
        if (loc.z == 0) {
            loc.z = 1e-7f;
        }

        double eta = 0.01;

        for (int i = 0; i < 1000; i++) {
            double error = 0;
            double gradx = 0;
            double grady = 0;
            double gradz = 0;
            for (int j = 0; j < positions.length; j++) {
                double dx = loc.x - positions[j].x;
                double dy = loc.y - positions[j].y;
                double dz = loc.z - positions[j].z;
                double rad = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double confidence = 1.0 / (distances[j] + 1);
                double distanceError = rad - distances[j];
                gradx += 2 * confidence * distanceError / rad * dx;
                grady += 2 * confidence * distanceError / rad * dy;
                gradz += 2 * confidence * distanceError / rad * dz;
                error += confidence * distanceError * distanceError;
            }
            double dx = loc.x - lastLocation.x;
            double dy = loc.y - lastLocation.y;
            double dz = loc.z - lastLocation.z;
            double rad = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (rad != 0) {
                // 0.1 is normalization constant
                gradx += 0.5 * 2 / rad * dx;
                grady += 0.5 * 2 / rad * dy;
                gradz += 0.5 * 2 / rad * dz;
                error += rad;
            }
            grady += 2 * (loc.y - ypres) * 5;   // Pressure is fairly accurate, give it a strong influence

            loc.x -= gradx * eta;
            loc.y -= grady * eta;
            loc.z -= gradz * eta;

            if (loc.isInvalid()) {
                loc = lastLocation;
                Log.d("multiphone", "NaN error ahhhh");
            }
        }

        return loc;
    }

    public ArrayList<ArrayList<Integer>> combine(int n, int k) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();

        if (n <= 0 || n < k)
            return result;

        ArrayList<Integer> item = new ArrayList<Integer>();
        dfs(n, k, 1, item, result); // because it need to begin from 1

        return result;
    }

    private void dfs(int n, int k, int start, ArrayList<Integer> item,
                     ArrayList<ArrayList<Integer>> res) {
        if (item.size() == k) {
            res.add(new ArrayList<Integer>(item));
            return;
        }

        for (int i = start; i <= n; i++) {
            item.add(i);
            dfs(n, k, i + 1, item, res);
            item.remove(item.size() - 1);
        }
    }

    public Map<String, Object> getSubset(double[][] positions, double[] distances, ArrayList<Integer> indices) {

        double[] d = new double[3];
        double[][] p = new double[3][3];
        Map<String, Object> res = new HashMap<String, Object>();

        int j = 0;
        for (int i = 0; i < distances.length; i++) {
            if (indices.contains(i+1)) {
                p[j] = positions[i];
                d[j] = distances[i];
                j++;
            }
            if(j == 3) {
                res.put("pos", p);
                res.put("dis", d);
                return res;
            }
        }

        return res;
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                //String freeDevice = "E";
                double y_barometer = Sensors.instance.estimatedHeight();
                //Server.instance.sendDebug("Estimated Height Change", (float) y_barometer);
                //if (Device.instance.id.equals(freeDevice)) {

                if (Sensors.instance.getIsMoving()) {
                    ArrayList<Map<Bluetooth.DataType, Object>> devices = Bluetooth.instance.getNearbyDevices();
                    Point[] positions = new Point[devices.size()];
                    double[] distances = new double[devices.size()];


                    for (int i = 0; i < devices.size(); i++) {
                        Map<Bluetooth.DataType, Object> data = devices.get(i);
                        positions[i] = (Point) data.get(Bluetooth.DataType.LOCATION);

                        MovingAverage rssi = (MovingAverage) data.get(Bluetooth.DataType.RSSI_VALUE);

                        distances[i] = getDistance((double) rssi.average());
                        Log.d("CurDis", i + "Distance from " + String.valueOf(data.get(Bluetooth.DataType.DEVICE_NAME)) + ": " + String.valueOf((float) distances[i]) + ", RSSI: " + String.valueOf(rssi.average()));
                        //Server.instance.sendDebug("Distance from " + String.valueOf(data.get(Bluetooth.DataType.DEVICE_NAME)), (float) distances[i]);

                        //Log.d("TEST0", "Have Device: " + String.valueOf(data[0]) + " " + String.valueOf(data[1]) + " " + String.valueOf(data[2]));
                    }


                    //NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                    //Optimum optimum = solver.solve();

                    // the answer
                    //double[] centroid = optimum.getPoint().toArray();
                    Point centroid = performTrilateration(positions, distances, Device.instance.location, y_barometer);


//                    if (devices.size() > 3) {
//                        ArrayList<ArrayList<Integer>> combs = combine(devices.size(), 3);
//                        double[][] centroids = new double[combs.size()][3];
//
//                        double[] avgCentroid = new double[3];
//                        int idx = 0;
//                        double[][] p = new double[3][3];
//                        double[] d = new double[3];
//                        Map<String, Object> res = new HashMap<>();
//                        for (ArrayList<Integer> comb : combs) {
//                            res = getSubset(positions, distances, comb);
//                            centroids[idx] = performTrilateration((double[][]) res.get("pos"), (double[]) res.get("dis"), Device.instance.x, Device.instance.y, Device.instance.z, y_barometer);
//                        }
//                        for (int i = 0; i < 3; i++) {
//                            double sum = 0;
//                            for (int j=0; j < combs.size(); j++) {
//                                sum += centroids[j][i];
//                            }
//                            avgCentroid[i] = sum/combs.size();
//                        }
//                        Log.d("Point", "Averaged:" + String.valueOf(avgCentroid[0]) + " " + String.valueOf(avgCentroid[1]) + " " + String.valueOf(avgCentroid[2]));
//                    }


                    Device.instance.location = centroid;

                    if (Device.instance.location.isInvalid()) {
                        Log.d("multiphone", "NAN error ahhhh");
                    }

                    Log.d("Point", "Current: " + Device.instance.location);

                    Bluetooth.instance.updateMessage();

                    //Server.instance.sendDebug("Anchored", 0);
                } else {
                    //Server.instance.sendDebug("Anchored", 1);
                }
                Server.instance.sendLocation();

            } finally {
                mHandler.postDelayed(mStatusChecker, 50);
            }
        }
    };
}
