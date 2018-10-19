package com.example.akina.stepcounter;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;

    public static final int STARTYEAR = 2016;
    public static final String[] months = new String[]{
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    CalendarView calView;
    Button leftBtn, rightBtn;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;

    BarData data;
    BarChart chart;
    BarDataSet dataSet;

    List<BarEntry> entries = new ArrayList<>();
    HourEntry total = new HourEntry();
    List<HourEntry> totalHrs = new ArrayList<>();

    int currPos;

    long today, currHour;
    static long currWeek, currMonth, currYear;

    DateFormat dateFormat;
    DecimalFormat df;
    TextView topText, dateText, stepText, calText, activeText, distText, activeT, distT, calT;

    // Create a reference for the tracker - Google Analytics
    Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing tracking here

        // Using Google Analytics
        AnalyticsTools application = (AnalyticsTools) getApplication();
        mTracker = application.getDefaultTracker();

        // Using Firebase
        //mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        dateFormat = getDateInstance();
        df = new DecimalFormat("#.##");
        topText = findViewById(R.id.currTimeText);
        dateText = findViewById(R.id.date_indicator);
        activeT = findViewById(R.id.active_title);
        distT = findViewById(R.id.dist_title);
        calT = findViewById(R.id.cal_title);

        stepText = findViewById(R.id.data_val);
        calText = findViewById(R.id.cal_val);
        activeText = findViewById(R.id.active_val);
        distText = findViewById(R.id.dist_val);
        calView = findViewById(R.id.calendarView);
        calView.setVisibility(CalendarView.GONE);

        calView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                // display the selected date by using a toast
                //Toast.makeText(getApplicationContext(), dayOfMonth + "/" + month + "/" + year, Toast.LENGTH_LONG).show();

                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth, 0,0,0);
                long changeDay = cal.getTimeInMillis();
                setCurrDays(changeDay);
                readData(currPos);
                calView.setVisibility(CalendarView.GONE);
                rightBtn.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
            }
        });

        initDays();

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                currPos = pos;
                readData(pos);



                // The Google Analytics and Firebase code is used where you want to log info on button press.

                // Using Google Analytics
                // setCategory is used to group multiple actions into the same category
                // setAction is used to determine what is the final action
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Select")
                        .setAction("Tab " + pos)
                        .build());


                // Using Firebase
                // ClickedOnTab (Created custom tab)
                /*
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Clicked on tab" + pos);
                mFirebaseAnalytics.logEvent("ClickedOnTab", bundle);
                */

                topText.setText(dateFormat.format(currHour));
                //if(today == currHour)
                //    topText.setText("Today");

                dateText.setText(dateFormat.format(currHour));

                chart.highlightValue(null);
                topText.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);

                calView.setVisibility(CalendarView.GONE);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .addDataType(DataType.TYPE_ACTIVITY_SAMPLES)
                .addDataType(DataType.TYPE_DISTANCE_DELTA)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(this, REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this), fitnessOptions);
        } else {
            subscribe();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                subscribe();
            }
        }
    }
    public void subscribe() {
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.i("Success", "Successfully subscribed!");
                                    readData(0);
                                } else {
                                    Log.w("Failed", "There was a problem subscribing.", task.getException());
                                }
                            }
                        });
    }

    void initDays(){
        Calendar cal = Calendar.getInstance();
        Date currTime = new Date();

        cal.setTime(currTime);
        // Set Hour
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        currHour = cal.getTimeInMillis();
        today = currHour;

        // Set Week
        cal.add(Calendar.DAY_OF_WEEK, -cal.get(Calendar.DAY_OF_WEEK) + 1);
        currWeek = cal.getTimeInMillis();

        // Set Month
        cal.setTime(currTime);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        currMonth = cal.getTimeInMillis();

        // Set Year
        cal.set(Calendar.MONTH, 0);
        currYear = cal.getTimeInMillis();
    }
    void setCurrDays(long changeDay){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(changeDay);
        // Set Hour
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        currHour = cal.getTimeInMillis();
        today = currHour;

        // Set Week
        cal.add(Calendar.DAY_OF_WEEK, -cal.get(Calendar.DAY_OF_WEEK) + 1);
        currWeek = cal.getTimeInMillis();

        // Set Month
        cal.setTimeInMillis(changeDay);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        currMonth = cal.getTimeInMillis();

        // Set Year
        cal.set(Calendar.MONTH, 0);
        currYear = cal.getTimeInMillis();
    }

    void drawGraph(final int pos) {
        // Get data here
        //moveView = 0;
        int max = 0;
        switch(pos)
        {
            case 0:
                max = 23;
                break;
            case 1:
                max = 6;
                break;
            case 2:
                max = 30;
                break;
            case 3:
                max = 11;
                break;
        }


        chart = findViewById(R.id.chart);
        dataSet = new BarDataSet(entries, "Label");

        dataSet.setBarBorderColor(android.R.color.white);
        dataSet.setBarBorderWidth(1f);
        dataSet.setHighLightAlpha(0);
        dataSet.setDrawValues(false);
        dataSet.setColor(0xFFFF6161);
        data = new BarData(dataSet);
        data.setBarWidth(0.7f);

        chart.setData(data);
        chart.getLegend().setEnabled(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(true);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setExtraOffsets(0, 45, 0, 0);

        IAxisValueFormatter xAxisFormatter = new CustomAxis(pos, chart);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // only intervals of 1 day
        xAxis.setLabelCount(7);
        xAxis.setValueFormatter(xAxisFormatter);
        xAxis.setDrawGridLines(true);
        xAxis.enableGridDashedLine(10f,10f,0f);
        xAxis.setGridLineWidth(0.5f);
        xAxis.setDrawAxisLine(false);

        xAxis.setAxisMinimum(0); // So that first element start in the middle
        xAxis.setAxisMaximum(max); // So that last element ends in the middle

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
        chart.notifyDataSetChanged();
        chart.invalidate();

        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int i = (int)e.getX();
                stepText.setText(Long.toString(totalHrs.get(i).steps) + " steps");
                calText.setText(Long.toString((int)totalHrs.get(i).calories) + " kcal");
                activeText.setText(Long.toString((totalHrs.get(i).activeTime / 60000)) + " min");
                distText.setText(Double.toString((double)Math.round((totalHrs.get(i).distance / 1000) * 100d) / 100d) + " km");

                calT.setVisibility(View.VISIBLE);
                distT.setVisibility(View.VISIBLE);
                calText.setVisibility(View.VISIBLE);
                distText.setVisibility(View.VISIBLE);

                activeT.setText("Active time");
                calT.setText("Calories burnt");
                distT.setText("Distance");

                dateText.setText(MainActivity.getDateStrFromGraphValue(pos, e.getX()));

                // Hide Text
                topText.setVisibility(View.INVISIBLE);
                rightBtn.setVisibility(View.INVISIBLE);
                leftBtn.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onNothingSelected() {
                updateText(pos);
                // Show Text
                if(pos == 0)
                {
                    activeT.setText("Total active time");
                    calT.setText("Total calories burnt");
                    distT.setText("Total distance");
                }
                else
                {
                    activeT.setText("Daily average active time");
                    calT.setVisibility(View.GONE);
                    distT.setVisibility(View.GONE);
                    calText.setVisibility(View.GONE);
                    distText.setVisibility(View.GONE);
                }

                topText.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
            }
        });

        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                movePrev(pos);
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveNext(pos);
            }
        });

        updateText(pos);
    }
    private void readData(final int pos){
        long startTime = 0;
        long endTime = 0;

        TimeUnit t = TimeUnit.DAYS;
        Calendar cal = Calendar.getInstance();

        switch (pos)
        {
            case 0:
                startTime = currHour;
                t = TimeUnit.HOURS;
                cal.setTimeInMillis(currHour);
                cal.add(Calendar.HOUR, 24);
                cal.add(Calendar.SECOND, -1);
                endTime = cal.getTimeInMillis();
                break;
            case 1:
                startTime = currWeek;
                cal.setTimeInMillis(currWeek);
                cal.add(Calendar.DAY_OF_WEEK, 7);
                cal.add(Calendar.SECOND, -1);
                endTime = cal.getTimeInMillis();
                break;
            case 2:
                startTime = currMonth;
                cal.setTimeInMillis(currMonth);
                cal.add(Calendar.MONTH, 1);
                cal.add(Calendar.SECOND, -1);
                endTime = cal.getTimeInMillis();
                break;
            case 3:
                startTime = currYear;
                cal.setTimeInMillis(currYear);
                cal.add(Calendar.MONTH, 12);
                cal.add(Calendar.SECOND, -1);
                endTime = cal.getTimeInMillis();
                break;
        }

        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder().setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED).setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms").build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                //.enableServerQueries()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, t)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS).build();

        // Get data
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this)).readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (dataReadResponse.getBuckets().size() > 0) {
                            totalHrs.clear();
                            entries.clear();
                            total.clear();
                            int i = 0;
                            long yearI = 0;
                            int dayAcc = 0;
                            int startMonth = 0;
                            boolean lastDayOfMonth = false;

                            HourEntry dayTotal = new HourEntry();

                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();

                                long s = 0;
                                float d = 0;
                                float c = 0;
                                List<DataPoint> a = new ArrayList<>();


                                for (int dataNo = 0; dataNo < dataSets.size(); ++dataNo)
                                {
                                    DataSet dataSet = dataSets.get(dataNo);

                                    if(dataSet.getDataPoints().size() > 0)
                                    {
                                        DataPoint dp = dataSet.getDataPoints().get(0);
                                        Field field = dp.getDataType().getFields().get(0);

                                        switch (dataNo)
                                        {
                                            case 0:
                                                if(pos == 3)
                                                {
                                                    yearI = dp.getEndTime(TimeUnit.MILLISECONDS);
                                                    dayAcc += dp.getValue(field).asInt();

                                                    Calendar today = Calendar.getInstance();
                                                    today.setTimeInMillis(dp.getStartTime(TimeUnit.MILLISECONDS));

                                                    Calendar tomorrow = Calendar.getInstance();
                                                    tomorrow.setTimeInMillis(today.getTimeInMillis());
                                                    tomorrow.add(Calendar.DAY_OF_YEAR, 1);

                                                    if(today.get(Calendar.MONTH) != tomorrow.get(Calendar.MONTH))
                                                    {
                                                        lastDayOfMonth = true;
                                                        startMonth = today.get(Calendar.MONTH);
                                                        entries.add(new BarEntry(today.get(Calendar.MONTH), dayAcc));
                                                        dayAcc = 0;
                                                    }
                                                }
                                                else
                                                    if(dp.getValue(field).getFormat() == 1)
                                                        entries.add(new BarEntry(i, dp.getValue(field).asInt()));

                                                if(dp.getValue(field).getFormat() == 1)
                                                    s = dp.getValue(field).asInt();
                                                break;
                                            case 1:
                                                if(dp.getValue(field).getFormat() == 2)
                                                    d = dp.getValue(field).asFloat();
                                                break;

                                            case 2:
                                                if(dp.getValue(field).getFormat() == 2)
                                                    c = dp.getValue(field).asFloat();
                                                break;

                                            case 3:
                                                //if(dp.getValue(field).getFormat() == 1)
                                                    a = dataSet.getDataPoints();
                                                break;
                                        }
                                    }
                                }

                                if(pos == 3)
                                {
                                    dayTotal.add(s, d, c, a);
                                    if(lastDayOfMonth)
                                    {
                                        while(startMonth != (totalHrs.size() % 12))
                                            totalHrs.add(new HourEntry());
                                        totalHrs.add(dayTotal);
                                        dayTotal = new HourEntry();
                                        lastDayOfMonth = false;
                                    }
                                }
                                else
                                {
                                    totalHrs.add(new HourEntry(s, d, c, a));
                                }

                                total.add(s, d, c, a);
                                ++i;
                            }
                            if(dayAcc != 0)
                            {
                                Calendar today = Calendar.getInstance();
                                today.setTimeInMillis(yearI);
                                entries.add(new BarEntry(today.get(Calendar.MONTH), dayAcc));

                                while(today.get(Calendar.MONTH) != (totalHrs.size() % 12))
                                    totalHrs.add(new HourEntry());
                                totalHrs.add(dayTotal);
                            }


                        }
                        drawGraph(pos);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("PRINT    ", "There was a problem reading the data.", e);
            }
        });
    }
    public static String getDateStrFromGraphValue(int pos, float value) {
        String wholeDate = "";

        switch(pos)
        {
            case 0:
                wholeDate = (int)value + ":00 to " + (int)(value + 1) + ":00";
                break;
            case 1:
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(currWeek);
                cal.add(Calendar.DAY_OF_WEEK, (int)value);

                DateFormat dateFormat = getDateInstance();
                wholeDate = dateFormat.format(cal.getTimeInMillis());
                break;
            case 2:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currMonth);
                cal.add(Calendar.DAY_OF_MONTH, (int)value);

                SimpleDateFormat simpleDateformat = new SimpleDateFormat("dd MMM");
                wholeDate = simpleDateformat.format(cal.getTimeInMillis());
                break;
            case 3:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currYear);
                cal.add(Calendar.MONTH, (int)value);

                simpleDateformat = new SimpleDateFormat("MMM YYYY");
                wholeDate = simpleDateformat.format(cal.getTimeInMillis());
                break;
        }
        return wholeDate;
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

    // For Debugging
    private void printRange(long start, long end) {
        DateFormat timeFormat = getTimeInstance();
        DateFormat dateFormat = getDateInstance();

        Log.e("Range Start  ", timeFormat.format(start) + " " + dateFormat.format(start));
        Log.e("Range End    ", timeFormat.format(end) + " " + dateFormat.format(end));
    }

    // For translating graph
    void movePrev(int pos){
        switch(pos)
        {
            case 0:
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(currHour);
                cal.add(Calendar.DAY_OF_YEAR, -1);
                currHour = cal.getTimeInMillis();
                readData(pos);
                break;
            case 1:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currWeek);
                cal.add(Calendar.DAY_OF_YEAR, -7);
                currWeek = cal.getTimeInMillis();
                readData(pos);
                break;
            case 2:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currMonth);
                cal.add(Calendar.MONTH, -1);
                currMonth = cal.getTimeInMillis();
                readData(pos);
                break;
            case 3:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currYear);
                cal.add(Calendar.MONTH, -12);
                currYear = cal.getTimeInMillis();
                readData(pos);
                break;
        }
    }
    void moveNext(int pos){
        switch(pos)
        {
            case 0:
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(currHour);
                cal.add(Calendar.DAY_OF_YEAR, 1);
                currHour = cal.getTimeInMillis();
                break;
            case 1:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currWeek);
                cal.add(Calendar.DAY_OF_YEAR, 7);
                currWeek = cal.getTimeInMillis();
                break;
            case 2:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currMonth);
                cal.add(Calendar.MONTH, 1);
                currMonth = cal.getTimeInMillis();
                break;
            case 3:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currYear);
                cal.add(Calendar.MONTH, 12);
                currYear = cal.getTimeInMillis();
                break;
        }
        readData(pos);
    }

    void updateText(int pos) {
        dateFormat = getDateInstance();

        switch(pos)
        {
            case 0:
                activeT.setText("Total active time");
                topText.setText(dateFormat.format(currHour));
                //if(today == currHour)
                //    topText.setText("Today");

                dateText.setText(dateFormat.format(currHour));

                stepText.setVisibility(View.VISIBLE);
                calText.setVisibility(View.VISIBLE);
                calT.setVisibility(View.VISIBLE);
                activeText.setVisibility(View.VISIBLE);
                distText.setVisibility(View.VISIBLE);
                distT.setVisibility(View.VISIBLE);

                stepText.setText("Total " + total.steps + " steps");
                calText.setText(Long.toString((int)total.calories) + " kcal");
                activeText.setText(Long.toString((total.activeTime / 60000)) + " min");
                distText.setText(Double.toString((double)Math.round((total.distance / 1000) * 100d) / 100d) + " km");

                break;
            case 1:
                activeT.setText("Daily average active time");
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(currWeek);
                cal.add(Calendar.DAY_OF_WEEK, 7);
                topText.setText(dateFormat.format(currWeek) + " - "+ dateFormat.format(cal.getTimeInMillis()));

                dateText.setText(dateFormat.format(currWeek) + " - "+ dateFormat.format(cal.getTimeInMillis()));

                stepText.setVisibility(View.VISIBLE);
                calText.setVisibility(View.GONE);
                calT.setVisibility(View.GONE);
                activeText.setVisibility(View.VISIBLE);
                distText.setVisibility(View.GONE);
                distT.setVisibility(View.GONE);

                stepText.setText("Daily Average " + (int)(total.steps / 7) + " steps");
                activeText.setText(Long.toString((total.activeTime / 60000) / 7) + " min");

                break;
            case 2:
                activeT.setText("Daily average active time");
                SimpleDateFormat simpleDateformat = new SimpleDateFormat("MMMM");
                topText.setText(simpleDateformat.format(currMonth));

                dateText.setText(simpleDateformat.format(currMonth));

                stepText.setVisibility(View.VISIBLE);
                calText.setVisibility(View.GONE);
                calT.setVisibility(View.GONE);
                activeText.setVisibility(View.VISIBLE);
                distText.setVisibility(View.GONE);
                distT.setVisibility(View.GONE);

                stepText.setText("Daily Average " + (int)(total.steps / 30) + " steps");
                activeText.setText(Long.toString((total.activeTime / 60000) / 30) + " min");

                break;
            case 3:
                activeT.setText("Daily average active time");
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currYear);
                topText.setText(Integer.toString(cal.get(Calendar.YEAR)));
                dateText.setText(Integer.toString(cal.get(Calendar.YEAR)));

                stepText.setVisibility(View.VISIBLE);
                calText.setVisibility(View.GONE);
                calT.setVisibility(View.GONE);
                activeText.setVisibility(View.VISIBLE);
                distText.setVisibility(View.GONE);
                distT.setVisibility(View.GONE);

                stepText.setText("Daily Average " + (int)(total.steps / 365) + " steps");
                activeText.setText(Long.toString((total.activeTime / 60000) / 365) + " min");

        }


    };

    public void showCal(View v) {
        if(calView.getVisibility() != CalendarView.VISIBLE)
        {
            calView.setVisibility(CalendarView.VISIBLE);
            rightBtn.setVisibility(View.INVISIBLE);
            leftBtn.setVisibility(View.INVISIBLE);
        }
        else
        {
            calView.setVisibility(CalendarView.GONE);
            rightBtn.setVisibility(View.VISIBLE);
            leftBtn.setVisibility(View.VISIBLE);
        }
    }

}
