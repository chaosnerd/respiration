package com.example.tarathontanwinit.pulmy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;


public class MainActivity extends AppCompatActivity implements SensorEventListener{
    TextView textRssi;

    private float calX, calY, calZ;
    private float lastX, lastY, lastZ;
    private int n=1;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private float vibrateThreshold = 0;

    private TextView currentX, currentY, currentZ;

    public Vibrator v;

    private Thread th = new Thread(new Runnable() {

        @Override
        public void run() {
            // we add 100 new entries
            for (int i = 0; i < 60; i++) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addEntry();
                    }
                });

                // sleep to slow down the add of entries
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    // manage error ...
                }
            }
        }
    });

    private static final Random RANDOM = new Random();
    private LineGraphSeries<DataPoint> series;
    private int lastG = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textRssi = findViewById(R.id.Rssi);

        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        // data
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);
        // customize a little bit viewport
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0);
        viewport.setMaxY(10);
        viewport.setScrollable(true);

        //th.start();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fai! we dont have an accelerometer!
        }

        //initialize vibration
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        DisplayWifiState();

        this.registerReceiver(this.myWifiReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        this.registerReceiver(this.myRssiChangeReceiver,
                new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));

    }
    private BroadcastReceiver myRssiChangeReceiver
            = new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            int newRssi = arg1.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
            textRssi.setText(String.valueOf(newRssi));
        }};

    private BroadcastReceiver myWifiReceiver
            = new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            NetworkInfo networkInfo = (NetworkInfo) arg1.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                DisplayWifiState();
            }
        }};

    private void DisplayWifiState(){

        ConnectivityManager myConnManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager myWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();



        if (myNetworkInfo.isConnected()){

            textRssi.setText(String.valueOf(myWifiInfo.getRssi()));
        }
        else{

            textRssi.setText("WIFI OFF");
        }

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // clean current values
        displayCleanValues();
        // display the current x,y,z accelerometer values
        displayCurrentValues();

        // get the change of the x,y,z values of the accelerometer
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);
        deltaZ = Math.abs(lastZ - event.values[2]);

//
//        // if the change is below 2, it is just plain noise
//        if (deltaX < 2)
//            deltaX = 0;
//        if (deltaY < 2)
//            deltaY = 0;
//
//        if ((deltaX > vibrateThreshold) || (deltaY > vibrateThreshold) || (deltaZ > vibrateThreshold)) {
//            v.vibrate(50);
//        }
    }

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // we're going to simulate real time with thread that append data to the graph


    }

    // add random data to graph
    private void addEntry() {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        if (calY==0){
            if (((deltaZ-calZ)*6d)+4d>8.5||((deltaZ-calZ)*6d)+4d<1){
                calib();
            }
            series.appendData(new DataPoint(lastG++, ((deltaZ-calZ)*6d)+4d), false, 10);
        }
        else if (calZ==0){
            if (((deltaY-calY)*6d)+4d>8.5||((deltaY-calY)*6d)+4d<1){
                calib();
            }
            series.appendData(new DataPoint(lastG++, ((deltaY-calY)*6d)+4d), false, 10);
        }
    }

    private void calib() {
        n=1;
        if (deltaY>deltaZ&&deltaY>deltaX){
            calZ=deltaZ;
            calY=0;
        }
        else if (deltaZ>deltaY&&deltaZ>deltaX){
            calY=deltaY;
            calZ=0;
        }
    }

    public void calibrate(View view){
        calib();

        th.start();
    }

}
