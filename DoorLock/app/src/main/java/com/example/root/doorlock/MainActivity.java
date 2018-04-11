package com.example.root.doorlock;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;
import static android.provider.Telephony.Carriers.PASSWORD;

public class MainActivity extends AppCompatActivity {
    // list of NFC technologies detected:
    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcF.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(), Ndef.class.getName()
            }
    };

    OkHttpClient client;
    TextView tv;
    TextView result;
    Button auth;
    Button unauth;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public String UID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.tv_result);
        tv = findViewById(R.id.text);
        tv.setText("Scan a tag to start....");

        auth = findViewById(R.id.btn_auth);
        unauth = findViewById(R.id.btn_unauth);

        //send a request to the server to insert the id of the scanned tag into the database when the authorise button is clicked
        auth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(UID.isEmpty()) {
                    Toast.makeText(getBaseContext(), "Scan a tag first!", Toast.LENGTH_LONG).show();
                } else {
                    JSONObject postBody = new JSONObject();
                    try {
                        postBody.put("action", "authorise");
                        postBody.put("id", UID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    sendRequest(postBody);
                }
            }
        });

        //send a request the server to delete the id of the tag from the database
        unauth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(UID.isEmpty()) {
                    Toast.makeText(getBaseContext(), "Scan a tag first!", Toast.LENGTH_LONG).show();
                } else {
                    JSONObject postBody = new JSONObject();
                    try {
                        postBody.put("action", "unauthorise");
                        postBody.put("id", UID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    sendRequest(postBody);
                }
            }
        });

        client = new OkHttpClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // creating pending intent:
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // creating intent receiver for NFC events:
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        // enabling foreground dispatch for getting intent from NFC event:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // disabling foreground dispatch:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            //get id of tag
            UID = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            //display it
            tv.setText("NFC Tag UID: " + UID + "\n");
            //set up a json object for a request to the server
            JSONObject postBody = new JSONObject();
            try {
                //request to check if the id is in the database
                postBody.put("action", "checkdb");
                postBody.put("id", UID);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //send the request
            sendRequest(postBody);
        }
    }

    //magic
    private String ByteArrayToHexString(byte [] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";

        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    //sends a request to the server with the given json object
    private void sendRequest(JSONObject jobj) {
        Request request = new Request.Builder()
                .url("http://192.168.0.11/test.php")
                .post(RequestBody.create(JSON, jobj.toString()))
                .build();

        // Get a handler that can be used to post to the main thread
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

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        result.setText(responseData);
                    }
                });
            }
        });
    }

}



