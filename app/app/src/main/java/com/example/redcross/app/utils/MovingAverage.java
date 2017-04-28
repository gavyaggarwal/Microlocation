package com.example.redcross.app.utils;


import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
    To use, create MovingAverage object by:
        MovingAverage avg = new MovingAverage(time in ms to run average over)
    To get average call:
        avg.average()
    To add value, call:
        avg.add(float val)
 */

public class MovingAverage {
    int capacity;
    long duration; // milliseconds
    float sum;
    List<Entry> queue;

    public MovingAverage(int dur) {
        this.queue = new ArrayList<Entry>();
        this.duration = dur;
        this.sum = 0;
    }

    public float average() {
        long currentTime = System.nanoTime() / 1000000;

        // first clear out unnecessary elements
        long dt = currentTime - queue.get(0).ts;

        while (dt >= duration) {
            sum -= queue.get(0).val;
            queue.remove(0);
            dt = currentTime - queue.get(0).ts;
        };

//        for (int i=0; i < queue.size(); i++) {
//            average += queue.get(i).val;
//        }

        if (queue.size() != 0) {
            return sum / queue.size();
        } else {
            return 0;
        }
    }

    public void print() {
        String message = "";

        for (int i =0; i < queue.size(); i++) {
            message += queue.get(i).val + ", ";
        }

        Log.d("MovingAverage", message);

    }

    public void add(float val) {
        long timestamp = System.nanoTime() / 1000000;
        queue.add(new Entry(val, timestamp));
        sum += val;
    }

    public class Entry {
        float val;
        long ts;

        public Entry(float val, long ts) {
            this.val = val;
            this.ts = ts;
        }
    }
}
