package com.example.redcross.app;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TrilaterationUnitTest {
    private static final double DELTA = 1e-15;

    @Test
    public void two_point_trilateration() throws Exception {
        double[][] positions = { {0, 0, -1}, {0, 0, 1} };
        double[] distances = { 1, 1 };

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        double[] centroid = optimum.getPoint().toArray();
        assertEquals(0, centroid[0], DELTA);
        assertEquals(0, centroid[1], DELTA);
        assertEquals(0, centroid[2], DELTA);
    }

    @Test
    public void three_point_trilateration() throws Exception {
        double[][] positions = { {0, 0, 1}, {0, 1, 0}, {1, 0, 0} };
        double[] distances = { 1, 1, 1 };

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        double[] centroid = optimum.getPoint().toArray();
        assertEquals(0, centroid[0], DELTA);
        assertEquals(0, centroid[1], DELTA);
        assertEquals(0, centroid[2], DELTA);
    }
}