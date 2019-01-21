package com.example.localuser.geofence;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread thread = new Thread() {
            @Override
            public void run() {
                {
                    try {
                        sleep(5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Intent intent=new Intent(MainActivity.this, Settings.class);
                        startActivity(intent);
                    }
                }
            }
        };
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
