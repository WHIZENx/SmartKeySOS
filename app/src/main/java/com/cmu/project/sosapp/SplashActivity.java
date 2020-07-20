package com.cmu.project.sosapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cmu.project.sosapp.Options.Options;
import com.cmu.project.sosapp.Service.JobServices;
import com.cmu.project.sosapp.Service.Service;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    TextView t1;
    LinearLayout lin;
    Intent intent;
    private FirebaseAuth mAuth;

    LocationManager locationManager;

    AlertDialog optionDialog;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Options.SetFullScreen(SplashActivity.this);
        Options.HideNavBar(SplashActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user != null) {
            startService(new Intent(this, Service.class));
            scheduleJob();
        }

        t1 = findViewById(R.id.t1);
        lin = findViewById(R.id.lin);

        optionDialog = new AlertDialog.Builder(SplashActivity.this)
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

        locationManager = (LocationManager) SplashActivity.this.getSystemService(Context.LOCATION_SERVICE);
        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  ) {
            optionDialog.show();
        } else {
            while (!checkLocationPermission()) {
                ActivityCompat.requestPermissions(SplashActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            openApp();
        }
    }

    public void scheduleJob() {
        ComponentName componentName = new ComponentName(this, JobServices.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            JobInfo info = new JobInfo.Builder(123, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPersisted(true)
                    .setPeriodic(15 * 60 * 1000)
                    .build();
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            int resultCode = scheduler.schedule(info);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.d("JobService", "Job scheduled");
            } else {
                Log.d("JobService", "Job scheduling failed");
            }
        }

    }

    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void openApp() {

        int START_TIME = 500;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                t1.setVisibility(View.VISIBLE);
                t1.startAnimation(AnimationUtils.loadAnimation(SplashActivity.this, R.anim.fade_in_activity));
            }
        }, START_TIME);

        int SPLASH_TIME = 1000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                lin.setVisibility(View.VISIBLE);
                lin.startAnimation(AnimationUtils.loadAnimation(SplashActivity.this, R.anim.fade_in_activity));
            }
        }, SPLASH_TIME);

        int END_TIME = 2000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (user == null) {
                    intent = new Intent(SplashActivity.this, SetIDActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                } else {
                    intent = new Intent(SplashActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                }
                Options.ChangePage(SplashActivity.this, intent);
                SplashActivity.this.finish();
            }
        }, END_TIME);

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
                ActivityCompat.requestPermissions(SplashActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            openApp();
        }
    }
}
