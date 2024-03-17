package com.example.carsos;

import static java.lang.Math.abs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.carsos.databinding.ActivityMainBinding;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private RetrieveSensorDataTask retrieveSensorDataTask;
    private  String SOS_NUMBER;
    private String SOS_MESSAGE;
    private String LOCATION;
    String ESP32_PORT = "80";
    String ESP32_IP = "192.168.240.6";
    private Timer sosTimer;
    private static final int PERMISSION_REQUEST_SEND_SMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SOS_NUMBER = "+91" + getIntent().getStringExtra("PHONE");
        SOS_MESSAGE = "Emergency. Vehicle number " + getIntent().getStringExtra("VEHICLE") + " has crashed.";
        LOCATION = getIntent().getStringExtra("LOCATION");

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            initialiseESP();
        } else {
            // Permission is not granted, request the permission
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SEND_SMS);
        }

    }

    private void initialiseESP() {
        retrieveSensorDataTask = new RetrieveSensorDataTask();
        binding.btnConnect.setOnClickListener(view -> {
            binding.infoTV.setText("Connecting to ESP32");
            binding.btnConnect.setEnabled(false);
            retrieveSensorDataTask.execute();
        });

        binding.btnCancelSOS.setOnClickListener(view -> {
            cancelSOSMessage(view);
        });
    }

    private class RetrieveSensorDataTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            while (!isCancelled()) { // Continuously retrieve data until the task is cancelled
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;
                String data = null;

                try {
                    // Construct URL for retrieving sensor data from ESP32
                    URL url = new URL("http://" + ESP32_IP + ":" + ESP32_PORT + "/data");
                    // Establish connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    // Read response
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuilder stringBuilder = new StringBuilder();
                    if (inputStream != null) {
                        reader = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }
                        data = stringBuilder.toString();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // Close connections
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Publish progress to update UI
                publishProgress(data);

                try {
                    Thread.sleep(1000); // Wait for 5 seconds before retrieving data again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values.length > 0) {
                binding.infoTV.setText("Connected to device " + ESP32_IP);
                binding.mainTV.setText("Data: " + values[0]);
                Data data = new Gson().fromJson(values[0], Data.class);
                Log.i("esp", "onProgressUpdate: pitch : " + data.getPitch());
                Log.i("esp", "onProgressUpdate: roll : " + data.getRoll());
                if(abs(Float.parseFloat(data.getPitch())) >= 80 || abs(Float.parseFloat(data.getRoll())) >= 80) {
                    binding.ivWarning.setVisibility(View.VISIBLE);
                    binding.infoTV.setText("Crash detected. Initialising sos procedure. Sending message to "+ SOS_NUMBER+" in 5 seconds. Please press cancel if it is a mistake");
                    binding.btnCancelSOS.setVisibility(View.VISIBLE);
                    sosTimer = new Timer();
                    sosTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            sendSOSMessage();
                        }
                    }, 6000);
                    retrieveSensorDataTask.cancel(true);

                }
            }
        }
    }
    private void sendSOSMessage() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(SOS_NUMBER, null, SOS_MESSAGE+". Last location was "+LOCATION, null, null);
            Log.i("esp", "message sent successfully");
            Toast.makeText(this, "Message sent successfully", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Log.i("esp sms", "sendSOSMessage: error "+e.getMessage());
            e.printStackTrace();
            finish();
        }
    }
    public void cancelSOSMessage(View view) {
        if (sosTimer != null) {
            sosTimer.cancel();
            showToast("SOS message sending cancelled");
            binding.btnConnect.setEnabled(true);
            finish();
        } else {
            showToast("No SOS message scheduled");
        }
    }
    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel the AsyncTask when the activity is destroyed
        if (retrieveSensorDataTask != null) {
            retrieveSensorDataTask.cancel(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, schedule sending the SOS message
                initialiseESP();
            } else {
                // Permission denied, show a message and handle accordingly
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SEND_SMS);
                showToast("Permission denied to send SMS");
            }
        }
    }


}
