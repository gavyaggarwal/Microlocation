package com.example.redcross.app.utils;

/**
 * Created by Gavy Aggarwal on 5/12/2017.
 */

public class Point {
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

    public double[] getLatLong(Point point) {
        double lat = 34.139922;
        double lon =  -118.124935;
        double[] converted = {0, 0};

        converted[0] = (point.y / 111111) + lat;
        converted[1] = (point.x / (111111 * Math.cos(converted[0])) + lon);

        return converted;
    }

}
