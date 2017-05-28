package com.example.redcross.app;

import com.example.redcross.app.demos.TrilaterationDemo;
import com.example.redcross.app.utils.BluetoothMessage;
import com.example.redcross.app.utils.Point;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Gavy Aggarwal on 5/12/17.
 */
public class TrilaterationUnitTest {
    private static final double DELTA = 1e-3;   // Allow 1 mm of error
    private Point trilaterationIter(Point[] points, double[] distances, Point start, double ypres) {
        // Run the optimization a couple times to overcome normalization
        for (int i = 0; i < 10; i++) {
            start = TrilaterationDemo.performTrilateration(points, distances, start, 0, null);
        }
        return start;
    }
    @Test
    public void barometerCorrection() throws Exception {
        Point[] points = {};
        double[] distances = {};
        Point startPoint = new Point(100, 100, 100);
        Point res = trilaterationIter(points, distances, startPoint, 0);

        assertEquals(100, res.x, DELTA);
        assertEquals(  0, res.y, DELTA);
        assertEquals(100, res.z, DELTA);
    }
    @Test
    public void simpleBluetoothTrilateration() throws Exception {
        // Equilateral triangle centered at origin
        Point[] points = {
                new Point(0, 0, 1),
                new Point(-0.8666666666666666667, 0, -0.5),
                new Point(0.86666666666666666667, 0, -0.5)
        };
        double[] distances = {1, 1, 1};
        Point loc = trilaterationIter(points, distances, new Point(1, 1, 1), 0);
        System.out.print(loc.toString());
        assertEquals(0, loc.x, DELTA);
        assertEquals(0, loc.y, DELTA);
        assertEquals(0, loc.z, DELTA);
    }
}