package com.example.redcross.app.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Gavy Aggarwal on 5/12/17.
 */
public class PointUnitTest {
    @Test
    public void equalsWorks() throws Exception {
        Point l1 = new Point(1.0f, 2.0f, 3.0f);
        Point l2 = new Point(2, 4, 6);
        Point l3 = new Point(1, 2, 3);

        assertNotEquals(l1, l2);
        assertEquals(l1, l3);
        assertNotEquals(l2, l3);
    }
}