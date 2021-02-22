package com.example.myfirstapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button uDocuments, uManual;
    private TextView pulseRate, oxygenRate;

    BluetoothSocket bluetoothSocket = null;
    String mac = "";
    String timeStamp;
    InputStream inputStream;
    int i;

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Health Data");
    Date date;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uDocuments = findViewById(R.id.upDocu);
        uManual = findViewById(R.id.uManual);
        pulseRate = findViewById(R.id.pRate);
        oxygenRate = findViewById(R.id.oRate);

        bluetoothDeviceConnect();

        uDocuments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent upDocuIntent = new Intent(MainActivity.this, uploadDocuments.class);
                upDocuIntent.putExtra("MAC_ADDR", mac);
                startActivity(upDocuIntent);
            }
        });

        uManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent uManualIntent = new Intent(MainActivity.this, userManual.class);
                startActivity(uManualIntent);
            }
        });
        //00:18:E4:00:43:6C
        //00001101-0000-1000-8000-00805F9B34FB

    }
    public void bluetoothDeviceConnect() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice myHealthDevice;
        String[] macAddress = {"00:18:E4:00:43:6C"};
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                for(i = 0; i < macAddress.length; i++) {
                    if(macAddress[i].equals(device.getAddress())) {
                        break;
                    }
                }
                if(i < macAddress.length)
                    break;
            }
        }
        mac += macAddress[i];
        myHealthDevice = bluetoothAdapter.getRemoteDevice(macAddress[i]);
        try {
            bluetoothSocket = myHealthDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            bluetoothSocket.connect();
            if(bluetoothSocket.isConnected()) {
                receiveData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveData() {
        try {
            inputStream = bluetoothSocket.getInputStream();
            byte[] buffer = new byte[20];
            inputStream.read(buffer, 0, 20);
            int bpmValue, spO2Value;
            bpmValue = 0;
            spO2Value = 0;
            for(i = 19; buffer[i] != 64 && buffer[i] != 35; i--);
            if(buffer[i] == 64) {
                for(i--; buffer[i] != 35; i--)
                    spO2Value = spO2Value * 10 + (buffer[i] - 48);

                for(i--; buffer[i] != 64; i--)
                    bpmValue = bpmValue * 10 + (buffer[i] - 48);
            }
            else {
                for(i--; buffer[i] != 64; i--)
                    bpmValue = bpmValue * 10 + (buffer[i] - 48);

                for(i--; buffer[i] != 35; i--)
                    spO2Value = spO2Value * 10 + (buffer[i] - 48);
            }
            pulseRate.setText("Heart-Rate : " + String.valueOf(bpmValue) + " beats (BPM)");
            oxygenRate.setText("Oxygen Saturation : " + String.valueOf(spO2Value) + " % (SpO2)");
            date = new Date();
            timeStamp = simpleDateFormat.format(date.getTime());
            databaseReference.child(mac).child(timeStamp).child("Heart-Rate").setValue(String.valueOf(bpmValue));
            databaseReference.child(mac).child(timeStamp).child("Oxygen-Saturation").setValue(String.valueOf(spO2Value));
            refresh(30000);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }

    public void refresh(long milliseconds) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                receiveData();
            }
        }, milliseconds);
    }

}