package com.locator.vishnu_grocery.myapplication;

import java.io.*;
import java.io.File;
import java.util.HashMap;

import android.Manifest;
import android.os.Bundle;
import android.os.StrictMode;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;

public class MainActivity extends Activity implements OnClickListener, PlaceSelectionListener {

    private static final int REQUEST_PICK_FILE = 1;

    boolean m_send_file;
    private TextView m_file_path;
    private File m_selected_file;
    private String m_application_load_service = "http://ec2-54-213-232-224.us-west-2.compute.amazonaws.com/load";
    private String m_application_retrieve_service = "http://ec2-54-213-232-224.us-west-2.compute.amazonaws.com/retrieve";
    private String m_store_name = "";
    private String m_address = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button browse = (Button)findViewById(R.id.browse);
        Button send_file = (Button)findViewById(R.id.send_file);
        Button retrieve_aisles = (Button)findViewById(R.id.retrieve_aisles);
        PlaceAutocompleteFragment autocomplete_fragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        browse.setOnClickListener(this);
        send_file.setOnClickListener(this);
        retrieve_aisles.setOnClickListener(this);
        autocomplete_fragment.setOnPlaceSelectedListener(this);

        m_file_path = (TextView)findViewById(R.id.file_path);
        m_send_file = true;
    }

    @Override
    public void onPlaceSelected(Place place) {
        Log.i("onPlaceSelected", "Place: " + place.getName());
        Log.i("onPlaceSelected", "Address: " + place.getAddress());
        m_store_name = place.getName().toString();
        m_address = place.getAddress().toString();
    }

    @Override
    public void onError(Status status) {
        Log.e("onError", "An error occurred: " + status);
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
                if (m_selected_file != null && m_store_name != null && m_address != null) {
                    load(); // at some point, we should figure out how to do this work in
                            // a separate thread, as the above is doing
                }
                break;
            case R.id.retrieve_aisles:
                if (m_store_name != null && m_address != null) {
                    retrieve(); // at some point, we should figure out how to do this work in
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
                        m_selected_file = new File
                                (data.getStringExtra(FilePicker.EXTRA_FILE_PATH));

                        Log.i("onActivityResult", m_selected_file.toString());

                        m_file_path.setText(m_selected_file.getPath());
                    }
                    break;
            }
        }
    }

    private void retrieve() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("store_name", m_store_name);
        params.put("address", m_address);

        try {
            HttpUtility.sendPostRequest(m_application_retrieve_service, params);
            String[] response = HttpUtility.readMultipleLinesRespone();

            String message = "";
            for (String line : response) {
                message += line + '\n';
            }

            generate_dialog_box("Response from service", message);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void load() {
        send_key_data();

        if (m_send_file) {
            send_file();
        } else {
            m_send_file = true; // reset for the next request
        }
    }

    private void send_key_data() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("store_name", m_store_name);
        params.put("address", m_address);

        try {
            HttpUtility.sendPostRequest(m_application_load_service, params);
            String[] response = HttpUtility.readMultipleLinesRespone();

            String message = "";
            for (String line : response) {
                message += line + '\n';
            }

            if (message.contains("FAILED")) {
                Log.e("send_key_data()", "We had an issue with server processing the keys that were sent for the store.");
                m_send_file = false;
            }

            generate_dialog_box("Response from service", message);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void send_file() {
        try {
            String message = "";
            HttpUtility.sendPostFileRequest(m_application_load_service, m_selected_file);

            String[] response = HttpUtility.readMultipleLinesRespone();
            for (String line : response) {
                message += line + '\n';
            }

            generate_dialog_box("Response from service", message);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void generate_dialog_box(String title, String message) {
        AlertDialog box = new AlertDialog.Builder(this).create();
        box.setTitle(title);
        box.setMessage(message);
        box.show();
    }
}
