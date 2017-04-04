package com.locator.vishnu_grocery.myapplication;
import java.io.*;
import java.io.Console;
import java.io.File;
import java.util.Iterator;
import java.util.HashMap;

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
    private AlertDialog Alert_Dialog;

    private String my_load_service = "http://ec2-54-213-232-224.us-west-2.compute.amazonaws.com/test1";
    private String map_quest_key = "ZIepWWgn7Exiak4rFV7s2biRGfknHhit";
    private String map_quest_address_verification_service = "http://www.mapquestapi.com/geocoding/v1/address?";

    // declared here to be modified and used later
    private String m_postal_code = new String();
    private String m_store_name = new String();

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

    private void load() {
        String store_address = ((EditText)findViewById(R.id.Entered_Store_Address)).getText().toString();
        Log.d("load", store_address);

        if (!request_location_data(store_address)) {
            generate_dialog_box("Error", "Location could not be sent to server because it did not appear to be valid. Please trying again.");
            return;
        }

        m_store_name = ((EditText)findViewById(R.id.Entered_Store_Name)).getText().toString();

        Log.d("load", m_postal_code);
        Log.d("load", m_store_name);

        // This will generate a dialog box with the data returned from the service
        // if successful - if it fails, whatever the error is, this will appear in
        // the dialog box instead
        send_file();
    }

    private void send_file() {
        try {
            // we will also send the store location and name as a separate blob
            // in the form of a get reqeust, so that these can be paired with the aisle data
            // if we receive a response from the server that says that the data is
            // already in the database, we not send the file

            HashMap<String, String> params = new HashMap<String, String>();
            params.put("postal_code", m_postal_code);
            params.put("store_name", m_store_name);
            HttpUtility.sendPostRequest(my_load_service, params);
            String[] response = HttpUtility.readMultipleLinesRespone();
            String message = new String("");
            for (String line : response) {
                message += line + '\n';
            }
            generate_dialog_box("Response from service", message);

            // we should check for a 400 error code here and
            // make sure that we do not proceed if that is the
            // case, as it will indicate (once we have implemented
            // it in the backend) that the data for this
            // store is already present in the database

            message = new String("");
            HttpUtility.sendPostFileRequest(my_load_service, selectedFile);
            response = HttpUtility.readMultipleLinesRespone();
            for (String line : response) {
                message += line + '\n';
            }

            generate_dialog_box("Response from service", message);
        } catch (IOException ex) {
            ex.printStackTrace();
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

    // REQUIRES:
    // The street address passed into the application
    // MODIFIES:
    // String passed in that will hold the required location
    // data (Street+Zipcode) to be sent to the server
    // RETURNS:
    // True if the quality of the data is good or False
    // if the quality of the data is bad
    private Boolean request_location_data(String store_address) {
        String url = construct_location_url(store_address);

        String geo_code_quality_code = new String("");
        String geo_code_quality = new String("");
        String street= new String("");
        String postal_code = new String("");
        JSONArray location_data = new JSONArray();
        try {
            HttpUtility.sendGetRequest(url);
            String[] response = HttpUtility.readMultipleLinesRespone();
            String full_response = new String("");
            for (String line : response) {
                full_response += line;
            }
            JSONObject json = new JSONObject(full_response);
            JSONArray results = json.getJSONArray("results");
            for (int i = 0; i < results.length(); ++i) {
                if (results.optJSONObject(i).has("locations")) {
                    location_data = (JSONArray) results.optJSONObject(i).get("locations");
                    break;
                }
            }

            // The only reason we do this is because the current api is returning
            // an array here, which appears to be usually length = 1, but this is done
            // as a precaution. As a result this will generally be a O(1) operation, as
            // are able to hash the JSON object in the array for the values we desire.
            for (int i = 0; i < location_data.length(); ++i) {
                Log.d("request_location_data", "in loop");
                if (location_data.optJSONObject(i).has("geocodeQualityCode")) {
                    geo_code_quality_code = location_data.optJSONObject(i).get("geocodeQualityCode").toString();
                }
                if (location_data.optJSONObject(i).has("postalCode")) {
                    postal_code = location_data.optJSONObject(i).get("postalCode").toString();
                }

                if (geo_code_quality_code != "" && geo_code_quality != "" && postal_code != "") {
                    break;
                }
            }
            Log.d("request_location_data", geo_code_quality_code);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        if (!Character.toString(geo_code_quality_code.charAt(4)).equals("A")) {
            char full_street_confidence = geo_code_quality_code.charAt(4);
            Log.d("request_location_data", Character.toString(full_street_confidence));
            return false;
        }
        
        m_postal_code = postal_code;

        return true;
    }

    private void generate_dialog_box(String title, String message) {
        Alert_Dialog = new AlertDialog.Builder(this).create();
        Alert_Dialog.setTitle("Response from service");
        Alert_Dialog.setMessage(message);
        Alert_Dialog.show();
    }
}
