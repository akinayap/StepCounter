package com.example.akina.stepcounter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;


public class MainActivity extends AppCompatActivity {

    public static final int STARTYEAR = 2016;

    public static final String[] months = new String[] {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    Button leftBtn;
    Button rightBtn;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    public static long startHour;
    public static long startHourValue;
    public static long startDay;

    BarChart chart;
    BarDataSet dataSet;
    BarData data;

    List<BarEntry> hours = new ArrayList<>();
    List<BarEntry> days = new ArrayList<>();
    List<BarEntry> years = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                refreshGraph(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                refreshGraph(position);
            }
        });
        FitnessOptions fitnessOptions = FitnessOptions.builder().
                                        addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE).
                                        addDataType(DataType.TYPE_STEP_COUNT_DELTA).build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(this, REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this), fitnessOptions);
        } else {
            subscribe();
        }
    }

    public void subscribe(){
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .listSubscriptions(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnSuccessListener(new OnSuccessListener<List<Subscription>>() {
                    @Override
                    public void onSuccess(List<Subscription> subscriptions) {
                        for (Subscription sc : subscriptions) {
                            DataType dt = sc.getDataType();
                            readData();
                            Log.i("Google Success", "Active subscription for data type: " + dt.getName());
                        }
                    }
                });
/*        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE).addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.e("Google Success", "Successfully subscribed!");
                                    readData();
                                } else {
                                    Log.e("Google Failed", "There was a problem subscribing.", task.getException());
                                }
                            }
                        });*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                Log.e("ON ACTIVITY SUBSCRIBE", "CREATE");
                subscribe();
            }
        }
    }

    void refreshGraph(int pos){
        long today = ((currDay() - firstDay())/86400000) + 1;

        switch (pos) {
            case 0:
                drawGraph(pos, hours, 24,startHourValue - startHour, today * 24,  (today - 1) * 24);
                break;
            case 1:
                long startOfWeek = startDay - (startDay % 7) + 3;
                long latestWeek = (today - (today % 7) + 3);
                drawGraph(pos, days, 7, startOfWeek, latestWeek, latestWeek);
                break;
            case 2:
                long startMYear = determineYear(startDay);
                long startMonth = determineMonth(startDay);
                long startDayOfMonth = determineDayOfMonth(startDay, startMonth + 12 * (startMYear - STARTYEAR));


                long latestMYear = determineYear(today);
                long latestMonth = determineMonth(today);
                long lastDayOfLatestMonth = getDaysForMonth(latestMonth, latestMYear);
                long latestDayOfMonth = determineDayOfMonth(today, latestMonth + 12 * (latestMYear - STARTYEAR));

                drawGraph(pos, days, 31, startDay - startDayOfMonth, today - latestDayOfMonth + lastDayOfLatestMonth, today - latestDayOfMonth + lastDayOfLatestMonth + 15);
                break;
            case 3:
                long yearOfStartDay = determineYear(startDay);
                long monthOfStartDay = determineMonth(startDay);
                long monthOfStartYear = (yearOfStartDay - STARTYEAR) * 12 + monthOfStartDay;
                long startYear = monthOfStartYear - (monthOfStartYear % 12);


                long yearOfLatestDay = determineYear(today);
                long monthOfLatestDay = determineMonth(today);
                long monthOfLatestYear = (yearOfLatestDay - STARTYEAR) * 12 + monthOfLatestDay;
                long latestYear = monthOfLatestYear - (monthOfLatestYear % 12);
                Log.e("STARTYEAR", Long.toString(startYear));
                drawGraph(pos, years, 12, startYear, latestYear + 12, startYear);
                break;
        }
    }
    void drawGraph(int pos, List<BarEntry> datatype, final long range, final long startValue, final long endValue, long moveView){
        // Get data here
        chart = findViewById(R.id.chart);
        dataSet = new BarDataSet(datatype, "Label");
        dataSet.setBarBorderColor(android.R.color.white);
        dataSet.setBarBorderWidth(1f);
        dataSet.setHighLightAlpha(0);
        dataSet.setDrawValues(false);
        dataSet.setColor(0xFFFF6161);
        data = new BarData(dataSet);
        data.setBarWidth(0.7f);

        chart.setData(data);
        chart.moveViewToX(moveView);
        chart.getLegend().setEnabled(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(true);

        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);

        chart.setAutoScaleMinMaxEnabled(true); //Scales the Y axis as it moves along
        chart.setExtraOffsets(0, 45, 0, 0);

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
            }

            @Override
            public void onNothingSelected() {

            }
        });
        chart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                chart.highlightValues(null);
            }
        });

        IAxisValueFormatter xAxisFormatter = new CustomAxis(pos, chart);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // only intervals of 1 day
        xAxis.setLabelCount(7);
        xAxis.setValueFormatter(xAxisFormatter);
        xAxis.setAxisMinimum(startValue);
        xAxis.setAxisMaximum(endValue);
        xAxis.setDrawGridLines(true);
        xAxis.enableGridDashedLine(10f,10f,0f);
        xAxis.setGridLineWidth(0.5f);
        xAxis.setDrawAxisLine(false);

        xAxis.removeAllLimitLines();
        drawLL(xAxis, pos, startValue, endValue);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setLabelCount(5, false);
        rightAxis.setAxisMinimum(0);
        rightAxis.setDrawGridLines(true);
        rightAxis.setDrawAxisLine(false);
        rightAxis.setGranularity(1f); // intervals of 1 step

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);

        CustomMarkerView marker = new CustomMarkerView(this, R.layout.marker, pos);
        chart.setMarker(marker);

        chart.fitScreen();
        chart.invalidate();
        chart.setVisibleXRange(range, range); // Week 7, Month 30, Year 12,

        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);

        if(startValue < chart.getLowestVisibleX())
            leftBtn.setBackgroundResource(R.drawable.left_arrow);

        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                movePrev(range, chart.getLowestVisibleX(), startValue, endValue);
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveNext(range, chart.getHighestVisibleX(), startValue, endValue);
            }
        });

    }
    private void drawLL(XAxis xAxis, int pos, long dateStart, long dateEnd) {
        switch(pos)
        {
            case 0:
                for(long d = dateStart; d < dateEnd + 24; d += 24)
                {
                    LimitLine ll = new LimitLine(d, getDateStr(pos, d));
                    ll.setLineColor(Color.GRAY);
                    ll.setLineWidth(1f);
                    ll.setTextColor(Color.BLACK);
                    ll.setTextSize(10f);
                    ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
                    xAxis.addLimitLine(ll);
                }
                break;
            case 1:
                for(long d = dateStart; d < dateEnd + 7; d += 7)
                {
                    LimitLine ll = new LimitLine(d, getDateStr(pos, d));
                    ll.setLineColor(Color.GRAY);
                    ll.setLineWidth(1f);
                    ll.setTextColor(Color.BLACK);
                    ll.setTextSize(10f);
                    ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
                    xAxis.addLimitLine(ll);
                }
                break;
            case 2:
                for(long d = dateStart; d < dateEnd + 30; ++d) {
                    long year = determineYear(d);
                    long month = determineMonth(d);
                    long dayOfMonth = determineDayOfMonth(d, month + 12 * (year - STARTYEAR));

                    if (dayOfMonth == 1) {
                        LimitLine ll = new LimitLine(d, getDateStr(pos, d));
                        ll.setLineColor(Color.GRAY);
                        ll.setLineWidth(1f);
                        ll.setTextColor(Color.BLACK);
                        ll.setTextSize(10f);

                        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
                        xAxis.addLimitLine(ll);
                    }
                }
                break;
            case 3:
                for(long d = dateStart; d < dateEnd + 12; d+=12) {
                    LimitLine ll = new LimitLine(d, getDateStr(pos, d));
                    ll.setLineColor(Color.GRAY);
                    ll.setLineWidth(1f);
                    ll.setTextColor(Color.BLACK);
                    ll.setTextSize(10f);
                    ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
                    xAxis.addLimitLine(ll);
                }
                break;
        }
    }
    private void readData(){
        // Set dates for data
        long endTime = currDay();
        long startTime = firstDay();
        printRange(startTime, endTime);
        // Request data
        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder().setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED).setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms").build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .enableServerQueries()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA).bucketByTime(1, TimeUnit.HOURS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS).build();


        // Get data
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this)).readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (dataReadResponse.getBuckets().size() > 0) {
                            Log.e("Count", "Number of returned buckets of DataSets is: " + dataReadResponse.getBuckets().size());

                            int hourIterator = 0;
                            long dayAcc = 0;
                            long yearAcc = 0;
                            long currMonth = 1;

                            boolean setFirst = false;
                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                if(bucket.getDataSets().size() > 0
                                        && bucket.getDataSets().get(0).getDataPoints().size() > 0
                                        && bucket.getDataSets().get(0).getDataPoints().get(0).getDataType().getFields().size() > 0) {
                                    DataPoint dp = bucket.getDataSets().get(0).getDataPoints().get(0);
                                    Field field = dp.getDataType().getFields().get(0);
                                    int value = dp.getValue(field).asInt();

                                    if (!setFirst)
                                    {
                                        long startTime = dp.getStartTime(TimeUnit.MILLISECONDS) - firstDay();
                                        long totalDays = startTime/86400000;
                                        startHour = (startTime / 3600000) % 24;
                                        startHourValue = hourIterator;
                                        startDay = totalDays + 1;
                                        setFirst = true;
                                    }
                                    Log.e("HORUS",Integer.toString(hourIterator));
                                    hours.add(new BarEntry(hourIterator, value));
                                    dayAcc += value;
                                    yearAcc += value;

                                    printRange(dp.getStartTime(TimeUnit.MILLISECONDS), dp.getEndTime(TimeUnit.MILLISECONDS));
                                    Log.e("Value    ", "" + dp.getValue(field));
                                }
                                // For each day
                                if(hourIterator % 24 == 0)
                                {
                                    int daysCount = (hourIterator/24);
                                    days.add(new BarEntry(daysCount, dayAcc));
                                    dayAcc = 0;

                                    long currYear = determineYear(daysCount);
                                    long nextMonth = determineMonth(daysCount + 1);
                                    if(nextMonth != currMonth)
                                    {
                                        long monthOfYear = (currYear - STARTYEAR) * 12 + currMonth;
                                        years.add(new BarEntry(monthOfYear, yearAcc));
                                        yearAcc = 0;
                                        currMonth = nextMonth;
                                    }
                                }
                                ++hourIterator;
                            }

                            // Remaining values
                            if (dayAcc != 0)
                            {
                                int daysCount = (hourIterator/24);
                                days.add(new BarEntry(daysCount, dayAcc));
                            }
                            if (yearAcc != 0)
                            {
                                long remainingYear = determineYear(hourIterator/24 + 1);
                                long remainingMonth = determineMonth(hourIterator/24 + 1);
                                long monthOfYear = (remainingYear - STARTYEAR) * 12 + remainingMonth;
                                years.add(new BarEntry(monthOfYear, yearAcc));
                            }
                        }
                        refreshGraph(0);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("PRINT    ", "There was a problem reading the data.", e);
                    }
                });
    }

    public static String getDateStrFromGraphValue(int pos, float value) {

        long days = (pos == 0) ? (long)value/24 : (long)value;
        long year = (pos == 3) ? (long)value/12 + STARTYEAR : determineYear(days);
        long month = (pos == 3) ? (long)value : determineMonth(days);
        long dayOfMonth = (pos == 3) ? 1 : determineDayOfMonth(days, month + 12 * (year - STARTYEAR));


        String monthName = months[(int)month % months.length];
        String yearName = String.valueOf(year);

        String wholeDate = "";

        switch (pos)
        {
            case 0:
                int hours = (int)((value - startHourValue + startHour) % 24);
                wholeDate = hours + ":00 to " + (hours + 1) + ":00";
                break;
            case 1:
                wholeDate = monthName + " " + dayOfMonth + " " + yearName;
                break;
            case 2:
                wholeDate = monthName + " " + dayOfMonth + " " + yearName;
                break;
            case 3:
                wholeDate = monthName + " " + yearName;
        }
        return dayOfMonth <= 0 ? "" : wholeDate;
    }
    public String getDateStr(int position, float value) {

        long days, year, month, dayOfMonth;
        String monthName, yearName;

        switch (position)
        {
            case 0:
                days = (int) (value/24) + 1;
                year = determineYear(days);
                month = determineMonth(days);
                monthName = months[(int)month % months.length];
                yearName = String.valueOf(year);
                dayOfMonth = determineDayOfMonth(days, month + 12 * (year - STARTYEAR));
                return dayOfMonth <= 0 ? "" : monthName + " " + dayOfMonth + " " + yearName;
            case 1:

                days = (int) value;
                year = determineYear(days);
                month = determineMonth(days);
                monthName = months[(int)month % months.length];
                yearName = String.valueOf(year);
                dayOfMonth = determineDayOfMonth(days, month + 12 * (year - STARTYEAR));
                return dayOfMonth <= 0 ? "" : monthName + " " + dayOfMonth + " " + yearName;
            case 2:
                days = (int) value;
                year = determineYear(days);
                month = determineMonth(days);
                monthName = months[(int)month % months.length];
                yearName = String.valueOf(year);
                dayOfMonth = determineDayOfMonth(days, month + 12 * (year - STARTYEAR));
                return dayOfMonth <= 0 ? "" : monthName + " " + yearName;
            case 3:
                return Integer.toString((int)(value/12) + STARTYEAR);
        }
        return "";
    }

    // Functions used by multiple files
    public static long getDaysForMonth(long month, long year) {
        if (month == 1) {
            boolean is29Feb = false;

            if (year < 1582)
                is29Feb = (year < 1 ? year + 1 : year) % 4 == 0;
            else if (year > 1582)
                is29Feb = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);

            return is29Feb ? 29 : 28;
        }

        if (month == 3 || month == 5 || month == 8 || month == 10)
            return 30;
        else
            return 31;
    }
    public static long determineMonth(long dayOfYear) {

        int month = -1;
        int days = 0;

        while (days < dayOfYear) {
            month = month + 1;

            if (month >= 12)
                month = 0;

            long year = determineYear(days);
            days += getDaysForMonth(month, year);
        }

        return Math.max(month, 0);
    }
    public static long determineDayOfMonth(long days, long month) {
        int count = 0;
        int daysForMonths = 0;

        while (count < month) {

            long year = determineYear(daysForMonths);
            daysForMonths += getDaysForMonth(count % 12, year);
            count++;
        }

        return days - daysForMonths;
    }
    public static long determineYear(long days) {

        if(days <= (366)) // 2016 has 366 days
            return 2016;
        else if (days <= 731) // 2017 has 365 days
            return 2017;
        else if(days <= 1096) // 2018 has 365 days
            return 2018;
        else if (days <= 1461) // 2019 has 365 days
            return 2019;
        else if(days <= 1826) // 2020 has 366 days
            return 2020;
        else
            return 2021; // 2021 has 365 days

    }

    // Used for Google FIT data
    private long currDay(){
        Calendar cal = Calendar.getInstance();
        Date currTime = new Date();

        cal.setTime(currTime);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MINUTE, 59);
        return cal.getTimeInMillis();
    }
    private long firstDay(){
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.YEAR, STARTYEAR);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    // For Debugging
    private void printRange(long start, long end) {
        DateFormat timeFormat = getTimeInstance();
        DateFormat dateFormat = getDateInstance();

        Log.e("Range Start  ", timeFormat.format(start) + " " + dateFormat.format(start));
        Log.e("Range End    ", timeFormat.format(end) + " " + dateFormat.format(end));
    }

    // For translating graph
    void movePrev(long range, float lowestValue, long startValue, long endValue){

        if(lowestValue - range < startValue) {
            chart.moveViewToX(startValue);
            leftBtn.setBackgroundResource(R.drawable.left_arrow_disable);
            leftBtn.setEnabled(false);
        }
        else {
            if(lowestValue - range == startValue) {
                leftBtn.setBackgroundResource(R.drawable.left_arrow_disable);
                leftBtn.setEnabled(false);
            }
            chart.moveViewToX(lowestValue - range);
        }

        if(chart.getHighestVisibleX() + range > endValue) {
            rightBtn.setBackgroundResource(R.drawable.right_arrow);
            rightBtn.setEnabled(true);
        }
    }
    void moveNext(long range, float highestValue, long startValue, long endValue){
        if(highestValue + range > endValue) {
            chart.moveViewToX(endValue - range);
            rightBtn.setBackgroundResource(R.drawable.right_arrow_disable);
            rightBtn.setEnabled(false);
        }
        else {
            if(highestValue + range == endValue) {
                rightBtn.setBackgroundResource(R.drawable.right_arrow_disable);
                rightBtn.setEnabled(false);
            }
            chart.moveViewToX(highestValue);
        }

        if(chart.getLowestVisibleX() - range < startValue)
        {
            leftBtn.setBackgroundResource(R.drawable.left_arrow);
            leftBtn.setEnabled(true);
        }
    }
}
