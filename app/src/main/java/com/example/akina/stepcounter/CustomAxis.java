package com.example.akina.stepcounter;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

public class CustomAxis implements IAxisValueFormatter {

    protected String[] mHours = new String[]{
            "12A", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
            "12P", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"    };

    protected String[] mWeeks = new String[]{
            "Thu", "Fri", "Sat", "Sun", "Mon", "Tue", "Wed"
    };

    private BarLineChartBase<?> chart;
    private int pos;

    public CustomAxis(int pos, BarLineChartBase<?> chart) {
        this.chart = chart;
        this.pos = pos;
    }
    public String getFormattedValue(float value, AxisBase axis) {
        long days = (long)value;
        long year = MainActivity.determineYear(days);
        long month = MainActivity.determineMonth(days);
        long dayOfMonth = MainActivity.determineDayOfMonth(days, month + 12 * (year - MainActivity.STARTYEAR));

        String formatted = "";
        switch(pos) {
            case 0:
                formatted = mHours[(int)days % mHours.length];
                break;
            case 1:
                formatted = mWeeks[(int)days % mWeeks.length];
                break;
            case 2:
                formatted = Long.toString(dayOfMonth);
                break;
            case 3:
                formatted = MainActivity.months[(int)days % MainActivity.months.length];
                break;
        }

        return value >= 0 ? formatted : "";
    }
}
