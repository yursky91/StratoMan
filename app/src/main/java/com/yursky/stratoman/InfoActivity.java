package com.yursky.stratoman;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class InfoActivity extends AppCompatActivity {

    TextView textInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        EventBus.getDefault().register(this);

        textInfo = findViewById(R.id.textInfo);

        if (MainActivity.dataSystem.getDeviceCategory(DataSystem.DeviceCategory.SUIT_NTYPE) == null) {
            try {
                showPointInfo(MainActivity.dataSystem.parseData(MainActivity.DATA3));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            showPointInfo(MainActivity.dataSystem.getDevice(DataSystem.DeviceCategory.SUIT_NTYPE, 0).getLastDataPoint());
    }

    void showPointInfo(DataSystem.DataPoint dataPoint) {
        StringBuilder stringBuilder = new StringBuilder();
        DecimalFormat dfVal = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
        DecimalFormat dfCoord = new DecimalFormat("0.00000", DecimalFormatSymbols.getInstance(Locale.US));
        DecimalFormat dfTime = new DecimalFormat("00", DecimalFormatSymbols.getInstance(Locale.US));

        stringBuilder.append("Дата: " + new SimpleDateFormat("dd.MM.yyyy", Locale.US).format(dataPoint.date.getTime()) + "\n");
        stringBuilder.append("Время: " + new SimpleDateFormat("HH:mm:ss", Locale.US).format(dataPoint.date.getTime()) + "\n");
        int secFromStart = ((DataSystem.SuitNtypePoint) dataPoint).secFromStart;
        int seconds = secFromStart % 60;
        int minutes = (secFromStart / 60) % 60;
        int hours = (secFromStart / 60) / 60;
        stringBuilder.append("Время работы: " + dfTime.format(hours) + ":" + dfTime.format(minutes) + ":" + dfTime.format(seconds) + "\n");
        stringBuilder.append("Стадия: " + ((DataSystem.SuitNtypePoint) dataPoint).mpStage + "\n");
        stringBuilder.append("\n");
        stringBuilder.append("Широта: " + dfCoord.format(((DataSystem.SuitNtypePoint) dataPoint).gpsLat) + "\n");
        stringBuilder.append("Долгота: " + dfCoord.format(((DataSystem.SuitNtypePoint) dataPoint).gpsLon) + "\n");
        stringBuilder.append("Высота-криус: " + ((DataSystem.SuitNtypePoint) dataPoint).mpAlt + " м\n");
        stringBuilder.append("Высота-барометр: " + ((DataSystem.SuitNtypePoint) dataPoint).baroAlt + " м\n");
        stringBuilder.append("Высота-GPS: " + ((DataSystem.SuitNtypePoint) dataPoint).gpsAlt + " м\n");
        stringBuilder.append("Скорость вертикальная: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).mpVSpeed) + " м/с\n");
        stringBuilder.append("Скорость вертикальная средняя: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).mpAvgVSpeed) + " м/с\n");
        stringBuilder.append("Скорость горизонтальная: " + ((DataSystem.SuitNtypePoint) dataPoint).gpsHSpeed + " м/с\n");
        stringBuilder.append("Курс: " + ((DataSystem.SuitNtypePoint) dataPoint).gpsCourse + " °\n");
        stringBuilder.append("HDOP: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).gpsHdop) + "\n");
        stringBuilder.append("\n");
        stringBuilder.append("Широта базы: " + dfCoord.format(((DataSystem.SuitNtypePoint) dataPoint).homeLat) + "\n");
        stringBuilder.append("Долгота базы: " + dfCoord.format(((DataSystem.SuitNtypePoint) dataPoint).homeLon) + "\n");
        stringBuilder.append("Расстояние до базы: " + ((DataSystem.SuitNtypePoint) dataPoint).gpsDist + " м\n");
        stringBuilder.append("\n");
        stringBuilder.append("Температура-криус: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).criusTemp) + " °С\n");
        stringBuilder.append("Температура-барометр: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).baroTemp) + " °С\n");
        stringBuilder.append("Температура снаружи: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).externalTemp) + " °С\n");
        stringBuilder.append("Температура баллона: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).airTankTemp) + " °С\n");
        stringBuilder.append("Температура вентиляции: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).airFlowTemp) + " °С\n");
        stringBuilder.append("Температура на груди: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).chestTemp) + " °С\n");
        stringBuilder.append("Температура под мышкой: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).underarmTemp) + " °С\n");
        stringBuilder.append("Температура на плече: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).shoulderTemp) + " °С\n");
        stringBuilder.append("\n");
        stringBuilder.append("Давление-барометр: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).baroPress) + " бар\n");
        stringBuilder.append("Давление в баллоне: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).airTankPress) + " бар\n");
        stringBuilder.append("Давление в скафандре: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).suitPress) + " бар\n");
        stringBuilder.append("Парциальное давление O2: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).suitPressO2) + " бар\n");
        stringBuilder.append("Парциальное давление CO2: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).suitPressCO2) + " бар\n");
        stringBuilder.append("\n");
        stringBuilder.append("Напряжение 5В: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).voltage5) + " В\n");
        stringBuilder.append("Напряжение 12В: " + dfVal.format(((DataSystem.SuitNtypePoint) dataPoint).voltage12) + " В\n");
        stringBuilder.append("Состояние переключателей: " + ((DataSystem.SuitNtypePoint) dataPoint).relayStatus);

        textInfo.setText(stringBuilder);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleBluetoothEvent(Bluetooth.BluetoothEvent event) {
        switch (event.getAction()) {
            case Bluetooth.MESSAGE_RECEIVED:
                DataSystem.DataPoint dataPoint = MainActivity.dataSystem.getDevice(DataSystem.DeviceCategory.SUIT_NTYPE, 0).getLastDataPoint();
                showPointInfo(dataPoint);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
