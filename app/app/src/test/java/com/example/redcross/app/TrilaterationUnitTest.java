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
    @Test
    public void barometerCorrection() throws Exception {
        Point[] points = {};
        double[] distances = {};
        Point startPoint = new Point(100, 100, 100);
        Point startPoint2 = new Point(100.0, 0.09999999847412118, 100.0);
        System.out.print(TrilaterationDemo.performTrilateration(points, distances, startPoint, 0, null).toString());

        System.out.print(TrilaterationDemo.performTrilateration(points, distances, startPoint2, 0, null).toString());


    }
    @Test
    public void simpleBluetoothTrilateration() throws Exception {
        Point[] points = {
                new Point(0, 0, 1),
                new Point(-0.866666666, 0, -0.5),
                new Point(0.8666666666, 0, -0.5)
        };
        double[] distances = {1, 1, 1};
        Point loc = TrilaterationDemo.performTrilateration(points, distances, new Point(1, 1, 1), 0, null);
        System.out.print(loc.toString());
        assertEquals(0, loc.x, 1e-7);
        assertEquals(0, loc.y, 1e-7);
        assertEquals(0, loc.z, 1e-7);
    }
}