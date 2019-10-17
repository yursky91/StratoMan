package com.yursky.stratoman;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Set;

@SuppressLint("SetTextI18n")
public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnPaired;
    Button btnDiscover;
    TextView bluetoothStatus;
    TextView devType;

    ListView devicesListView;
    ArrayAdapter<String> btArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        EventBus.getDefault().register(this);

        btnPaired = findViewById(R.id.btnPaired);
        btnDiscover = findViewById(R.id.btnDiscover);
        bluetoothStatus = findViewById(R.id.bluetoothStatus);
        devType = findViewById(R.id.devType);

        btnPaired.setOnClickListener(this);
        btnDiscover.setOnClickListener(this);

        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        devicesListView = findViewById(R.id.devicesListView);
        devicesListView.setAdapter(btArrayAdapter);
        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                String info = ((TextView) v).getText().toString();
                String name = info.substring(0, info.length() - 18);
                String address = info.substring(info.length() - 17);
                MainActivity.bluetooth.connect(getApplicationContext(), address, name, 1);
            }
        });

        setCurrentStatus(MainActivity.bluetooth.getStatus());
        showPairedDevices();
    }

    void setCurrentStatus(int status) {
        switch (status) {
            case Bluetooth.DISABLED:
                btArrayAdapter.clear();
                bluetoothStatus.setText(getString(R.string.blOff));
                break;

            case Bluetooth.ENABLED:
                bluetoothStatus.setText(getString(R.string.disconnected));
                break;

            case Bluetooth.DISCONNECTED:
                bluetoothStatus.setText(getString(R.string.disconnected));
                btnPaired.setText(getString(R.string.showPairedDevices1));
                btnDiscover.setEnabled(true);
                devicesListView.setEnabled(true);
                devicesListView.setAlpha(1);
                break;

            case Bluetooth.CONNECTING:
                setCurrentStatus(Bluetooth.CONNECTED);
                bluetoothStatus.setText(getString(R.string.connectingTo) + MainActivity.bluetooth.name + "...");
                break;

            case Bluetooth.CONNECTED:
                bluetoothStatus.setText(getString(R.string.connectedTo) + MainActivity.bluetooth.name);
                btnPaired.setText(getString(R.string.disconnect));
                btnDiscover.setEnabled(false);
                devicesListView.setEnabled(false);
                devicesListView.setAlpha(0.5f);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPaired:
                if (btnPaired.getText().equals(getText(R.string.disconnect)))
                    MainActivity.bluetooth.disconnect();
                else
                    showPairedDevices();
                break;

            case R.id.btnDiscover:
                showDiscoveredDevices();
                break;
        }
    }

    void showPairedDevices() {
        if (MainActivity.bluetooth.getStatus() == Bluetooth.DISABLED) {
            MainActivity.bluetooth.turnOn(this, Bluetooth.REQUEST_ENABLE_BT_PAIR);
            return;
        }

        MainActivity.bluetooth.stopDiscovery();

        btArrayAdapter.clear(); //clear items
        devType.setText(getText(R.string.showPairedDevices));
        Set<BluetoothDevice> pairedDevices = MainActivity.bluetooth.getPairedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    void showDiscoveredDevices() {
        if (MainActivity.bluetooth.getStatus() == Bluetooth.DISABLED) {
            MainActivity.bluetooth.turnOn(this, Bluetooth.REQUEST_ENABLE_BT_DISCOVER);
            return;
        }

        if (btnDiscover.getText().equals(getText(R.string.startDiscovery))) {
            MainActivity.bluetooth.startDiscovery();
        } else {
            MainActivity.bluetooth.stopDiscovery();

        }
    }

    //Enter here after user selects "yes" or "no" to enabling bluetooth
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == Bluetooth.REQUEST_ENABLE_BT_PAIR) {
                btnPaired.callOnClick();
            } else if (requestCode == Bluetooth.REQUEST_ENABLE_BT_DISCOVER) {
                btnDiscover.callOnClick();
            }

        } else
            EventBus.getDefault().post(new Bluetooth.BluetoothEvent(Bluetooth.DISABLED, null));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleBluetoothEvent(Bluetooth.BluetoothEvent event) {

        switch (event.getAction()) {
            case Bluetooth.DISCOVERY_STARTED:
                Toast.makeText(this, getString(R.string.startDiscovery) + "...", Toast.LENGTH_SHORT).show();
                btnDiscover.setText(getText(R.string.stopDiscovery));
                devType.setText(getText(R.string.showDiscoveredDevices));
                btArrayAdapter.clear(); //clear items
                break;

            case Bluetooth.DISCOVERY_FINISHED:
                Toast.makeText(this, getString(R.string.discoveryFinished), Toast.LENGTH_SHORT).show();
                btnDiscover.setText(getText(R.string.startDiscovery));
                break;

            case Bluetooth.DEVICE_FOUND:
                BluetoothDevice device = (BluetoothDevice) event.getData();
                String address = device.getAddress();
                String name = device.getName();
                if (name == null) name = getString(R.string.noData);

                //Check if already in the list
                for (int i = 0; i < btArrayAdapter.getCount(); i++) {
                    if (btArrayAdapter.getItem(i).contains(address)) {
                        btArrayAdapter.remove(btArrayAdapter.getItem(i));
                        btArrayAdapter.insert(name + "\n" + address, i);
                        btArrayAdapter.notifyDataSetChanged();
                        return;
                    }
                }

                //Add the name to the list
                btArrayAdapter.add(name + "\n" + address);
                btArrayAdapter.notifyDataSetChanged();
                break;

            case Bluetooth.CONNECTED:
                finish();
                break;

            default:
                //Got status action
                setCurrentStatus(event.getAction());
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.bluetooth.stopDiscovery();
        EventBus.getDefault().unregister(this);
    }
}

