package com.cmu.project.sosapp.Service;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.cmu.project.sosapp.MainActivity;
import com.cmu.project.sosapp.Model.Sos;
import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.cmu.project.sosapp.R;
import com.cmu.project.sosapp.SosActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class Service extends android.app.Service {
    FirebaseAuth mAuth;
    FirebaseUser curUser;
    Context context = this;
    private DatabaseReference reference;
    private final LocalBinder mBinder = new LocalBinder();
    protected Handler handler;

    public class LocalBinder extends Binder {
        public Service getService() {
            return Service.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mAuth = FirebaseAuth.getInstance();
        curUser = mAuth.getCurrentUser();

        //Check
        if (curUser != null) {
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
                                    Intent intent = new Intent(context, SosActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("uid", sos.getUserid());
                                    intent.putExtra("id", sos.getFrom());
                                    intent.putExtra("latitude", sos.getLatitude());
                                    intent.putExtra("longitude", sos.getLongitude());
                                    intent.putExtra("sosKey", sos.getSosKey());
                                    intent.putExtra("check", sos.getCheck());
                                    context.startActivity(intent);
                                }
                                if (users.getId().equals(sos.getFrom()) && sos.getStatus().equals("Yes") && sos.getSent().equals("No")) {
                                    showNotification(sos.getFromname(), sos.getTo(), sos.getSosKey());
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

        return START_STICKY;
    }

    private void showNotification(String user_name, String user_id, String sosKey) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.sos);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // The id of the channel.
        String id = "sos_id_channel";

        // The user-visible name of the channel.
        CharSequence name = "sos_channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[] { 500, 1000, 500 });

            mNotificationManager.createNotificationChannel(mChannel);
        }

        Notification notification =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.sos)
                        .setLargeIcon(bitmap)
                        .setContentTitle("✔ Helper!")
                        .setContentText(user_name+" ("+user_id+") has already received sos!")
                        .setAutoCancel(true)
                        .setVibrate(new long[] { 500, 1000, 500 })
                        .setChannelId(id)
                        .setContentIntent(getPendingIntent(context))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1000, notification);

        reference = FirebaseDatabase.getInstance().getReference("Danger").child(sosKey);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sent", "Yes");
        reference.updateChildren(hashMap);
    }

    private PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("noti", 1);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
