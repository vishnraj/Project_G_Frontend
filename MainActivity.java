package com.locator.vishnu_grocery.myapplication;
import java.io.*;
import java.io.Console;
import java.io.File;
import java.util.Iterator;

import android.Manifest;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements OnClickListener {

    private static final int REQUEST_PICK_FILE = 1;

    private TextView filePath;
    private Button Browse;
    private Button Send_File;
    private File selectedFile;
    private EditText Store_Address;
    private AlertDialog Alert_Dialog;

    private String my_load_service = "http://ec2-54-213-232-224.us-west-2.compute.amazonaws.com/test1";
    private String map_quest_key = "ZIepWWgn7Exiak4rFV7s2biRGfknHhit";
    private String map_quest_address_verification_service = "http://www.mapquestapi.com/geocoding/v1/address?";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filePath = (TextView)findViewById(R.id.file_path);
        Browse = (Button)findViewById(R.id.browse);
        Store_Address = (EditText)findViewById(R.id.Entered_Store_Address);
        Send_File = (Button)findViewById(R.id.send_file);
        Browse.setOnClickListener(this);
        Send_File.setOnClickListener(this);
        Alert_Dialog = new AlertDialog.Builder(this).create();
    }

    public void onClick(View v) {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);

        switch(v.getId()) {
            case R.id.browse:
                Intent intent = new Intent(this, FilePicker.class);
                startActivityForResult(intent, REQUEST_PICK_FILE);
                break;
            case R.id.send_file:
                if (selectedFile != null) {
                    load(); // at some point, we should figure out how to do this work in
                            // a separate thread, as the above is doing
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            switch(requestCode) {
                case REQUEST_PICK_FILE:
                    if(data.hasExtra(FilePicker.EXTRA_FILE_PATH)) {
                        selectedFile = new File
                                (data.getStringExtra(FilePicker.EXTRA_FILE_PATH));
                        filePath.setText(selectedFile.getPath());
                    }
                    break;
            }
        }
    }

    private String construct_location_url(String address) {
        String delims = "[ ]+";
        String[] tokens = address.split(delims);
        String address_params = new String("");
        int i = 1;
        for (String token : tokens) {
            address_params += token;
            if (tokens.length != i)
                address_params += "+";
            ++i;
        }

        String url = map_quest_address_verification_service + "key=" + map_quest_key + "&location=" + address_params;
        Log.d("construct_location_url", url);

        return url;
    }

    void load() {
        Store_Address = (EditText)findViewById(R.id.Entered_Store_Address);
        String text_address = Store_Address.getText().toString();
        Log.d("onClick", text_address);
        String url = construct_location_url(text_address);

        String geo_code_quality_code = new String("");
        String geo_code_quality = new String("");
        try {
            HttpUtility.sendGetRequest(url);
            String[] response = HttpUtility.readMultipleLinesRespone();
            String full_response = new String("");
            for (String line : response) {
                full_response += line;
            }
            JSONObject json = new JSONObject(full_response);
            JSONArray results = json.getJSONArray("results");
            JSONArray location_data = new JSONArray();
            for (int i = 0; i < results.length(); ++i) {
                if (results.optJSONObject(i).has("locations")) {
                    location_data = (JSONArray) results.optJSONObject(i).get("locations");
                    break;
                }
            }
            Integer len = location_data.length();
            for (int i = 0; i < location_data.length(); ++i) {
                Log.d("LoadingOperations", "in loop");
                if (location_data.optJSONObject(i).has("geocodeQualityCode")) {
                    geo_code_quality_code = location_data.optJSONObject(i).get("geocodeQualityCode").toString();
                }
                if (location_data.optJSONObject(i).has("geocodeQuality")) {
                    geo_code_quality = location_data.optJSONObject(i).get("geocodeQuality").toString();
                }

                if (geo_code_quality_code != "" && geo_code_quality != "") {
                    break;
                }
            }
            Log.d("LoadingOperations", geo_code_quality_code);
            Log.d("LoadingOperations", geo_code_quality);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        try {
            if (!geo_code_quality.equals("ADDRESS") && !geo_code_quality.equals("POINT") || !Character.toString(geo_code_quality_code.charAt(2)).equals("A")) {
                Log.d("LoadingOperations1", geo_code_quality);
                char full_stree_confidence = geo_code_quality_code.charAt(2);
                Log.d("LoadingOperations1", Character.toString(full_stree_confidence));
                Alert_Dialog = new AlertDialog.Builder(this).create();
                Alert_Dialog.setTitle("Error");
                Alert_Dialog.setMessage("Location could not be sent to server because it did not appear to be valid. Please trying again.");
                Alert_Dialog.show();
                return;
            }
            HttpUtility.sendPostFileRequest(my_load_service, selectedFile);
            String[] response = HttpUtility.readMultipleLinesRespone();
            String message = new String("");
            for (String line : response) {
                message += line + '\n';
            }
            // next, we will also send the store location and name as a separate blob
            // in the form of a get reqeust, so that these can be paired with the aisle data
            Alert_Dialog = new AlertDialog.Builder(this).create();
            Alert_Dialog.setTitle("Response from service");
            Alert_Dialog.setMessage(message);
            Alert_Dialog.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
