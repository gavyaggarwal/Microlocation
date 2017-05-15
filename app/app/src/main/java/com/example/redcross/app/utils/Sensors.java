package com.example.redcross.app.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by gavya on 4/24/2017.
 */

public class Sensors implements SensorEventListener {
    public static Sensors instance = new Sensors();
    private boolean isCurrentlyMoving = false;
    private long updateTime = 0;
    private final double HPA_PER_METER = 0.11179333;
    // The pressure decreases by 0.11179333 hPa for every meter increase in altitude
    public float referencePressure;
    public float referenceHeight;
    public float currentPressure;
    private MovingAverage averager = new MovingAverage(1000);

    private class TimedMeasure {
        public float[] values;
        public float timestamp;
    }
    private Queue<TimedMeasure> accMeasurements;
    private Queue<TimedMeasure> magMeasurements;
    private float[] currentGravity;

    private float currentCardinalDirection;
    private float[] currentVelocity;
    private float[] currentAccelerometerPosition;

    private static final float nanoToSec = 1.0f / 1000000000.0f;
    private static final float MEASUREMENT_DELAY_SEC= 0.0f;

    public void setContext(Context context) {
        Log.d("Sensors", "Turning on Sensors");
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(this, barometer , SensorManager.SENSOR_DELAY_NORMAL);

        Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_NORMAL);

        currentVelocity = new float[3];
        currentVelocity[0] = 0.0f;
        currentVelocity[1] = 0.0f;
        currentVelocity[2] = 0.0f;
        currentCardinalDirection = -1.0f;

        currentGravity = null;
        currentAccelerometerPosition = null;

        accMeasurements = new LinkedList<TimedMeasure>();
        magMeasurements = new LinkedList<TimedMeasure>();    }

    public boolean getIsMoving() {
        return isCurrentlyMoving || new Date().getTime() - updateTime < 1000;
    }

    public double estimatedHeight() {
        //Server.instance.sendDebug("Estimated Height", (float) (-currentPressure / HPA_PER_METER));
        return (referencePressure - currentPressure) / HPA_PER_METER / 2 + referenceHeight;
    }

    public float getCurrentCardinalDirection() {
        return currentCardinalDirection;
    }

    public float[] getCurrentVelocity() {
        return currentVelocity;
    }

    public float[] getCurrentAccelerometerPosition() {
        return currentAccelerometerPosition;
    }

    private void safeQueueAdd(Queue<TimedMeasure> q, float[] f, float timestamp) {
        assert(q.size() <= 2);
        if (q.size() == 2) {
            q.remove();
        }
        TimedMeasure tm = new TimedMeasure();
        tm.values = f;
        tm.timestamp = timestamp;

        if (q.isEmpty() || ( (tm.timestamp - q.peek().timestamp) * nanoToSec > MEASUREMENT_DELAY_SEC)) {
            q.add(tm);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (x > 0.01 || y > 0.01 || z > 0.01) {
                if (!isCurrentlyMoving) updateTime = new Date().getTime();
                isCurrentlyMoving = true;
            }
            else {
                if (isCurrentlyMoving) updateTime = new Date().getTime();
                isCurrentlyMoving = false;
            }

        }
        if (mySensor.getType() == Sensor.TYPE_PRESSURE) {
            float pressure = event.values[0];
            averager.add(pressure);
            float averagePressure = averager.average();
            currentPressure = averagePressure;
            if (!getIsMoving()) {
                referencePressure = averagePressure;
                referenceHeight = Device.instance.y;
            }
        }
        if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float scalar = event.values[3];
            Log.d("Rotation", String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + " " + String.valueOf(scalar));
        }

        // Other accelerometer measurements:
        if (mySensor.getType() == Sensor.TYPE_GRAVITY) {
            currentGravity = event.values;
        }
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            safeQueueAdd(accMeasurements, event.values, event.timestamp);
        }
        if (mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            safeQueueAdd(magMeasurements, event.values, event.timestamp);
        }
        if (accMeasurements.size() == 2 && magMeasurements.size() == 2 && currentGravity != null) {
            // Calculate linear acceleration and velocity
            TimedMeasure accTM = accMeasurements.remove();
            float accelX = accTM.values[0];
            float accelY = accTM.values[1];
            float accelZ = accTM.values[2];

            // NOTE: Below no longer necessary if gravity sensor present
            // Isolate the force of gravity via low-pass filter.
//            float alpha = 0.8f; // low-pass constant (may need to be change)
//            currentGravity[0] = alpha * currentGravity[0] + (1 - alpha) * accelX;
//            currentGravity[1] = alpha * currentGravity[1] + (1 - alpha) * accelY;
//            currentGravity[2] = alpha * currentGravity[2] + (1 - alpha) * accelZ;

//            float linAccelX = accelX - currentGravity[0];
//            float linAccelY = accelY - currentGravity[1];
//            float linAccelZ = accelZ - currentGravity[2];

            float linAccelX = accelX - currentGravity[0];
            float linAccelY = accelY - currentGravity[1];
            float linAccelZ = accelZ - currentGravity[2];

            // Update the current velocity.
            float dT = nanoToSec * (accTM.timestamp - accMeasurements.peek().timestamp);
            // Change device relative linear acceleration values to earth relative values
            // X axis -> Longitude (+ towards East)
            // Y axis -> Latitude (+ towards North Pole)
            // Z axis -> Elevation (+ towards sky)
            float[] cRotMatR = new float[16];
            float[] cRotMatI = new float[16];
            float[] earthAcc = new float[16];
            boolean getRotIsASuccess = SensorManager.getRotationMatrix(cRotMatR, cRotMatI, currentGravity, magMeasurements.peek().values);
            if (getRotIsASuccess) {
                float[] inv = new float[16];
                android.opengl.Matrix.invertM(inv, 0, cRotMatR, 0);
                float[] temp = new float[4];
                temp[0] = linAccelX;
                temp[1] = linAccelY;
                temp[2] = linAccelZ;
                temp[3] = 0.0f;
                android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, temp, 0);

                // Update velocity, but using 'xyz' conventions for trilateration (where X is
                // along the W-E axis, Z along the S-N axis, and Y is elevation.
                currentVelocity[0] += linAccelX * dT;
                currentVelocity[1] += linAccelZ * dT; // note the Y-Z switch.
                currentVelocity[2] += linAccelY * dT;

                // Set position accordingly.
                if (currentAccelerometerPosition == null) {
                    currentAccelerometerPosition = new float[3];
                    currentAccelerometerPosition[0] = 0.0f;
                    currentAccelerometerPosition[1] = 0.0f;
                    currentAccelerometerPosition[2] = 0.0f;
                }
                currentAccelerometerPosition[0] += currentVelocity[0] * dT;
                currentAccelerometerPosition[1] += currentVelocity[1] * dT;
                currentAccelerometerPosition[2] += currentVelocity[2] * dT;
            }

            // Calculate cardinal direction.
            float rotMatR[] = new float[9];
            float rotMatI[] = new float[9];
            getRotIsASuccess = SensorManager.getRotationMatrix(rotMatR, rotMatI, accMeasurements.peek().values, magMeasurements.peek().values);
            if (getRotIsASuccess) {
                float orientation[] = new float[3];
                float remappedRotMat[] = new float[9];

                SensorManager.remapCoordinateSystem(rotMatR, SensorManager.AXIS_X, SensorManager.AXIS_Y, remappedRotMat);

                SensorManager.getOrientation(remappedRotMat, orientation);
                float orientationAzimuth = orientation[0];

                currentCardinalDirection = (float)(Math.toDegrees(orientationAzimuth)+360) % 360;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("Sensors", "Accuracy Changed: " + Integer.toString(accuracy) + " " + sensor);
    }
}
