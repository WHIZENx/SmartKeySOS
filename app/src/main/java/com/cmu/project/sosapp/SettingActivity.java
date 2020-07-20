package com.cmu.project.sosapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cmu.project.sosapp.Model.Sos;
import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SettingActivity extends AppCompatActivity {

    Button detect;
    TextView latitude, longitude, city;

    MapView mMapView;
    private GoogleMap googleMap;

    private LocationManager locationManager = null;
    private LocationListener locationListener = null;

    DatabaseReference reference;

    FirebaseAuth mAuth;
    FirebaseUser curUser;

    RelativeLayout rev;
    private ProgressBar loadingProgress;

    AlertDialog optionDialog;

    public boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        detect = findViewById(R.id.detect);
        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        city = findViewById(R.id.location);
        loadingProgress = findViewById(R.id.reg_progress);

        optionDialog = new AlertDialog.Builder(SettingActivity.this)
                .setTitle("Warning!")  // GPS not found
                .setMessage("You must Enable GPS!") // Want to enable?
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setIcon(R.drawable.warn)
                .create();
        optionDialog.setCancelable(false);
        optionDialog.setCanceledOnTouchOutside(false);

        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detect.setVisibility(View.INVISIBLE);
                Options.showMessage(SettingActivity.this, "Please wait...", 1);
                locationListener = new MyLocationListener();
                if (displayGpsStatus() && checkLocationPermission()) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 1000, locationListener);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
                } else {
                    detect.setVisibility(View.VISIBLE);
                    optionDialog.show();
                }
            }
        });

        rev = findViewById(R.id.rev);

        mMapView = findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(SettingActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAuth = FirebaseAuth.getInstance();
        curUser = mAuth.getCurrentUser();

        //Check
        DatabaseReference danger = FirebaseDatabase.getInstance().getReference("Users").child(curUser.getUid());

        danger.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final Users users = dataSnapshot.getValue(Users.class);
                reference = FirebaseDatabase.getInstance().getReference("Danger");

                reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                        for (DataSnapshot postsnap: dataSnapshot2.getChildren()) {
                            final Sos sos = postsnap.getValue(Sos.class);
                            if (users.getId().equals(sos.getTo()) && sos.getStatus().equals("No")) {
                                Intent intent = new Intent(SettingActivity.this, SosActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtra("uid", sos.getUserid());
                                intent.putExtra("id", sos.getFrom());
                                intent.putExtra("latitude", sos.getLatitude());
                                intent.putExtra("longitude", sos.getLongitude());
                                intent.putExtra("sosKey", sos.getSosKey());
                                intent.putExtra("check", sos.getCheck());
                                Options.ChangePage(SettingActivity.this, intent);
                            }
                            if (users.getId().equals(sos.getFrom()) && sos.getStatus().equals("Yes")) {
                                AlertDialog optionDialog = new AlertDialog.Builder(SettingActivity.this)
                                        .setTitle("Helper!")
                                        .setMessage(sos.getFromname()+" (ID: "+sos.getTo()+") has already received sos!")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                reference.child(sos.getSosKey()).removeValue();
                                                NotificationManager notificationManager =
                                                        (NotificationManager) SettingActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                                assert notificationManager != null;
                                                notificationManager.cancel(1000);
                                            }
                                        })
                                        .setIcon(R.drawable.success)
                                        .create();
                                if(!(SettingActivity.this).isFinishing())
                                {
                                    optionDialog.setCanceledOnTouchOutside(false);
                                    optionDialog.setCancelable(false);
                                    optionDialog.show();
                                }

                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(final Location location) {
            latitude.setText("Latitude: "+location.getLatitude());
            longitude.setText("Longitude: "+location.getLongitude());

            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap mMap) {
                    googleMap = mMap;

                    // For showing a move to my location button
                    googleMap.setMyLocationEnabled(true);

                    // For dropping a marker at a point on the Map
                    LatLng l = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(l).title("Your Position").snippet("You are here!"));

                    // For zooming automatically to the location of the marker
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(l).zoom((float) 13.25).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            });
            rev.setVisibility(View.VISIBLE);

            String subdistrict = null;
            String country = null;
            String code = null;
            String province = null;
            String district = null;
            String postcode = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
                    subdistrict = addresses.get(0).getSubLocality();
                    country = addresses.get(0).getCountryName();
                    code = addresses.get(0).getCountryCode();
                    postcode = addresses.get(0).getPostalCode();
                    province = addresses.get(0).getAdminArea();
                    district = addresses.get(0).getSubAdminArea();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            city.setText("  Sub District: "+subdistrict+
                    "\n\n\tDistrict: "+district+
                    "\n\n\tProvince: "+province+
                    "\n\n\tCode: "+postcode+
                    "\n\n\tCountry: "+country+" ("+code+")");

            detect.setVisibility(View.VISIBLE);
            locationManager.removeUpdates(locationListener);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationListener != null) locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            optionDialog.setCanceledOnTouchOutside(false);
            optionDialog.setCancelable(false);
            optionDialog.show();
        } else {
            if (optionDialog != null) optionDialog.dismiss();
            while (!checkLocationPermission()) {
                ActivityCompat.requestPermissions(SettingActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }
}
