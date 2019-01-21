package com.example.localuser.geofence;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.internal.NavigationMenu;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import io.github.yavski.fabspeeddial.FabSpeedDial;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener {

    LinearLayout linearLayout;

    private GoogleMap mMap;
    //Play services Location
    private static final int MY_PERMISSION_REQUEST_CODE = 7192;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 300193;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT_INTERVAL = 10;

    boolean flag = true;

    DatabaseReference ref;
    GeoFire geoFire;

    String radius, phone_number;
    int rad;

    Circle circle;

    Marker mCurrent;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent intent = getIntent();
        radius = intent.getStringExtra("e1");
        phone_number = intent.getStringExtra("e2");
        rad = Integer.parseInt(radius);

        FabSpeedDial fabSpeedDial = (FabSpeedDial) findViewById(R.id.fabSpeedDial);
        fabSpeedDial.setMenuListener(new FabSpeedDial.MenuListener() {
            @Override
            public boolean onPrepareMenu(NavigationMenu navigationMenu) {
                return true;
            }

            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.create_geofence:
                        creategeofence();
                        break;
                    case R.id.remove_geofence:
                        removegeofence();
                        break;
                }
                return true;
            }

            @Override
            public void onMenuClosed() {

            }
        });


        ref = FirebaseDatabase.getInstance().getReference("MYLOCATION");
        geoFire = new GeoFire(ref);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Geofence");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.inflateMenu(R.menu.menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.search:
                        final SearchView sv = (SearchView) item.getActionView();
                        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextSubmit(String query) {
                                String str = query;
                                if (str != null) {
                                    searchLocation(str);
                                } else {
                                    Toast.makeText(MapsActivity.this, "Location Parameter Empty!", Toast.LENGTH_SHORT).show();
                                }
                                return true;
                            }

                            @Override
                            public boolean onQueryTextChange(String newText) {
                                return true;
                            }
                        });
                        break;
                    case R.id.map_type_normal:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                    case R.id.settings:
                        onBackPressed();
                        break;
                    case R.id.map_type_none:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                        break;
                    case R.id.map_type_terrain:
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        break;
                    case R.id.map_type_satellite:
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        break;
                    case R.id.map_type_hybrid:
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        break;
                }
                return true;
            }
        });
        setUpLocation();
    }

    private void removegeofence() {
        flag = true;
        if (circle != null) {
            remove_fence();
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
            alertDialog.setTitle("VIKAS").setIcon(R.drawable.icon);
            alertDialog.setMessage("GEOFENCE REMOVED.").setPositiveButton("OK", null);
            alertDialog.show();
        } else {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
            alertDialog.setTitle("VIKAS").setIcon(R.drawable.icon);
            alertDialog.setMessage("NO GEOFENCE TO BE REMOVED.").setPositiveButton("OK", null);
            alertDialog.show();
        }
    }

    private void creategeofence() {
        final double latitude = mLastLocation.getLatitude();
        final double longitude = mLastLocation.getLongitude();
        //Create Geofence
        if (mLastLocation != null && flag) {
            flag = false;
            LatLng area = new LatLng(latitude, longitude);
            circle = drawCircle(new LatLng(latitude, longitude));
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
            alertDialog.setTitle("VIKAS").setIcon(R.drawable.icon);
            alertDialog.setMessage("GEOFENCE CREATED.").setPositiveButton("OK", null);
            alertDialog.show();
            //Add GeoQuery here
            GeoQueryFunction(area);
        } else {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
            alertDialog.setTitle("VIKAS").setIcon(R.drawable.icon);
            alertDialog.setMessage("REMOVE OLD GEOFENCE.").setPositiveButton("OK", null);
            alertDialog.show();
        }
    }
    private void searchLocation(String location) {
        if (location != null) {
            Geocoder gc = new Geocoder(MapsActivity.this);
            List<Address> list = null;
            try {
                list = gc.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (list.size() > 0) {
                Address address = list.get(0);
                String locality = address.getLocality();
                Toast.makeText(MapsActivity.this, locality, Toast.LENGTH_LONG).show();
                double lat = address.getLatitude();
                double lng = address.getLongitude();
                gotoLocationZoom(lat, lng, 15);
            } else {
                Toast.makeText(MapsActivity.this, "No Such Location Exists: " + location, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(MapsActivity.this, "Blank field", Toast.LENGTH_LONG).show();
        }
    }


    private void GeoQueryFunction(final LatLng area) {
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(area.latitude, area.longitude), (float) (rad / 1000));
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                sendNotification("VIKAS", String.format("%s HAVE ENTERED HOME BOUNDARY", key));
                SmsManager.getDefault().sendTextMessage(phone_number, null, "Vikas has entered Home \nhttp://maps.google.com?q=" + area.latitude + "," + area.longitude, null, null);
            }

            @Override
            public void onKeyExited(String key) {
                sendNotification("VIKAS", String.format("%s ARE NO LONGER IN HOME BOUNDARY", key));
                SmsManager.getDefault().sendTextMessage(phone_number, null, "Vikas is no longer at Home \nhttp://maps.google.com?q=" + area.latitude + "," + area.longitude, null, null);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d("MOVE", String.format("%s ARE MOVING WITHIN HOME BOUNDARY [%f/%f]", key, location.longitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("ERROR", "" + error);
            }
        });
    }

    private Circle drawCircle(LatLng latLng) {
        CircleOptions options = new CircleOptions()
                .center(latLng)
                .radius(rad)
                .fillColor(0x220000FF)
                .strokeColor(Color.BLUE)
                .strokeWidth(3);
        return mMap.addCircle(options);
    }


    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            final double latitude = mLastLocation.getLatitude();
            final double longitude = mLastLocation.getLongitude();
            //Update To Firebase
            geoFire.setLocation("YOU", new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    //Add Marker
                    if (mCurrent != null) {
                        mCurrent.remove();
                    }
                    //remove old marker
                    mCurrent = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("You"));
                    //Move Camera to this Position
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));
                }
            });
            Log.d("Vikas", String.format("Location Was Changed: %f/ %f", latitude, longitude));
        } else
            Log.d("Vikas", "Cannot get Your Location");
    }

    private void remove_fence() {
        circle.remove();
        circle = null;
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT_INTERVAL);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            else {
                Toast.makeText(this, "This device is not Supported", Toast.LENGTH_SHORT);
                finish();
            }
            return false;
        }
        return true;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
    }
    private void sendNotification(String title, String content) {
        Notification.Builder builder = new Notification.Builder(this).setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(content);

        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MapsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;

        manager.notify(new Random().nextInt(), notification);

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)

        {
            return;
        }
       LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, (com.google.android.gms.location.LocationListener) this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }


    private void gotoLocationZoom(double lat, double lng, int i) {
        LatLng latLng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, i);
        mMap.moveCamera(update);
        if (circle != null) {
            circle.remove();
        }
        flag = false;
        circle = drawCircle(new LatLng(lat, lng));
        GeoQueryFunction(new LatLng(lat, lng));
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}