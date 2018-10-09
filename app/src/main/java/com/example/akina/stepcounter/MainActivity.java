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

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

public class MainActivity extends AppCompatActivity {

    public static final int STARTYEAR = 2016;
    public static final String[] months = new String[]{
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    Button leftBtn, rightBtn;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;

    BarData data;
    BarChart chart;
    BarDataSet dataSet;

    List<BarEntry> entries = new ArrayList<>();
    HourEntry total = new HourEntry();
    List<HourEntry> totalHrs = new ArrayList<>();

    long today;
    long currHour;
    static long currWeek;
    static long currMonth;
    static long currYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDays();

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                readData(pos);
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

    void drawGraph(final int pos) {
        // Get data here
        //moveView = 0;
        int max = 0;
        switch(pos)
        {
            case 0:
                max = 24;
                break;
            case 1:
                max = 7;
                break;
            case 2:
                max = 31;
                break;
            case 3:
                max = 12;
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
                Log.e("SELECTED ", Integer.toString(totalHrs.size()));


                int i = (int)e.getX();
                TextView stepText = findViewById(R.id.data_val);
                stepText.setText(Long.toString(totalHrs.get(i).steps) + " steps");
                TextView calText = findViewById(R.id.cal_val);
                calText.setText(Float.toString(totalHrs.get(i).calories) + " kcal");
                TextView activeText = findViewById(R.id.active_val);
                activeText.setText(Long.toString((totalHrs.get(i).activeTime / 60000)) + " min");
                TextView distText = findViewById(R.id.dist_val);
                distText.setText(Float.toString(totalHrs.get(i).distance / 1000) + " km");
            }

            @Override
            public void onNothingSelected() {
                TextView stepText = findViewById(R.id.data_val);
                stepText.setText(Long.toString(total.steps) + " steps");
                TextView calText = findViewById(R.id.cal_val);
                calText.setText(Float.toString(total.calories) + " kcal");
                TextView activeText = findViewById(R.id.active_val);
                activeText.setText(Long.toString((total.activeTime / 60000)) + " min");
                TextView distText = findViewById(R.id.dist_val);
                distText.setText(Float.toString(total.distance / 1000) + " km");
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
                            boolean lastDayOfMonth = false;


                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();

                                long s = 0;
                                float d = 0;
                                float c = 0;
                                List<DataPoint> a = new ArrayList<>();

                                HourEntry dayTotal = new HourEntry();

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
                                        totalHrs.add(dayTotal);
                                        dayTotal.clear();
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
        DateFormat dateFormat = getDateInstance();
        TextView topText = findViewById(R.id.currTimeText);
        TextView dateText = findViewById(R.id.date_indicator);

        switch(pos)
        {
            case 0:
                topText.setText(dateFormat.format(currHour));
                if(today == currHour)
                    topText.setText("Today");

                dateText.setText(dateFormat.format(currHour));
                break;
            case 1:
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(currWeek);
                cal.add(Calendar.DAY_OF_WEEK, 7);
                topText.setText(dateFormat.format(currWeek) + " - "+ dateFormat.format(cal.getTimeInMillis()));

                dateText.setText(dateFormat.format(currWeek) + " - "+ dateFormat.format(cal.getTimeInMillis()));
                break;
            case 2:
                SimpleDateFormat simpleDateformat = new SimpleDateFormat("MMMM");
                topText.setText(simpleDateformat.format(currMonth));

                dateText.setText(simpleDateformat.format(currMonth));
                break;
            case 3:
                cal = Calendar.getInstance();
                cal.setTimeInMillis(currYear);
                topText.setText(Integer.toString(cal.get(Calendar.YEAR)));
                dateText.setText(Integer.toString(cal.get(Calendar.YEAR)));
        }

        TextView stepText = findViewById(R.id.data_val);
        stepText.setText(Long.toString(total.steps) + " steps");
        TextView calText = findViewById(R.id.cal_val);
        calText.setText(Float.toString(total.calories) + " kcal");
        TextView activeText = findViewById(R.id.active_val);
        activeText.setText(Long.toString((total.activeTime / 60000)) + " min");
        TextView distText = findViewById(R.id.dist_val);
        distText.setText(Float.toString(total.distance / 1000) + " km");

    };

}

/*public class MainActivity extends AppCompatActivity {

    public static final int STARTYEAR = 2016;
    public static final String[] months = new String[] {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    Button leftBtn;
    Button rightBtn;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;

    BarChart chart;
    BarDataSet dataSet;
    BarData data;

    List<BarEntry> hours = new ArrayList<>();
    List<BarEntry> days = new ArrayList<>();
    List<BarEntry> years = new ArrayList<>();

    Map<Long, HourEntry> allHours = new HashMap<>();
    //List<HourEntry> allHours = new ArrayList<>();

    long firstHour = Integer.MAX_VALUE;
    long firstDay = Integer.MAX_VALUE;
    long firstMonth = Integer.MAX_VALUE;

    boolean setFirst;

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

    public void subscribe(){
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
*//*                                    setFirst = false;

                                    long totalTime = determineTotalMonth((currDay() - firstDay())/86400000);
                                    Calendar cal = Calendar.getInstance();
                                    long startTime = firstDay();
                                    cal.setTimeInMillis(startTime);
                                    cal.add(Calendar.MONTH, 1);
                                    cal.add(Calendar.DAY_OF_YEAR, -1);
                                    cal.set(Calendar.HOUR_OF_DAY, 23);
                                    cal.set(Calendar.SECOND, 59);
                                    cal.set(Calendar.MINUTE, 59);
                                    long endTime = cal.getTimeInMillis();

                                    for(int i = 0; i < totalTime; ++i)
                                    {
                                        readData(startTime, endTime, i);
                                        cal.add(Calendar.DAY_OF_YEAR, 1);
                                        cal.set(Calendar.HOUR_OF_DAY, 0);
                                        cal.set(Calendar.SECOND, 0);
                                        cal.set(Calendar.MINUTE, 0);
                                        startTime = cal.getTimeInMillis();
                                        cal.add(Calendar.MONTH, 1);
                                        cal.add(Calendar.DAY_OF_YEAR, -1);
                                        cal.set(Calendar.HOUR_OF_DAY, 23);
                                        cal.set(Calendar.SECOND, 59);
                                        cal.set(Calendar.MINUTE, 59);
                                        endTime = cal.getTimeInMillis();
                                    }
                                    readData(startTime, endTime, totalTime);*//*

                                    // Set for loop to iterate through all months starting Jan 2016
                                    *//*
                                        firstDay -> milliseconds
                                        currDay -> milliseconds

                                      Day - Begins at current hour, offset hour of day      (0:00)      - 0:00 Jan 1 2016   (0) 12:00 Sep 12 2018   (currHour)  0:00 Sep 12 2018    (currHour - currHour%24)
                                      Week - Begins at current day, offset day of week      (Monday)    - Friday            (0) Wed                 (currDay)   Monday              (currDay - currDay%7)
                                      Month - Begins at current day, offset day of month    (1st)       - 1                 (0) 12                  (currDay)   1                   (currDay - DayOfMonth)
                                      Year - Begins at current month, offset month of year  (Jan)       - Jan 2016          (0) Sep 2018            (currMonth) Jan                 (currMonth - monthOfYear)
                                     *//*
                                    Log.i("Success", "Successfully subscribed!");
                                } else {
                                    Log.w("Failed", "There was a problem subscribing.", task.getException());
                                }
                            }
                        });
    }

    void refreshGraph(int pos){
        long today = ((currDay() - firstDay())/86400000) + 1;

        switch (pos) {
            case 0:
                drawGraph(pos, hours, 24, firstHour - (firstHour%24), today * 24,  (today - 1) * 24);
                break;
            case 1:
                long latestWeek = (today - today%7) + 7;
                drawGraph(pos, days, 7,  firstDay - (firstDay%7) - 4, latestWeek +  3, today - (today%7));
                break;
            case 2:
                long latestMYear = determineYear(today);
                long latestMonth = determineMonth(today);
                long lastDayOfLatestMonth = getDaysForMonth(latestMonth, latestMYear);
                long latestDayOfMonth = determineDayOfMonth(today, latestMonth + 12 * (latestMYear - STARTYEAR));

                drawGraph(pos, days, 31, firstDay - (firstDay%7), today - latestDayOfMonth + lastDayOfLatestMonth, today - latestDayOfMonth + lastDayOfLatestMonth + 15);
                break;
            case 3:
                long yearOfLatestDay = determineYear(today);
                long monthOfLatestDay = determineMonth(today);
                long monthOfLatestYear = (yearOfLatestDay - STARTYEAR) * 12 + monthOfLatestDay;
                long latestYear = monthOfLatestYear - (monthOfLatestYear % 12);

                drawGraph(pos, years, 12, firstMonth - (firstMonth%12), latestYear + 12, latestYear);
                break;
        }
        updateText(pos);
    }
    void drawGraph(final int pos, List<BarEntry> datatype, final long range, final long startValue, final long endValue, long moveView){
        // Get data here
        //moveView = 0;
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

        //chart.setAutoScaleMinMaxEnabled(true); //Scales the Y axis as it moves along
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

                if(chart.getLowestVisibleX() < startValue)
                    chart.moveViewToX(startValue);
                if(chart.getHighestVisibleX() > endValue)
                    chart.moveViewToX(endValue);

                updateText(pos);
                //Log.e("Lowest Visible X", Float.toString(chart.getLowestVisibleX()));
                //Log.e("Highest Visible X", Float.toString(chart.getHighestVisibleX()));
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
        chart.notifyDataSetChanged();
        chart.invalidate();
        chart.setVisibleXRange(range, range); // Week 7, Month 30, Year 12,

        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);

        //Log.e("Lowest Visible X", Float.toString( chart.getLowestVisibleX()));
        //Log.e("START", Long.toString(startValue));
        if( chart.getLowestVisibleX() <= startValue) {
            leftBtn.setBackgroundResource(R.drawable.left_arrow_disable);
            leftBtn.setEnabled(false);
        }
        else
        {
            leftBtn.setBackgroundResource(R.drawable.left_arrow);
            leftBtn.setEnabled(true);
        }

        //Log.e("Highest Visible X", Float.toString(chart.getHighestVisibleX()));
        //Log.e("End", Long.toString(endValue));
        if(chart.getHighestVisibleX() < endValue) {
            rightBtn.setBackgroundResource(R.drawable.right_arrow);
            rightBtn.setEnabled(true);
        }
        else
        {
            rightBtn.setBackgroundResource(R.drawable.right_arrow_disable);
            rightBtn.setEnabled(false);
        }

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

        //Log.e("PRINTHOURSSSS FINAL", Integer.toString(datatype.size()));
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
                                        long startHour = (startTime / 3600000) % 24;
                                        long startHourValue = hourIterator;
                                        long startDay = totalDays + 1;
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

    private void readData(final long startTime, final long endTime, final long monthIt){
        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder().setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED).setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms").build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                //.enableServerQueries()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.HOURS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS).build();

        // Get data
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this)).readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (dataReadResponse.getBuckets().size() > 0) {
                            long dayIt = getDayIndex(startTime);
                            long hourIt = getHourIndex(startTime);
                            long dayAcc = 0;
                            long yearAcc = 0;


                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                long s = 0;
                                float d = 0;
                                float c = 0;
                                List<Integer> a = new ArrayList<>();

                                for (int dataSetNo = 0; dataSetNo < bucket.getDataSets().size(); ++dataSetNo)
                                {
                                    if (bucket.getDataSets().size() > 0
                                            && bucket.getDataSets().get(dataSetNo).getDataPoints().size() > 0
                                            && bucket.getDataSets().get(dataSetNo).getDataPoints().get(0).getDataType().getFields().size() > 0) {

                                        DataPoint dp = bucket.getDataSets().get(dataSetNo).getDataPoints().get(0);
                                        printRange(dp.getStartTime(TimeUnit.MILLISECONDS), dp.getEndTime(TimeUnit.MILLISECONDS));
                                        Field field = dp.getDataType().getFields().get(0);

                                        switch (dataSetNo) {
                                            case 0:

                                                if(hourIt < firstHour)
                                                {
                                                    firstHour = hourIt;
                                                }
                                                if(dayIt < firstDay)
                                                {
                                                    firstDay = dayIt;
                                                }
                                                if(monthIt < firstMonth)
                                                {
                                                    firstMonth = monthIt;
                                                }

                                                int value = dp.getValue(field).asInt();
                                                //hours.add(new BarEntry(getHourIndex(dp.getStartTime(TimeUnit.MILLISECONDS)), value));

                                                dayAcc += value;
                                                yearAcc += value;
                                                s = value;
                                                break;
                                            case 1:
                                                //Log.e("FORMAT ", Integer.toString(dp.getValue(field).getFormat()));
                                                d = dp.getValue(field).asFloat();
                                                break;
                                            case 2:
                                                //Log.e("FORMAT ", Integer.toString(dp.getValue(field).getFormat()));
                                                c = dp.getValue(field).asFloat();
                                                break;
                                            case 3:
                                                for(DataPoint dataPoint : bucket.getDataSets().get(dataSetNo).getDataPoints())
                                                {
                                                    //Log.e("FORMAT ", Integer.toString(dataPoint.getValue(field).getFormat()));
                                                    a.add(dataPoint.getValue(field).asInt());
                                                }
                                                Log.e("ALLSTUFF", Long.toString(s));
                                                Log.e("ALLSTUFF", Float.toString(d));
                                                Log.e("ALLSTUFF", Float.toString(c));

                                                allHours.put(getHourIndex(dp.getStartTime(TimeUnit.MILLISECONDS)), new HourEntry(s, d, c, a));
                                        }

                                        Log.e("Value    ", dataSetNo + ": " + dp.getValue(field));
                                    }
                                    if ((hourIt) % 24 == 0 && dataSetNo == 0) {
                                        if(dayAcc != 0)
                                        {
                                            //days.add(new BarEntry(dayIt, dayAcc));
                                            dayAcc = 0;
                                        }
                                        ++dayIt;
                                    }
                                }
                                //allHours.add(new HourEntry(s, d, c, a));
                                ++hourIt;
                            }
                            if (dayAcc != 0) {
                                //days.add(new BarEntry(dayIt, dayAcc));
                                ++dayIt;
                            }
                            if (yearAcc != 0)
                            {
                                //years.add(new BarEntry(monthIt, yearAcc));
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
                int hours = (int)((value) % 24);
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
    public static long determineTotalMonth(long dayOfYear) {

        int month = -1;
        int monthGet =  -1;
        int days = 0;

        while (days < dayOfYear) {
            month = month + 1;
            monthGet = monthGet + 1;

            if (monthGet >= 12)
                monthGet = 0;

            long year = determineYear(days);

            days += getDaysForMonth(monthGet, year);
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

    long getHourIndex(long timeInMillis) {
        long hour = (timeInMillis - firstDay())/3600000;
        //Log.e("Hourtime", Long.toString(hour));
        return hour;
    }
    long getDayIndex(long timeInMillis) {
        long day = getHourIndex(timeInMillis)/24;
        //Log.e("Dayyyyy", Long.toString(day));
        return day;
    }
    long getMonthIndex(long timeInMillis) {
        long month = determineTotalMonth( (timeInMillis - firstDay())/86400000);
        //Log.e("Monthssss", Long.toString(month));
        return month;
    }


    void updateText(int pos) {
        Log.e("Allsize",Integer.toString(allHours.size()));
        Log.e("stuffsinside", allHours.toString());

*//*        if(allHours.isEmpty())
            Log.e("NOOOOOOOO", "EMPTY");
        else
            Log.e("NOOOO", "Not EMPTY");*//*

        TextView topText = findViewById(R.id.currTimeText);
        TextView dateText = findViewById(R.id.date_indicator);
        TextView stepText = findViewById(R.id.data_val);
        TextView calText = findViewById(R.id.cal_val);
        TextView activeText = findViewById(R.id.active_val);
        TextView distText = findViewById(R.id.dist_val);

        switch(pos)
        {
            case 0:
                float startVal = chart.getLowestVisibleX();
                topText.setText(getDateStr(pos, startVal));
                dateText.setText(getDateStr(pos, startVal));
                Log.e("NOOOOOOOO", Integer.toString((int)startVal));
                Log.e("NOOOOOOOO", allHours.toString());

                allHours.get((int)startVal);
                Log.e("NOO", Long.toString(((HourEntry)allHours.get(23650)).steps));
                Log.e("NOO", Float.toString(((HourEntry)allHours.get(23650)).calories));
                Log.e("NOO", Float.toString(((HourEntry)allHours.get(23650)).distance));
                if(allHours.containsKey(23650))
                {
                    stepText.setText(Long.toString(((HourEntry)allHours.get(23650)).steps));
                    calText.setText(Float.toString(((HourEntry)allHours.get(23650)).calories));
                    distText.setText(Float.toString(((HourEntry)allHours.get(23650)).distance));
                }
                //stepText.setText(Long.toString(allHours.get(startVal).steps));
                //calText.setText(Float.toString(allHours.get(startVal).calories));
                //distText.setText(Float.toString(allHours.get(startVal).distance));
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
        }
    };

}*/
