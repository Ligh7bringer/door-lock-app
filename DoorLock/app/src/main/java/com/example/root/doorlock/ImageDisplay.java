package com.example.root.doorlock;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageDisplay extends AppCompatActivity {
    private ListView lv;
    private String serverResponse = "";
    private ArrayList<SecurityImage> securityImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        lv = findViewById(R.id.list);
        securityImages = new ArrayList<>();

        JSONObject requestObj = new JSONObject();
        try {
            requestObj.put("action", "images");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(MainActivity.SERVER)
                .post(RequestBody.create(MainActivity.JSON, requestObj.toString()))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()) {
                    return;
                } else {
                    final String responseData = response.body().string();
                    serverResponse = responseData;
                    Log.d("RESPONSE", responseData);

                    if(!serverResponse.isEmpty()) {
                        String[] split = serverResponse.split(" ");
                        for (String s : split) {
                            SecurityImage image = new SecurityImage(s, s);
                            Log.d("IMAGE", s);
                            securityImages.add(image);
                        }
                    }

                    ImageDisplay.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<SecurityImage> adapter = new ImageArrayAdapter(getBaseContext(), 0, securityImages);
                            adapter.addAll(securityImages);
                            lv.setAdapter(adapter);
                        }
                    });

                }
            }
        });
    }

}

class ImageArrayAdapter extends ArrayAdapter<SecurityImage> {
    private Context context;
    private ArrayList<SecurityImage> images;
    private static final String IMG_URL = "http://192.168.0.11/images/";

    public ImageArrayAdapter(@NonNull Context context, int resource, ArrayList<SecurityImage> images) {
        super(context, resource);
        this.context = context;
        this.images = images;
    }

    @NonNull
    @Override
    public View getView(int position, View currentView, ViewGroup parent) {
        SecurityImage securityImage = images.get(position);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.listitem, null);

        TextView title = view.findViewById(R.id.title);
        ImageView image = view.findViewById(R.id.image);

        title.setText(securityImage.getTitle());

        Picasso.get().load(IMG_URL + securityImage.getImage()).into(image);
        Log.d("IMG URL", IMG_URL + securityImage.getImage());

        return view;
    }
}