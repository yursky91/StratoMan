package com.yursky.stratoman;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.widget.ScrollView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class TerminalActivity extends AppCompatActivity {

    ScrollView scrollVertical;
    TextView textTerminal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        EventBus.getDefault().register(this);

        scrollVertical = findViewById(R.id.scrollVertical);
        textTerminal = findViewById(R.id.textTerminal);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleBluetoothEvent(Bluetooth.BluetoothEvent event) {
        switch (event.getAction()) {
            case Bluetooth.MESSAGE_RECEIVED:
                textTerminal.setText(textTerminal.getText() + "\n" + event.getData() + "\n");

                //Handler is needed for scrollView scrolling
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollVertical.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                }, 250); //250 ms delay
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
