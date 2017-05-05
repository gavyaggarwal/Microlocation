package com.example.redcross.app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by kbrar on 4/28/17.
 */
public class SensorsUnitTester {
    @Test
    public void getIsMoving() throws Exception {
        Sensors sensors = new Sensors();
        assertFalse(sensors.getIsMoving());
    }

    @Test
    public void estimatedHeight() throws Exception {
        Sensors sensors = new Sensors();
        sensors.referencePressure = 0;
        sensors.referencePressure = 10;
        assertTrue(sensors.estimatedHeight() < 0);
    }

    private SensorEvent getMockSensorEvent(int sensorEventType, float[] testValues) {
        // Setup mock Sensor Event by spoofing event type/values.

        SensorEvent sensorEvent = Mockito.mock(SensorEvent.class);
        when(sensorEvent.sensor.getType()).thenReturn(sensorEventType);

//       Field valuesField = SensorEvent.class.getDeclaredField("values");

        return sensorEvent;
    }

    @Test
    public void onSensorChanged() throws Exception {
        float[] testValues = {0.01f, 0.01f, 0.01f};
        SensorEvent sensorEvent = getMockSensorEvent(Sensor.TYPE_PRESSURE, testValues);
        if (sensorEvent == null) throw new Exception(); // strange error
    }

    @Test
    public void onAccuracyChanged() throws Exception {
        // not much can be done here?
    }

}