package com.example.redcross.app.utils;

// Import testing apparatus
import org.junit.Test;
import static org.junit.Assert.*;

public class MovingAverageUnitTester {


    @Test
    // Open question: what should the defined behavior be for empty moving average queues? Right
    // now, this test fails by design.
    public void empty_average_test() throws Exception {
        MovingAverage avg = new MovingAverage(1000);
        assertEquals(0, avg.average(), 0);
    }

    @Test
    public void basic_average_test() throws Exception {
        MovingAverage avg = new MovingAverage(1000);

        avg.add(10);
        avg.add(10);
        Thread.sleep(1000); // force elements added before this to be excluded in average calc.
        avg.add(20);
        avg.add(20);

        assertEquals(20, avg.average(), 0);
    }

}
