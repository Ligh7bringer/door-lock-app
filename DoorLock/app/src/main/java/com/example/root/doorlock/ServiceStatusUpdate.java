package com.example.root.doorlock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ServiceStatusUpdate extends Service {
    OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String SERVER = "http://192.168.0.11/request_handler.php";
    JSONObject json = new JSONObject();
    private int ID = 0;
    private int DELAY = 10000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initNotifications();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    json.put("action", "security");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendRequest(json);
                Log.d("REQUEST", "!!!");

                handler.postDelayed(this, DELAY);
            }
        }, DELAY);

        return Service.START_STICKY;
    }

    private void sendRequest(JSONObject jsonObject) {
        Request request = new Request.Builder()
                .url(SERVER)
                .post(RequestBody.create(JSON, jsonObject.toString()))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response" + response);
                }

                // Read data on the worker thread
                final String responseData = response.body().string();
                if(!responseData.isEmpty()) {
                    displayNotification(ID, "Warning!", "Someone tried to open the door " + responseData);
                    ID++;
                    Log.d("RESPONSE", responseData);
                }
            }
        });
    }

    private final String channelID = "default";
    private void initNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = getString(R.string.channel);
            String description = getString(R.string.channelDescription);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(channelID, name, importance);
            mChannel.setDescription(description);
            NotificationManager manager = (NotificationManager)(getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE));
            if (manager != null) {
                manager.createNotificationChannel(mChannel);
            } else {
                Log.e("ERROR", "Notification Manager is null!");
            }
        }
    }

    private void displayNotification(int id, String title, String text) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Intent intent = new Intent(this, ImageDisplay.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent notificationIntent= PendingIntent.getActivity(this, id, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setContentIntent(notificationIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(id, mBuilder.build());
    }
}

