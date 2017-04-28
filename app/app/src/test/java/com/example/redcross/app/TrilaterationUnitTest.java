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
        double[][] positions = { {0, 0, 101}, {0, 99, 0}, {100, 0, 0} };
        double[] distances = { 100, 100, 100 };

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        double[] centroid = optimum.getPoint().toArray();
        assertEquals(0, centroid[0], DELTA);
        assertEquals(0, centroid[1], DELTA);
        assertEquals(0, centroid[2], DELTA);
    }

    @Test
    public void gavy_trilateration() throws Exception {
        double[][] positions = { {0, 0, 100}, {0, 100, 0}, {100, 0, 0} };
        double[] distances = { 100, 100, 100 };

        double x = 0;
        double y = 0;
        double z = 0;


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

        for (int i = 0; i < 100; i++) {
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
            gradx += 2 / rad * dx;
            grady += 2 / rad * dy;
            gradz += 2 / rad * dz;
            error += rad;

            x -= gradx * eta;
            y -= grady * eta;
            z -= gradz * eta;
            System.out.println("x = " + String.valueOf(x) + ", y = " + String.valueOf(y) + ", z = " + String.valueOf(z));
            System.out.println("error = " + String.valueOf(error));
        }

    }
}