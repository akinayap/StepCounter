package com.example.akina.stepcounter;

import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.Field;

import java.util.List;

class HourEntry {
    public long steps;
    public float distance;
    public float calories;
    public long activeTime;

    HourEntry()
    {
        steps = 0;
        distance = 0;
        calories = 0;
        activeTime = 0;
    }

    void add(long s, float d, float c, List<DataPoint>a)
    {
        steps += s;
        distance += d;
        calories += c;

        for (DataPoint dp : a) {
            for (Field field : dp.getDataType().getFields()) {
                activeTime += dp.getValue(field).asInt();
            }
        }
    }

    void clear()
    {
        steps = 0;
        distance = 0;
        calories = 0;
        activeTime = 0;
    }



}
