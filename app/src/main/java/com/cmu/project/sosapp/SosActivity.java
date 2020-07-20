package com.cmu.project.sosapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cmu.project.sosapp.Model.PersonList;
import com.cmu.project.sosapp.Model.Sos;
import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.cmu.project.sosapp.Options.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SosActivity extends AppCompatActivity {

    CircleImageView per_img;

    String uid, id;
    DatabaseReference reference;

    TextView text_id, text_name, text_latitude, text_longitude, text_location;

    FirebaseAuth mAuth;
    FirebaseUser curUser;

    MapView mMapView;
    private GoogleMap googleMap;

    private LocationManager locationManager = null;
    private LocationListener locationListener = null;

    String latitude, longitude, sosKey, check;

    Button receive;
    Vibrator v;
    MediaPlayer mediaPlayer;

    Handler handler;

    boolean check_re = false;
    private ProgressBar loadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");
        id = intent.getStringExtra("id");
        latitude = intent.getStringExtra("latitude");
        longitude = intent.getStringExtra("longitude");
        sosKey = intent.getStringExtra("sosKey");
        check = intent.getStringExtra("check");

        per_img = findViewById(R.id.per_img);
        text_id = findViewById(R.id.id);
        text_name = findViewById(R.id.name);
        text_latitude = findViewById(R.id.latitude);
        text_longitude = findViewById(R.id.longitude);

        text_location = findViewById(R.id.location);

        receive = findViewById(R.id.receive);
        loadingProgress = findViewById(R.id.reg_progress);

        text_latitude.setText("Latitude: "+latitude);
        text_longitude.setText("Longitude: "+longitude);

        mAuth = FirebaseAuth.getInstance();
        curUser = mAuth.getCurrentUser();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new SosActivity.MyLocationListener();
        if (displayGpsStatus() && checkLocationPermission()) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 1000, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        }

        mMapView = findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(SosActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        playSound();
        vibrate();

        handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run(){
                vibrate();
                if(v.hasVibrator()){
                    handler.postDelayed(this, 10000);
                }
                if (mediaPlayer == null) {
                    handler.removeCallbacksAndMessages(null);
                }
            }
        }, 10000);

        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                receive.setVisibility(View.INVISIBLE);
                reference = FirebaseDatabase.getInstance().getReference("Danger").child(sosKey);

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("status", "Yes");

                reference.updateChildren(hashMap);
                loadingProgress.setVisibility(View.GONE);
                receive.setVisibility(View.GONE);
                stopSound();
                v.cancel();
                handler.removeCallbacksAndMessages(null);
                check_re = true;
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Person").child(curUser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postsnap: dataSnapshot.getChildren()) {
                    PersonList personList = postsnap.getValue(PersonList.class);
                    if (personList.getId().equals(id)) {
                        text_name.setText("Name: "+personList.getName()+"\n(ID: "+id+")");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        switch (Integer.parseInt(check)) {
            case 1: {
                text_id.setTextColor(Color.parseColor("#388E3C"));
                text_id.setText("เกิดเหตุฉุกเฉินน้อย!");
                break;
            }
            case 2: {
                text_id.setTextColor(Color.parseColor("#FBC02D"));
                text_id.setText("เกิดเหตุฉุกเฉินปานกลาง!");
                break;
            }
            case 3: {
                text_id.setTextColor(Color.parseColor("#D32F2F"));
                text_id.setText("เกิดเหตุฉุกเฉินมาก!");
                break;
            }
        }

        mMapView.setVisibility(View.VISIBLE);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                // For showing a move to my location button
                googleMap.setMyLocationEnabled(true);

                // For dropping a marker at a point on the Map
                LatLng l = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                googleMap.addMarker(new MarkerOptions().position(l).title("Help!").snippet("Warning!"));

                // For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder().target(l).zoom((float) 13.5).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });

    }

    private void vibrate() {
        int dot = 200;      // Length of a Morse Code "dot" in milliseconds
        int dash = 500;     // Length of a Morse Code "dash" in milliseconds
        int short_gap = 200;    // Length of Gap Between dots/dashes
        int medium_gap = 500;   // Length of Gap Between Letters
        int long_gap = 1000;    // Length of Gap Between Words
        long[] pattern = {
                0,  // Start immediately
                dot, short_gap, dot, short_gap, dot,    // s
                medium_gap,
                dash, short_gap, dash, short_gap, dash, // o
                medium_gap,
                dot, short_gap, dot, short_gap, dot,    // s
                long_gap
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, 0));

        } else {
            v.vibrate(pattern, 0);
        }
    }

    private void playSound() {
        mediaPlayer = MediaPlayer.create(SosActivity.this, R.raw.warn);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void stopSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        return gpsStatus;
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
            String subdistrict = null;
            String country = null;
            String code = null;
            String province = null;
            String district = null;
            String postcode = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(Double.parseDouble(latitude),
                        Double.parseDouble(longitude), 1);
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
            text_location.setText("  Sub District: "+subdistrict+
                    "\n\n\tDistrict: "+district+
                    "\n\n\tProvince: "+province+
                    "\n\n\tCode: "+postcode+
                    "\n\n\tCountry: "+country+" ("+code+")");
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
    public void onBackPressed() {
        if (check_re) super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        v.cancel();
        stopSound();
        handler.removeCallbacksAndMessages(null);
        if (locationListener != null) locationManager.removeUpdates(locationListener);
    }
}
