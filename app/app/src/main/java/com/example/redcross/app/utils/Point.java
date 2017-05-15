package com.example.redcross.app.utils;

/**
 * Created by Gavy Aggarwal on 5/12/2017.
 */

public class Point {
    public float x;
    public float y;
    public float z;
    public Point() {
        x = 0;
        y = 0;
        z = 0;
    }
    public Point(float x_, float y_, float z_) {
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

}
