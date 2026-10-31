package com.example.glasspedometer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensors;
    private Sensor counter;
    private SharedPreferences prefs;
    private GlassView view;
    private int steps = 0;
    private String status = "Getting your steps…";
    private static final int REQUEST_ACTIVITY = 9;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(225,237,255));
        getWindow().setNavigationBarColor(Color.rgb(219,233,252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        prefs = getSharedPreferences("steps", MODE_PRIVATE);
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        counter = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        view = new GlassView(this);
        setContentView(view);
        if (counter == null) status = "Step sensor not available on this phone";
        else if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_ACTIVITY);
    }
    @Override public void onResume() {
        super.onResume();
        if (counter != null && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
            sensors.registerListener(this, counter, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override public void onPause() { super.onPause(); sensors.unregisterListener(this); }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == REQUEST_ACTIVITY) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                status = "Getting your steps…";
                if (counter != null) sensors.registerListener(this, counter, SensorManager.SENSOR_DELAY_NORMAL);
            } else status = "Allow Physical activity in Settings to count steps";
            view.invalidate();
        }
    }
    private String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    @Override public void onSensorChanged(SensorEvent event) {
        // TYPE_STEP_COUNTER is cumulative since device boot, not since midnight.
        // Establish a daily baseline at first observation of the day.
        int cumulative = (int) event.values[0];
        String day = today();
        String savedDay = prefs.getString("day", "");
        int previous = prefs.getInt("last", -1);
        int baseline = prefs.getInt("baseline", cumulative);
        if (!day.equals(savedDay) || previous < 0 || cumulative < previous) {
            baseline = cumulative;
            prefs.edit().putString("day", day).putInt("baseline", baseline).apply();
        }
        steps = Math.max(0, cumulative - baseline);
        prefs.edit().putInt("last", cumulative).putInt("steps", steps).apply();
        status = "Steps since today's first reading";
        view.invalidate();
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private class GlassView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float density;
        GlassView(Context c) { super(c); density = getResources().getDisplayMetrics().density; setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        float d(float n) { return n * density; }
        void text(String s, float x, float y, float size, int color, boolean bold, boolean center) {
            p.reset(); p.setAntiAlias(true); p.setColor(color); p.setTextSize(d(size));
            p.setTypeface(bold ? Typeface.create("sans-serif", Typeface.BOLD) : Typeface.create("sans-serif", Typeface.NORMAL));
            p.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT);
            getCanvas().drawText(s, x, y, p);
        }
        Canvas canvas;
        Canvas getCanvas() { return canvas; }
        void oval(float x, float y, float r, int color, float blur) {
            p.reset(); p.setAntiAlias(true); p.setColor(color); p.setMaskFilter(new BlurMaskFilter(d(blur), BlurMaskFilter.Blur.NORMAL));
            canvas.drawCircle(x, y, d(r), p); p.setMaskFilter(null);
        }
        @Override protected void onDraw(Canvas c) {
            canvas = c; float w = getWidth(), h = getHeight(), cx = w/2f;
            p.reset(); p.setShader(new LinearGradient(0,0,w,h,0xffedf5ff,0xffd6e7ff,Shader.TileMode.CLAMP)); c.drawRect(0,0,w,h,p); p.setShader(null);
            oval(w*.2f,h*.25f,130,0x66f5a8f2,42); oval(w*.85f,h*.67f,150,0x665dbff7,45); oval(w*.65f,h*.12f,100,0x88ffffff,40);
            text("Pedometer", d(25), d(67), 37, 0xff17213e, true, false);
            text(new SimpleDateFormat("EEEE, MMMM d", Locale.US).format(new Date()), d(27), d(96), 14, 0xff536783, false, false);
            float left=d(20), right=w-d(20), top=h*.27f, bottom=Math.min(h*.74f, top+d(345));
            RectF rect=new RectF(left,top,right,bottom);
            p.reset(); p.setAntiAlias(true); p.setShadowLayer(d(22),0,d(12),0x335a7ba9); p.setColor(0x9effffff); c.drawRoundRect(rect,d(32),d(32),p); p.clearShadowLayer();
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(d(1.3f)); p.setColor(0xddffffff); c.drawRoundRect(rect,d(32),d(32),p); p.setStyle(Paint.Style.FILL);
            float centerY=(top+bottom)/2;
            text("TODAY",cx,centerY-d(67),13,0xff647895,true,true);
            text(NumberFormat.getIntegerInstance().format(steps),cx,centerY+d(11),64,0xff182542,true,true);
            text("steps",cx,centerY+d(44),19,0xff536783,false,true);
            p.setColor(0x33536f9c); c.drawRoundRect(new RectF(left+d(35),bottom-d(64),right-d(35),bottom-d(55)),d(5),d(5),p);
            p.setColor(0xff6d93e9); float maxWidth=(right-left)-d(70);
            c.drawRoundRect(new RectF(left+d(35),bottom-d(64),left+d(35)+maxWidth*Math.min(1,steps/10000f),bottom-d(55)),d(5),d(5),p);
            text("10,000 step goal",cx,bottom-d(27),12,0xff637896,false,true);
            text(status,cx,Math.min(h-d(36),bottom+d(55)),12,0xff536783,false,true);
        }
    }
}
