package com.yursky.stratoman;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static DataSystem dataSystem = new DataSystem(Environment.getExternalStorageDirectory() + "/StratoMan/");
    static Bluetooth bluetooth = new Bluetooth();

    final static String DATA1 = "[0,1,2,3,4,5]";
    final static String DATA2 = "{0,11,22,33,44,2}";
    final static String DATA3 = "N:661;T:11m5s;MP.Stage:0;MP.Alt:20;MP.VSpeed:3.2;MP.AvgVSpeed:3.3;Baro.Press:1010.81;Baro.Alt:20;Baro.Temp:35.69;GPS.Coord:N55d45m49s,E37d31m52s;GPS.Home:N55d45m50s,E37d31m52s;GPS.HDOP:3.83;GPS.Alt:150;GPS.Dst:16;GPS.HSpeed:2;GPS.Course:33;GPS.Time:10h51m09s;GPS.Date:10.09.2019;DS.Temp:[d6]=28.38,[02]=34.13,[fb]=10.00,[7b]=13.00,[e6]=33.50,[44]=29.44,[7e]=15.5;Volt:5.22,12.20,9.97,7.30,0.00,4.69,7.24,8.34;Relays:0,0,1,0,0,0;";

    ImageView imgStrato;
    TextView textInside;
    TextView textOutside;

    TextView altitude;
    TextView vSpeed;
    TextView hSpeed;
    TextView suitPress;
    TextView airTankPress;
    TextView suitTemp;
    TextView airFlowTemp;
    TextView externalPress;
    TextView externalTemp;
    TextView lat;
    TextView lon;
    TextView nowTime;
    TextView flightTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN};

        ActivityCompat.requestPermissions(this, permissions, 0);

        EventBus.getDefault().register(this);
        bluetooth.registerReceiver(this);

        imgStrato = findViewById(R.id.imgStrato);
        textInside = findViewById(R.id.textInside);
        textOutside = findViewById(R.id.textOutside);

        altitude = findViewById(R.id.altitude);
        vSpeed = findViewById(R.id.vSpeed);
        hSpeed = findViewById(R.id.hSpeed);
        suitPress = findViewById(R.id.suitPress);
        airTankPress = findViewById(R.id.airTankPress);
        suitTemp = findViewById(R.id.suitTemp);
        airFlowTemp = findViewById(R.id.airFlowTemp);
        externalPress = findViewById(R.id.externalPress);
        externalTemp = findViewById(R.id.externalTemp);
        lat = findViewById(R.id.lat);
        lon = findViewById(R.id.lon);
        nowTime = findViewById(R.id.nowTime);
        flightTime = findViewById(R.id.flightTime);

        imgStrato.setOnClickListener(this);
        textInside.setOnClickListener(this);
        textOutside.setOnClickListener(this);

        suitTemp.setOnClickListener(this);
        airFlowTemp.setOnClickListener(this);

        //Test
        try {
            showPointInfo(dataSystem.parseData(DATA3));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.imgStrato:
                intent = new Intent(this, BluetoothActivity.class);
                startActivity(intent);
                break;

            case R.id.textInside:
                intent = new Intent(this, InfoActivity.class);
                startActivity(intent);
                break;

            case R.id.textOutside:
                intent = new Intent(this, TerminalActivity.class);
                startActivity(intent);
                break;

            case R.id.suitTemp:
            case R.id.airFlowTemp:
                //Hide top panel
                onStart();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleBluetoothEvent(Bluetooth.BluetoothEvent event) {
        switch (event.getAction()) {
            case Bluetooth.DISABLED:
                Toast.makeText(this, getString(R.string.blOff), Toast.LENGTH_SHORT).show();
                break;

            case Bluetooth.ENABLED:
                Toast.makeText(this, getString(R.string.blOn), Toast.LENGTH_SHORT).show();
                break;

            case Bluetooth.DISCONNECTED:
                Toast.makeText(this, getString(R.string.disconnected), Toast.LENGTH_SHORT).show();
                break;

            case Bluetooth.CONNECTING:
                Toast.makeText(this, getString(R.string.connectingTo) + event.getData() + "...", Toast.LENGTH_SHORT).show();
                break;

            case Bluetooth.CONNECTED:
                Toast.makeText(this, getString(R.string.connectedTo) + event.getData(), Toast.LENGTH_SHORT).show();
                break;

            case Bluetooth.MESSAGE_RECEIVED:
                String message = (String) event.getData();
                dataSystem.writeData(this, message);

                DataSystem.DataPoint dataPoint = null;
                try {
                    dataPoint = dataSystem.parseData(message);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                if (dataPoint.deviceCategory != DataSystem.DeviceCategory.SUIT_NTYPE) return;

                dataSystem.addDataPoint(dataPoint);
                showPointInfo(dataPoint);

                break;
        }
    }

    void showPointInfo(DataSystem.DataPoint dataPoint) {
        DecimalFormat dfVal = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
        DecimalFormat dfCoord = new DecimalFormat("0.00000", DecimalFormatSymbols.getInstance(Locale.US));
        DecimalFormat dfTime = new DecimalFormat("00", DecimalFormatSymbols.getInstance(Locale.US));

        altitude.setText(((DataSystem.SuitNtypePoint) dataPoint).baroAlt + " м");
        vSpeed.setText(dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).mpAvgVSpeed) + " м/с");
        hSpeed.setText(((DataSystem.SuitNtypePoint) dataPoint).gpsHSpeed + " м/с");

        suitPress.setText(dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).baroPress) + " бар");
        setStatusColor(suitPress, 1f, 0.3f, 0, "low");
        airTankPress.setText(dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).airTankPress) + " бар");
        setStatusColor(airTankPress, 200f, 1f, 0, "low");

        float avgSuitTemp = (((DataSystem.SuitNtypePoint) dataPoint).chestTemp + ((DataSystem.SuitNtypePoint) dataPoint).shoulderTemp) / 2f;
        suitTemp.setText(dfVal.format(avgSuitTemp) + " °С");
        setStatusColor(suitTemp, 25f, 10f, 35f, "between");
        airFlowTemp.setText(dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).airFlowTemp) + " °С");
        setStatusColor(airFlowTemp, 25f, 10f, 35f, "between");

        lat.setText(dfCoord.format(((DataSystem.SuitNtypePoint) dataPoint).gpsLat));
        lon.setText(dfCoord.format(((DataSystem.SuitNtypePoint) dataPoint).gpsLon));

        externalPress.setText(dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).baroPress) + " бар");
        externalTemp.setText(dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).externalTemp) + " °С");

        nowTime.setText(new SimpleDateFormat("HH:mm:ss", Locale.US).format(dataPoint.date.getTime()));

        int secFromStart = ((DataSystem.SuitNtypePoint) dataPoint).secFromStart;
        int seconds = secFromStart % 60;
        int minutes = (secFromStart / 60) % 60;
        int hours = (secFromStart / 60) / 60;
        flightTime.setText(dfTime.format(hours) + ":" + dfTime.format(minutes) + ":" + dfTime.format(seconds));
    }

    void setStatusColor(TextView textView, float nominalValue, float minValue, float maxValue, String warnType) {
        int color = getResources().getColor(R.color.green);
        float currentValue = Float.valueOf(textView.getText().toString().split(" ")[0]);

        float yellowLevelLow;
        float redLevelLow;
        float yellowLevelHigh;
        float redLevelHigh;

        switch (warnType) {
            case "low":
                yellowLevelLow = minValue + (nominalValue - minValue) / 2f; //midLevel, 50% left
                redLevelLow = minValue + (yellowLevelLow - minValue) / 3f;  //15% left

                if (currentValue < redLevelLow)
                    color = getResources().getColor(R.color.red);
                else if (currentValue < yellowLevelLow)
                    color = getResources().getColor(R.color.yellow);

                break;

            case "between":
                yellowLevelLow = minValue + (nominalValue - minValue) / 2f; //midLevel, 50% left
                redLevelLow = minValue + (yellowLevelLow - minValue) / 3f;  //15% left

                yellowLevelHigh = maxValue - (maxValue - nominalValue) / 2f; //midLevel, 50% left
                redLevelHigh = maxValue - (maxValue - yellowLevelHigh) / 3f;  //15% left

                if (currentValue < redLevelLow || currentValue > redLevelHigh)
                    color = getResources().getColor(R.color.red);
                else if (currentValue < yellowLevelLow || currentValue > yellowLevelHigh)
                    color = getResources().getColor(R.color.yellow);

                break;

            case "high":
                yellowLevelHigh = maxValue - (maxValue - nominalValue) / 2f; //midLevel, 50% left
                redLevelHigh = maxValue - (maxValue - yellowLevelHigh) / 3f;  //15% left

                if (currentValue > redLevelHigh)
                    color = getResources().getColor(R.color.red);
                else if (currentValue > yellowLevelHigh)
                    color = getResources().getColor(R.color.yellow);
                break;
        }

        textView.setTextColor(color);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetooth.unregisterReceiver(this);
        EventBus.getDefault().unregister(this);
    }
}
