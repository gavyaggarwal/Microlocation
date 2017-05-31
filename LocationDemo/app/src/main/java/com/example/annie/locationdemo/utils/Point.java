package com.example.annie.locationdemo.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import com.example.annie.locationdemo.MapsActivity;
import com.google.android.gms.maps.model.LatLng;

import static com.example.annie.locationdemo.R.id.map;

/**
 * Created by Annie on 5/17/17.
 */

public class Point {
    static Location location;
    static double[] initLocation;


    public double x;
    public double y;
    public double z;
    public Point() {
        x = 0;
        y = 0;
        z = 0;
    }
    public Point(double x_, double y_, double z_) {
        x = x_;
        y = y_;
        z = z_;
    }
    public Point(Point other) {
        x = other.x;
        y = other.y;
        z = other.z;
    }
    public boolean isInvalid() {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
    }
    public Point add(Point pt) {
        return new Point(x + pt.x, y + pt.y, z + pt.z);
    }
    public Point subtract(Point pt) {
        return new Point(x - pt.x, y - pt.y, z - pt.z);
    }
    public Point scale(double c) {
        return new Point(x * c, y * c, z * c);
    }
    public double getNorm() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Point))
            return false;
        Point l = (Point) obj;
        return x == l.x && y == l.y && z == l.z;
    }
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public static double[] getLatLong(Point pt) {
        double lat = initLocation[0];
        double lon = initLocation[1];
        double[] converted = {0, 0};

        try {
            Log.d("atLeastThis", "Hello");
            converted[0] = (pt.y / 111111) + lat;
            converted[1] = (pt.x / (111111 * Math.cos(converted[0])) + lon);} catch (NullPointerException e) {
            converted[0] = 34.137160;
            converted[1] = -118.123174;
        }

        return converted;
    }

    public static void getInitialPoint() {
        initLocation[0] = location.getLatitude();
        initLocation[1] = location.getLongitude();
    }

}
