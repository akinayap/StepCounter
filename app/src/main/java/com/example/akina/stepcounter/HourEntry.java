package com.example.akina.stepcounter;

import java.util.List;

class HourEntry {
    public long steps;
    public float distance;
    public float calories;
    List<Integer> activeTime;

    HourEntry(long s, float d, float c, List<Integer>a)
    {
        steps = s;
        distance = d;
        calories = c;
        activeTime = a;
    }
}
