package com.robotikauinsg.bluetooth_control;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;

import android.os.Handler;
import android.os.Bundle;
import android.os.Message;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionResultListener;

import io.flutter.plugins.GeneratedPluginRegistrant;
import io.flutter.embedding.android.FlutterActivity;

public class MainActivity extends FlutterActivity {
    private class BluetoothConnection extends Thread {
        private BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        BluetoothConnection(BluetoothSocket socket) {
            initializeStream(socket);
        }
        
        void initializeStream(BluetoothSocket socket) {
            bluetoothSocket = socket;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void read(byte[] buffer) throws IOException {
            int bytes = inputStream.read(buffer);
            systemHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
        }

        void write(String data) throws IOException {
            byte[] messageBuffer = data.getBytes();
            outputStream.write(messageBuffer);
        }

        public void run() {
            byte[] buffer = new byte[256];
            while (true) {
                try {
                    read(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        void disconnect() throws IOException {
            bluetoothSocket.close();
        }
    }

    private boolean bluetoothConnectionState = false;
    
    BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothConnection bluetoothConnection;
    private BluetoothSocket bluetoothSocket;

    private InputStream inputStream;
    private OutputStream outputStream;

    private MethodChannel channel;
    private Handler systemHandler;
    private StringBuilder messageData = new StringBuilder();
    private String toOutputDataBuilder;

    private static final String CHANNEL = "flutter.native/helper";

    private final UUID BLUETOOTH_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final int RECEIVE_MESSAGE = 1;

    @SurpressLint("HandlerLeak")
    private void setupHandler() {
        systemHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what != RECEIVE_MESSAGE) {
                    return;
                }
                
                byte[] readBuffer = (byte[]) msg.obj;
                String strInCOM = new String(readBuffer, 0, msg.arg1);

                messageData.append(strInCOM);
                int endOfLineIndex = messageData.indexOf(":");

                if (endOfLineIndex > 0) {
                    toOutputDataBuilder = messageData.substring(0, endOfLineIndex);
                    messageData.delete(0, messageData.length());
                    String data = toOutputDataBuilder.trim();

                    channel.invokeMethod("message", toOutputDataBuilder);
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneratedPluginRegistrant.registerWith(this);
        
        setupHandler();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        channel = new MethodChannel(getFlutterView(), CHANNEL);

        channel.setMethodCallHandler(
            new MethodChannel.MethodCallHandler() {
                @Override
                public void onMethodCall(MethodCall call, MethodChannel.Result result) {
                    handleMethod(call, result);
                }
            }
        );
    }

    private void handleMethod(MethodCall call, MethodChannel.Result result) {
        switch (call.method) {
            case "enumerate-devices":
                enumerateDevices(result);
                break;
            case "connect":
                connect(call, result);
                break;
            case "transmit":
                transmit(call);
                break;
        }
    }

    private void enumerateDevices(MethodChannel.Result result) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBoundedDevices();
        if (pairedDevices.size() > 0) {
            final ArrayList<Map> devices = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                Map<String, String> info = new HashMap<String, String>();
                info.put(device.getName(), device.getAddress());
                devices.add(info);
            }
            result.success(devices);
        }
    }

    private void connect(MethodCall call, MethodChannel.Result result) {
        BluetoothDevice btDevice = bluetoothAdapter.getRemoteDevice(call.arguments.toString());
        try {
            bluetoothSocket = btDevice.createRfcommSocketToServiceRecord(BLUETOOTH_UUID);
            bluetoothSocket.connect();
            bluetoothConnection = new BluetoothConnection(bluetoothSocket);
            bluetoothConnection.start();
            bluetoothConnectionState = true;
        } catch (IOException e) {
            try {
                bluetoothSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void transmit(MethodCall call) {
        try {
            String data = call.arguments.toString();
            bluetoothConnection.outputStream.write(data.getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
