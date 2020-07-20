package com.cmu.project.sosapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import de.hdodenhof.circleimageview.CircleImageView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cmu.project.sosapp.Model.Sos;
import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PersonActivity extends AppCompatActivity {

    CircleImageView per_img;

    String name, id, img_url, check, userid;
    Button btn_sos;
    DatabaseReference reference;

    TextView text_id, text_name;

    FirebaseAuth mAuth;
    FirebaseUser curUser;

    private LocationManager locationManager = null;
    private LocationListener locationListener = null;

    String latitude, longitude;

    private ProgressBar loadingProgress;

    int check_radio = 0;
    AlertDialog optionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarCustom);
        setSupportActionBar(toolbar);
        TextView textView = (TextView)toolbar.findViewById(R.id.toolbarTextView);
        final TextView textViewactive = (TextView) toolbar.findViewById(R.id.toolbarTextViewActive);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        id = intent.getStringExtra("id");
        img_url = intent.getStringExtra("imgurl");
        check = intent.getStringExtra("check");
        userid = intent.getStringExtra("userid");

        textView.setText(name);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userid);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Users users = dataSnapshot.getValue(Users.class);
                if (users.getStatus().equals("online")) {
                    textViewactive.setText("Active");
                    textViewactive.setTextColor(Color.parseColor("#05df29"));
                } else {
                    textViewactive.setText("Inactive");
                    textViewactive.setTextColor(Color.parseColor("#bfbfbf"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        per_img = findViewById(R.id.per_img);

        Glide.with(PersonActivity.this).load(img_url).into(per_img);

        btn_sos = findViewById(R.id.send);
        text_id = findViewById(R.id.id);
        text_name = findViewById(R.id.name);

        loadingProgress = findViewById(R.id.reg_progress);

        text_name.setText("Name: "+name);
        text_id.setText("ID: "+id);

        mAuth = FirebaseAuth.getInstance();
        curUser = mAuth.getCurrentUser();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        optionDialog = new AlertDialog.Builder(PersonActivity.this)
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

        btn_sos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgress.setVisibility(View.VISIBLE);
                btn_sos.setVisibility(View.INVISIBLE);
                sent = false;
                locationListener = new PersonActivity.MyLocationListener();
                if (displayGpsStatus() && checkLocationPermission() && !sent && check_radio != 0) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 1000, locationListener);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
                } else {
                    if (check_radio == 0) {
                        Options.showMessage(PersonActivity.this, "Please select emergency sequence!", 1);
                        loadingProgress.setVisibility(View.INVISIBLE);
                        btn_sos.setVisibility(View.VISIBLE);
                    } else {
                        optionDialog.show();
                        loadingProgress.setVisibility(View.INVISIBLE);
                        btn_sos.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

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
                                Intent intent = new Intent(PersonActivity.this, SosActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtra("uid", sos.getUserid());
                                intent.putExtra("id", sos.getFrom());
                                intent.putExtra("latitude", sos.getLatitude());
                                intent.putExtra("longitude", sos.getLongitude());
                                intent.putExtra("sosKey", sos.getSosKey());
                                intent.putExtra("check", sos.getCheck());
                                Options.ChangePage(PersonActivity.this, intent);
                            }
                            if (users.getId().equals(sos.getFrom()) && sos.getStatus().equals("Yes")) {
                                AlertDialog optionDialog = new AlertDialog.Builder(PersonActivity.this)
                                        .setTitle("Helper!")
                                        .setMessage(sos.getFromname()+" ("+sos.getTo()+") has already received sos!")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                reference.child(sos.getSosKey()).removeValue();
                                                NotificationManager notificationManager =
                                                        (NotificationManager) PersonActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                                assert notificationManager != null;
                                                notificationManager.cancel(1000);
                                            }
                                        })
                                        .setIcon(R.drawable.success)
                                        .create();
                                if(!(PersonActivity.this).isFinishing())
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

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_1:
                if (checked)
                    check_radio = 1;
                break;
            case R.id.radio_2:
                if (checked)
                    check_radio = 2;
                break;
            case R.id.radio_3:
                if (checked)
                    check_radio = 3;
                break;
        }
    }

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

    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    boolean sent = false;

    ValueEventListener valueEventListener;
    DatabaseReference ref;

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(final Location location) {
            latitude = ""+location.getLatitude();
            longitude = ""+location.getLongitude();
            if (!sent) {
                ref = FirebaseDatabase.getInstance().getReference("Users").child(curUser.getUid());

                valueEventListener = ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final Users users = dataSnapshot.getValue(Users.class);
                        reference = FirebaseDatabase.getInstance().getReference("Danger");
                        String sosKey = reference.push().getKey();

                        Sos sos = new Sos(users.getId(), ""+id, name, ""+check_radio, latitude, longitude, curUser.getUid(), "No", sosKey, "No");

                        reference.child(sosKey).setValue(sos).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    sent = true;
                                    locationManager.removeUpdates(locationListener);
                                    Options.showMessage(PersonActivity.this, "Sent SOS Complete!", 1);
                                    Intent intent = new Intent(PersonActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    PersonActivity.this.overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
                                    finish();
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
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
        if (valueEventListener != null) {
            Options.showMessage(PersonActivity.this, "You already cancel sent sos!", 1);
            ref.removeEventListener(valueEventListener);
        }
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
                ActivityCompat.requestPermissions(PersonActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }
}
