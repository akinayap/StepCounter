package com.example.akina.stepcounter;

import android.content.Context;
import android.graphics.Canvas;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.NumberFormat;
import java.util.Locale;

public class CustomMarkerView extends MarkerView {

    private TextView dateTV;
    private TextView distTV;
    private ImageView pointer;

    private int pos;

    public CustomMarkerView (Context context, int layoutResource, int pos) {
        super(context, layoutResource);
        this.pos = pos;
        // this markerview only displays a textview
        dateTV = findViewById(R.id.date);
        distTV = findViewById(R.id.dist);
        pointer = findViewById(R.id.pointer);
    }

    @Override
    public void draw(Canvas canvas, float posX, float posY) {

        MPPointF offset = getOffsetForDrawingAtPoint(posX, posY);
        float threshold = canvas.getWidth()/3f;

        if(posX < threshold)
        {
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            float dp = -85f;
            float fpixels = metrics.density * dp;

            pointer.setTranslationX(fpixels);
        }
        else if (posX < threshold * 2)
        {
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            float dp = 0;
            float fpixels = metrics.density * dp;

            pointer.setTranslationX(fpixels);
            offset.x = -getWidth()/2;
        }
        else
        {
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            float dp = 85f;
            float fpixels = metrics.density * dp;

            pointer.setTranslationX(fpixels);
            offset.x = -getWidth();
        }



        Log.e("width", Integer.toString(canvas.getWidth()));


        int saveId = canvas.save();
        // translate to the correct position and draw
        canvas.translate(posX + offset.x, offset.y);//posY + offset.y);
        draw(canvas);
        canvas.restoreToCount(saveId);
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        dateTV.setText(MainActivity.getDateStrFromGraphValue(pos, e.getX()));
        distTV.setText("" + NumberFormat.getNumberInstance(Locale.US).format((int)e.getY()) + " Steps"); // set the entry-value as the display text
    }
}
