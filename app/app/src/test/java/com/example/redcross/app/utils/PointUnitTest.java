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
    @Test
    public void mathWorks() throws Exception {
        Point l1 = new Point(1, 2, 3);
        Point l2 = new Point(2, 4, 6);
        Point l3 = new Point(3, 6, 9);
        Point l4 = new Point(-1, -2, -3);

        assertEquals(l2.add(l1), l3);
        assertEquals(l1.add(l2), l3);
        assertEquals(l4.add(l3), l2);

        assertEquals(l3.subtract(l1), l2);
        assertEquals(l3.subtract(l2), l1);
        assertEquals(l2.subtract(l4), l3);

        assertEquals(l1.scale(2), l2);
        assertEquals(l2.scale(0.5f), l1);
    }
}