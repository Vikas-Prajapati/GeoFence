package com.example.localuser.geofence;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class Settings extends AppCompatActivity {
        Toolbar toolbar1;
        EditText radius, phone_number;

        ImageView makeCall;

        String iradius, inumber;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_settings);

            radius = findViewById(R.id.radius);

            phone_number = findViewById(R.id.phone_number);


            makeCall = findViewById(R.id.make_call);
            makeCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    inumber = phone_number.getText().toString();
                    if (inumber.length() == 0) {
                        Toast.makeText(Settings.this, "Phone Number is Empty!", Toast.LENGTH_SHORT).show();
                    } else if (inumber.length() < 10 && inumber.length() > 0) {
                        Toast.makeText(Settings.this, "Not a Valid Number!", Toast.LENGTH_SHORT).show();
                    } else {
                        if (ActivityCompat.checkSelfPermission(Settings.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + inumber));
                        startActivity(intent);
                    }
                }
            });

            toolbar1 = findViewById(R.id.toolbar1);
            toolbar1.setTitle("Settings");
            toolbar1.inflateMenu(R.menu.settings_menu);
            toolbar1.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.goback:
                            iradius = radius.getText().toString();
                            inumber = phone_number.getText().toString();
                            if (iradius.length() == 0) {
                                Toast.makeText(Settings.this, "Radius Parameter is Empty!", Toast.LENGTH_SHORT).show();
                            } else if (inumber.length() == 0) {
                                Toast.makeText(Settings.this, "Phone Number is Empty!", Toast.LENGTH_SHORT).show();
                            } else if (inumber.length() > 0 && inumber.length() < 10) {
                                Toast.makeText(Settings.this, "Not a valid Number!", Toast.LENGTH_SHORT).show();
                            } else {
                                Intent intent = new Intent(Settings.this, MapsActivity.class);
                                intent.putExtra("e1", iradius);
                                intent.putExtra("e2", inumber);
                                startActivity(intent);
                            }
                            break;
                    }
                    return true;
                }
            });

        }
    }

