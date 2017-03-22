package com.locator.vishnu_grocery.myapplication;
import java.io.*;
import java.io.Console;
import java.io.File;
import android.Manifest;
import android.os.Bundle;
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
import android.app.AlertDialog;

public class MainActivity extends Activity implements OnClickListener {

    private static final int REQUEST_PICK_FILE = 1;

    private TextView filePath;
    private Button Browse;
    private Button Send_File;
    private File selectedFile;

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
    }

    public void onClick(View v) {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);

        switch(v.getId()) {
            case R.id.browse:
                Intent intent = new Intent(this, FilePicker.class);
                Log.d("onClick", "here");
                Log.d("CURRENT_DIRECTORY 1",FilePicker.CURRENT_DIRECTORY);
                startActivityForResult(intent, REQUEST_PICK_FILE);
                Log.d("CURRENT_DIRECTORY 2",FilePicker.CURRENT_DIRECTORY);
                break;
            case R.id.send_file:
                Log.d("onClick", "in send_file case");
                if (selectedFile != null) {
                    String requestURL = "http://ec2-54-213-232-224.us-west-2.compute.amazonaws.com/test1";
                    try {
                        Log.d("onActivityResult", "here3");
                        HttpUtility.sendPostFileRequest(requestURL, selectedFile);
                        Log.d("onActivityResult", "here4");
                        String[] response = HttpUtility.readMultipleLinesRespone();
                        String message = new String("");
                        for (String line : response) {
                            System.out.println(line);
                            message += line + '\n';
                        }
                        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                        alertDialog.setTitle("Response from service");
                        alertDialog.setMessage(message);
                        alertDialog.show();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", "here");
        Log.d("RESULT_OK", (Integer.toString(RESULT_OK)));
        Log.d("resultCode value", Integer.toString(resultCode));
        Log.d("CURRENT_DIRECTORY 3",FilePicker.CURRENT_DIRECTORY);
        if(resultCode == RESULT_OK) {
            switch(requestCode) {
                case REQUEST_PICK_FILE:
                    Log.d("onActivityResult", "here1");
                    if(data.hasExtra(FilePicker.EXTRA_FILE_PATH)) {
                        selectedFile = new File
                                (data.getStringExtra(FilePicker.EXTRA_FILE_PATH));
                        filePath.setText(selectedFile.getPath());
                        Log.d("onActivityResult", "here2");
                    }
                    break;
            }
        }
    }
}
