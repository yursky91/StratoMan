package com.yursky.stratoman;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

class Bluetooth {

    private final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter(); //get a handle on the bluetooth radio;

    private ConnectedThread connectedThread;    //bluetooth background worker thread to send and receive data
    private BluetoothSocket btSocket;   //bi-directional client-to-client data path
    private BroadcastReceiver blReceiver;   //event listener

    private final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    static final int REQUEST_ENABLE_BT_PAIR = 1; // used to identify adding bluetooth names
    static final int REQUEST_ENABLE_BT_DISCOVER = 2; // used to identify adding bluetooth names

    static final int DISABLED = 10;
    static final int ENABLED = 11;
    static final int DISCONNECTED = 12;
    static final int CONNECTING = 13;
    static final int CONNECTED = 14;
    static final int DISCOVERY_STARTED = 15;
    static final int DISCOVERY_FINISHED = 16;
    static final int DEVICE_FOUND = 17;
    static final int MESSAGE_RECEIVED = 18;

    private String address;
    String name;

    private int connectAttempts;  //number of tries

    void registerReceiver(Context context) {
        blReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        EventBus.getDefault().post(new BluetoothEvent(DEVICE_FOUND, intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
                        break;

                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        EventBus.getDefault().post(new BluetoothEvent(DISCOVERY_STARTED, null));
                        break;

                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        EventBus.getDefault().post(new BluetoothEvent(DISCOVERY_FINISHED, null));
                        break;

                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

                        if (state == BluetoothAdapter.STATE_OFF)
                            EventBus.getDefault().post(new BluetoothEvent(DISABLED, null));
                        else if (state == BluetoothAdapter.STATE_ON)
                            EventBus.getDefault().post(new BluetoothEvent(ENABLED, null));
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(blReceiver, filter);
    }

    void unregisterReceiver(Context context) {
        context.unregisterReceiver(blReceiver);
    }

    void turnOn(Context context, int request) {
        if (btAdapter == null) {
            EventBus.getDefault().post(new BluetoothEvent(DISABLED, null));
            return;
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) context).startActivityForResult(enableBtIntent, request);
        }
    }

    void turnOff() {
        if (btAdapter == null) {
            EventBus.getDefault().post(new BluetoothEvent(DISABLED, null));
            return;
        }

        btAdapter.disable(); //turn off
    }

    Set<BluetoothDevice> getPairedDevices() {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            EventBus.getDefault().post(new BluetoothEvent(DISABLED, null));
            return null;
        }

        return btAdapter.getBondedDevices();
    }

    void startDiscovery() {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            EventBus.getDefault().post(new BluetoothEvent(DISABLED, null));
            return;
        }

        if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();

        btAdapter.startDiscovery();
    }

    void stopDiscovery() {
        if (btAdapter == null || !btAdapter.isEnabled()) return;

        //Check if the device is already discovering
        if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();

    }

    void connect(final Context context, final String address, final String name, final int connectionAttempts) {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            EventBus.getDefault().post(new BluetoothEvent(DISABLED, null));
            return;
        }
        if (connectionAttempts < 1) return;

        stopDiscovery();

        this.address = address;
        this.name = name;
        this.connectAttempts = connectionAttempts;
        final BluetoothDevice device = btAdapter.getRemoteDevice(address);

        //Spawn a new thread to avoid blocking the GUI one
        new Thread() {
            public void run() {
                EventBus.getDefault().post(new BluetoothEvent(CONNECTING, name));
                Log.d("DEBUG", context.getString(R.string.connectingTo));

                //Establish the Bluetooth socket connection
                try {
                    if (btSocket != null) btSocket.close(); //stop previous connection

                    btSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                    btSocket.connect();

                } catch (Exception e) {
                    e.printStackTrace();
                    EventBus.getDefault().post(new BluetoothEvent(DISCONNECTED, null));
                    Log.d("DEBUG", context.getString(R.string.connectionFailed));

                    try {
                        btSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        Log.d("DEBUG", "ПРОВЕРКА_2");
                    }

                    connectAttempts--;
                    if (connectAttempts > 0) run();

                    return;
                }

                connectedThread = new ConnectedThread(context);
                connectedThread.start();
                connectAttempts = 5;
                EventBus.getDefault().post(new BluetoothEvent(CONNECTED, name));
                Log.d("DEBUG", context.getString(R.string.connectedTo) + name);
            }
        }.start();
    }

    void disconnect() {
        try {
            connectAttempts = 0;
            btSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int getStatus() {
        if (btAdapter == null || !btAdapter.isEnabled())
            return DISABLED;

        if (btSocket == null || !btSocket.isConnected()) {

            if (connectAttempts > 0) return CONNECTING;
            else return DISCONNECTED;
        }

        return CONNECTED;
    }

    /* Call this from the main activity to send data to the remote device */
    void write(String input) {
        byte[] bytes = input.getBytes();
        try {
            connectedThread.outStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static class BluetoothEvent {
        private final int action;
        private final Object data;

        BluetoothEvent(int action, Object data) {
            this.action = action;
            this.data = data;
        }

        int getAction() {
            return action;
        }

        Object getData() {
            return data;
        }
    }


    private class ConnectedThread extends Thread {
        private final InputStream inStream;
        private final OutputStream outStream;
        private final Context context;

        ConnectedThread(Context context) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = btSocket.getInputStream();
                tmpOut = btSocket.getOutputStream();
            } catch (IOException ignored) {
            }

            this.inStream = tmpIn;
            this.outStream = tmpOut;
            this.context = context;
        }

        public void run() {
            StringBuilder msg = new StringBuilder();
            //Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    int read = inStream.read();
                    if (read != -1) {
                        char ch = (char) read;

                        if (ch != '\n') {
                            msg.append(ch);
                        } else {
                            Log.d("DEBUG", msg.toString());
                            EventBus.getDefault().post(new BluetoothEvent(MESSAGE_RECEIVED, msg.toString()));
                            msg = new StringBuilder();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("DEBUG", context.getString(R.string.disconnected));
                    EventBus.getDefault().post(new BluetoothEvent(DISCONNECTED, null));
                    connect(context, address, name, connectAttempts);
                    break;
                }
            }
        }
    }
}
