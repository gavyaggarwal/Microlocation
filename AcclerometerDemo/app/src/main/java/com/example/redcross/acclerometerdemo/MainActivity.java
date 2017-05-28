package com.example.redcross.acclerometerdemo;

import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import java.util.Timer;

import org.w3c.dom.Text;

import java.sql.Time;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    /* Default text fields */
    private TextView accelXTextView;
    private TextView accelYTextView;
    private TextView accelZTextView;

    private TextView velocityXTextView;
    private TextView velocityYTextView;
    private TextView velocityZTextView;

    private TextView posXTextView;
    private TextView posYTextView;
    private TextView posZTextView;

    /* Directionality text views */
    private TextView cardinalDirectionTextView;
    private TextView totalStepCountView;
    private TextView tempStepCountView;
    private TextView cumulativeDriftTextView;

    private boolean collectCumAccValues;

    /* Metric-adjusted text views */
    private TextView adjustedAccelXTextView;
    private TextView adjustedAccelYTextView;
    private TextView adjustedAccelZTextView;

    private TextView adjustedVelXTextView;
    private TextView adjustedVelYTextView;
    private TextView adjustedVelZTextView;

    private TextView adjustedPosXTextView;
    private TextView adjustedPosYTextView;

    private TextView stepPositionTextView;

    private TextView adjustedPosZTextView;

    private Button resetCumulativeDriftButton;

    private Sensor accSensor;
    private Sensor magSensor;
    private Sensor stepSensor;
    private Sensor gravSensor;
    private SensorManager sensorManger;

    private class TimedMeasure {
        public float[] values;
        public float timestamp;
    }

    private enum CardinalDirection {
        NORTH("\nNORTH", 0),
        SOUTH("\nSOUTH", 1),
        EAST("\nEAST", 2),
        WEST("\nWEST", 3);

        private String stringValue;
        private int intValue;

        private CardinalDirection(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

    }

    private static final float nanoToSec = 1.0f / 1000000000.0f;
    private static final float MEASUREMENT_DELAY_SEC= 0.0f;
    private static final int AVERAGE_MALE_STEP_LENGTH = 78; // cm
    private static final int AVERAGE_FEMALE_STEP_LENGTH = 70; // cm

    private Queue<TimedMeasure> accMeasurements;
    private Queue<TimedMeasure> magMeasurements;
    private Queue<TimedMeasure> cumAccMeasurements;
    private float[] gravity;
    private float[] velocity;
    private float[] position;

    private float[] averageDriftAcceleration;
    private float[] adjustedVelocity;
    private float[] adjustedPosition;
    private float[] stepPosition;

    private int tempStepCount;
    private int totalStepCount;
    private CardinalDirection lastCardinalDirection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManger = (SensorManager)getSystemService(SENSOR_SERVICE);
        accSensor = sensorManger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManger.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepSensor = sensorManger.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        gravSensor = sensorManger.getDefaultSensor(Sensor.TYPE_GRAVITY);

        sensorManger.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManger.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManger.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManger.registerListener(this, gravSensor, SensorManager.SENSOR_DELAY_NORMAL);

        accelXTextView = ((TextView)findViewById(R.id.accelXTextView));
        accelYTextView = ((TextView)findViewById(R.id.accelYTextView));
        accelZTextView = ((TextView)findViewById(R.id.accelZTextView));

        velocityXTextView = ((TextView)findViewById(R.id.velocityXTextView));
        velocityYTextView = ((TextView)findViewById(R.id.velocityYTextView));
        velocityZTextView = ((TextView)findViewById(R.id.velocityZTextView));

        posXTextView = ((TextView)findViewById(R.id.posXTextView));
        posYTextView = ((TextView)findViewById(R.id.posYTextView));
        posZTextView = ((TextView)findViewById(R.id.posZTextView));

        cardinalDirectionTextView = ((TextView)findViewById(R.id.cardinalDirectionTextView));
        totalStepCountView = ((TextView)findViewById(R.id.totalStepCountView));
        tempStepCountView = ((TextView)findViewById(R.id.tempStepCountView));
        cumulativeDriftTextView = ((TextView)findViewById(R.id.cumulativeDriftTextView));

        adjustedAccelXTextView = ((TextView)findViewById(R.id.adjustedAccelXTextView));
        adjustedAccelYTextView = ((TextView)findViewById(R.id.adjustedAccelYTextView));
        adjustedAccelZTextView = ((TextView)findViewById(R.id.adjustedAccelZTextView));

        adjustedVelXTextView = ((TextView)findViewById(R.id.adjustedVelXTextView));
        adjustedVelYTextView = ((TextView)findViewById(R.id.adjustedVelYTextView));
        adjustedVelZTextView = ((TextView)findViewById(R.id.adjustedVelZTextView));

        adjustedPosXTextView = ((TextView)findViewById(R.id.adjustedPosXTextView));
        adjustedPosYTextView = ((TextView)findViewById(R.id.adjustedPosYTextView));
        adjustedPosZTextView = ((TextView)findViewById(R.id.adjustedPosZTextView));

        stepPositionTextView = ((TextView)findViewById(R.id.stepPositionTextView));

        resetCumulativeDriftButton = ((Button)findViewById(R.id.resetCumulativeDriftButton));
        addListenerOnButton();

        accMeasurements = new LinkedList<TimedMeasure>();
        magMeasurements = new LinkedList<TimedMeasure>();
        cumAccMeasurements = new LinkedList<TimedMeasure>();
        collectCumAccValues = false;

        gravity = null;
        velocity = initZeroFloatArray(3);
        position = initZeroFloatArray(3);
        averageDriftAcceleration = initZeroFloatArray(3);

        adjustedVelocity = initZeroFloatArray(3);
        adjustedPosition = initZeroFloatArray(3);
        stepPosition = initZeroFloatArray(2);

        tempStepCount = 0;
        totalStepCount = 0;
        lastCardinalDirection = null;
    }

    private float[] initZeroFloatArray(int size) {
        float[] temp = new float[size];
        for (int i = 0; i < size; i++) {
            temp[0] = 0.0f;
        }
        return temp;
    }

    private void safeQueueAddWithLimit(Queue<TimedMeasure> q, float[] f, float timestamp, int limit) {
        if (limit != -1) {
            assert (q.size() <= limit);
            if (q.size() == limit) {
                q.remove();
            }
        }
        TimedMeasure tm = new TimedMeasure();
        tm.values = f;
        tm.timestamp = timestamp;

        if (q.isEmpty() || ( (tm.timestamp - q.peek().timestamp) * nanoToSec > MEASUREMENT_DELAY_SEC)) {
            q.add(tm);
        }
    }

    private CardinalDirection getCardinalDirectionFromDegreeWRTNorth(float degreeWRTNorth) {
        if (degreeWRTNorth > 315 || degreeWRTNorth < 45) {
            return CardinalDirection.NORTH;
        }
        else if (degreeWRTNorth > 45 && degreeWRTNorth < 135) {
            return CardinalDirection.EAST;
        }
        else if (degreeWRTNorth > 135 && degreeWRTNorth < 225) {
            return CardinalDirection.SOUTH;
        }
        else {
            return CardinalDirection.WEST;
        }
    }

    private float convertDegreeWRTNorthToDegreeWRTUnitCircle(float degreeWRTNorth) {
        return (360 + 90 - degreeWRTNorth) % 360;
    }

    // NOTE: 'deg' assumed to be wrt normal unit circle
    private float[] getLocationChangeInKilometers(int steps, float deg){
        float[] dxy = initZeroFloatArray(2);
        float mag = (float)(steps*AVERAGE_MALE_STEP_LENGTH)/(float)100000;
        dxy[0] = (float) (mag * Math.cos(deg));
        dxy[1] = (float) (mag * Math.sin(deg));
        return dxy;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            totalStepCount += 1;
            tempStepCount += 1;
            totalStepCountView.setText("totalStepCount: " + totalStepCount);
            tempStepCountView.setText("steps in current direction: " + tempStepCount);
        }
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            safeQueueAddWithLimit(accMeasurements, event.values, event.timestamp, 2);

            if (collectCumAccValues) {
                safeQueueAddWithLimit(cumAccMeasurements, event.values, event.timestamp, -1);
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            safeQueueAddWithLimit(magMeasurements, event.values, event.timestamp, 2);
        }
        if (accMeasurements.size() == 2 && magMeasurements.size() == 2 && gravity != null) {
            // Calculate linear acceleration and velocity
            TimedMeasure accTM = accMeasurements.remove();
            float accelX = accTM.values[0];
            float accelY = accTM.values[1];
            float accelZ = accTM.values[2];

//            // Unnecessary with the gravity sensor:
//
//            // Isolate the force of gravity via low-pass filter.
//            float alpha = 0.8f; // low-pass constant (may need to be change)
//            gravity[0] = alpha * gravity[0] + (1 - alpha) * accelX;
//            gravity[1] = alpha * gravity[1] + (1 - alpha) * accelY;
//            gravity[2] = alpha * gravity[2] + (1 - alpha) * accelZ;

            float linAccelX = accelX - gravity[0];
            float linAccelY = accelY - gravity[1];
            float linAccelZ = accelZ - gravity[2];

            float dT = nanoToSec * (accTM.timestamp - accMeasurements.peek().timestamp);
            velocity[0] += linAccelX * dT;
            velocity[1] += linAccelY * dT;
            velocity[2] += linAccelZ * dT;

            position[0] += velocity[0] * dT;
            position[1] += velocity[1] * dT;
            position[2] += velocity[2] * dT;

            // Set TextViews
            accelXTextView.setText("linAccel X: " + linAccelX);
            accelYTextView.setText("linAccel Y: " + linAccelY);
            accelZTextView.setText("linAccel Z: " + linAccelZ);

            velocityXTextView.setText("vel X: " + velocity[0]);
            velocityYTextView.setText("vel Y: " + velocity[1]);
            velocityZTextView.setText("vel Z: " + velocity[2]);

            posXTextView.setText("pos X: " + position[0]);
            posYTextView.setText("pos Y: " + position[1]);
            posZTextView.setText("pos Z: " + position[2]);


            // Calculate cardinal direction.
            float rotMatR[] = new float[9];
            float rotMatI[] = new float[9];

            boolean getRotIsASuccess = SensorManager.getRotationMatrix(rotMatR, rotMatI, accMeasurements.peek().values, magMeasurements.peek().values);

            if (getRotIsASuccess) {
                float orientation[] = new float[3];
                float remappedRotMat[] = new float[9];

                SensorManager.remapCoordinateSystem(rotMatR, SensorManager.AXIS_X, SensorManager.AXIS_Y, remappedRotMat);

                SensorManager.getOrientation(remappedRotMat, orientation);
                float orientationAzimuth = orientation[0];

                float degreeWRTNorth = (float)(Math.toDegrees(orientationAzimuth)+360) % 360; // always positive
                CharSequence cardinalDirectionText = "" + degreeWRTNorth + getCardinalDirectionFromDegreeWRTNorth(degreeWRTNorth).stringValue;
                cardinalDirectionTextView.setText(cardinalDirectionText);
//                Log.d("OnSensorChanged", "getting here!");
                if (getCardinalDirectionFromDegreeWRTNorth(degreeWRTNorth) != lastCardinalDirection) {
                    float[] dxy = getLocationChangeInKilometers(1, convertDegreeWRTNorthToDegreeWRTUnitCircle(degreeWRTNorth));
                    stepPosition[0] += dxy[0];
                    stepPosition[1] += dxy[1];
                    stepPositionTextView.setText("stepPosition: " + stepPosition[0] + ", " + stepPosition[1]);
                    tempStepCount = 0;
                }
            }

            float adjLinAccX = linAccelX - averageDriftAcceleration[0];
            float adjLinAccY = linAccelY - averageDriftAcceleration[1];
            float adjLinAccZ = linAccelZ - averageDriftAcceleration[2];

            adjustedVelocity[0] += adjLinAccX * dT;
            adjustedVelocity[1] += adjLinAccY * dT;
            adjustedVelocity[2] += adjLinAccZ * dT;

            adjustedPosition[0] += adjustedVelocity[0] * dT;
            adjustedPosition[1] += adjustedVelocity[1] * dT;
            adjustedPosition[2] += adjustedVelocity[2] * dT;

            // Set TextViews
            adjustedAccelXTextView.setText("adjusted linAccel X: " + adjLinAccX);
            adjustedAccelYTextView.setText("adjusted linAccel Y: " + adjLinAccY);
            adjustedAccelZTextView.setText("adjusted linAccel Z: " + adjLinAccZ);

            adjustedVelXTextView.setText("adjusted vel X: " + adjustedVelocity[0]);
            adjustedVelYTextView.setText("adjusted vel Y: " + adjustedVelocity[0]);
            adjustedVelZTextView.setText("adjusted vel Z: " + adjustedVelocity[0]);

            adjustedPosXTextView.setText("adjusted pos X: " + adjustedPosition[0]);
            adjustedPosYTextView.setText("adjusted pos Y: " + adjustedPosition[1]);
            adjustedPosZTextView.setText("adjusted pos Z: " + adjustedPosition[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor,  int accuracy) {
        // pass
    }

    private float[] getAverageAccFromTMQueue(Queue<TimedMeasure> tmQueue) {
        float[] tempA = initZeroFloatArray(3);

        float tmp = tmQueue.size();

        while (!tmQueue.isEmpty()) {
            TimedMeasure accTM = tmQueue.remove();
            tempA[0] += accTM.values[0];
            tempA[1] += accTM.values[1];
            tempA[2] += accTM.values[2];
        }

        tempA[0] = tempA[0] / tmp;
        tempA[1] = tempA[1] / tmp;
        tempA[2] = tempA[2] / tmp;

        return tempA;
    }

    public void addListenerOnButton() {

        resetCumulativeDriftButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                collectCumAccValues = true;
                resetCumulativeDriftButton.setClickable(false);

                new CountDownTimer(30000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        cumulativeDriftTextView.setText("" + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        collectCumAccValues = false;
                        float[] tempA = getAverageAccFromTMQueue(cumAccMeasurements);
                        cumulativeDriftTextView.setText("average drift acc: \n" + tempA[0] + ", " + tempA[1] + ", " + tempA[2]);
                        averageDriftAcceleration = tempA;
                        resetCumulativeDriftButton.setClickable(true);
                    }

                }.start();

            }

        });

    }
}
