package com.cmu.project.sosapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cmu.project.sosapp.Adapter.PersonAdapter;
import com.cmu.project.sosapp.Adapter.UsersAdapter;
import com.cmu.project.sosapp.Model.PersonList;
import com.cmu.project.sosapp.Model.Sos;
import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.cmu.project.sosapp.Options.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static AlertDialog optionDialog;
    RelativeLayout addperson;

    TextView id, name;

    private RecyclerView person_list;
    private PersonAdapter personAdapter;
    private List<PersonList> mPeople;

    private UsersAdapter usersAdapter;
    private List<Users> mUsers;

    private DatabaseReference reference;

    FirebaseAuth mAuth;
    FirebaseUser curUser;

    private ProgressBar loadingProgress;
    static int noti = 1, remove = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addperson = findViewById(R.id.addperson);

        id = findViewById(R.id.id);
        name = findViewById(R.id.name);
        loadingProgress = findViewById(R.id.reg_progress);

        mAuth = FirebaseAuth.getInstance();
        curUser = mAuth.getCurrentUser();

        noti = getIntent().getIntExtra("noti", 1);
        remove = getIntent().getIntExtra("remove", 0);

        if (remove == 1) reference.child(getIntent().getStringExtra("soskey")).removeValue();

        Status.setStatus(curUser.getUid(), "online");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(curUser.getUid());

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Users users = dataSnapshot.getValue(Users.class);
                id.setText(""+users.getId());
                name.setText(users.getName());
                if (users.getName().equals("admin")) personAdminList();
                else personList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        addperson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Options.ChangePage(MainActivity.this, intent);
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);

        person_list = findViewById(R.id.person_list);
        person_list.setHasFixedSize(true);
        person_list.setLayoutManager(linearLayoutManager);

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
                                Intent intent = new Intent(MainActivity.this, SosActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtra("uid", sos.getUserid());
                                intent.putExtra("id", sos.getFrom());
                                intent.putExtra("latitude", sos.getLatitude());
                                intent.putExtra("longitude", sos.getLongitude());
                                intent.putExtra("sosKey", sos.getSosKey());
                                intent.putExtra("check", sos.getCheck());
                                Options.ChangePage(MainActivity.this, intent);
                            }
                            if (users.getId().equals(sos.getFrom()) && sos.getStatus().equals("Yes")) {
                                optionDialog = new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Helper!")
                                        .setMessage(sos.getFromname()+" ("+sos.getTo()+") has already received sos!")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                reference.child(sos.getSosKey()).removeValue();
                                                NotificationManager notificationManager =
                                                        (NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                                assert notificationManager != null;
                                                notificationManager.cancel(1000);
                                            }
                                        })
                                        .setIcon(R.drawable.success)
                                        .create();
                                if(!(MainActivity.this).isFinishing())
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

    private void personAdminList() {
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUsers = new ArrayList<>();
                for (DataSnapshot postsnap: dataSnapshot.getChildren()) {
                    Users users = postsnap.getValue(Users.class);
                    mUsers.add(users);
                }

//                Collections.sort(mPeople, new Comparator<PersonList>() {
//                    @Override
//                    public int compare(PersonList obj1, PersonList obj2) {
//                        if (Integer.parseInt(obj1.getId()) > Integer.parseInt(obj2.getId())) {
//                            return 1;
//                        } else {
//                            return -1;
//                        }
//                    }
//                });

                usersAdapter = new UsersAdapter(MainActivity.this, mUsers);
                person_list.setAdapter(usersAdapter);

                loadingProgress.setVisibility(View.INVISIBLE);
                person_list.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void personList() {
        reference = FirebaseDatabase.getInstance().getReference("Person").child(curUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mPeople = new ArrayList<>();
                for (DataSnapshot postsnap: dataSnapshot.getChildren()) {
                    PersonList personList = postsnap.getValue(PersonList.class);
                    mPeople.add(personList);
                }

//                Collections.sort(mPeople, new Comparator<PersonList>() {
//                    @Override
//                    public int compare(PersonList obj1, PersonList obj2) {
//                        if (Integer.parseInt(obj1.getId()) > Integer.parseInt(obj2.getId())) {
//                            return 1;
//                        } else {
//                            return -1;
//                        }
//                    }
//                });

                personAdapter = new PersonAdapter(MainActivity.this, mPeople, curUser);
                person_list.setAdapter(personAdapter);

                loadingProgress.setVisibility(View.INVISIBLE);
                person_list.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_logout:
                Status.setStatus(curUser.getUid(), "offline");
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(MainActivity.this, SetIDActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Options.ChangePage(MainActivity.this, intent);
                MainActivity.this.finish();
                return true;
            case R.id.action_settings:
                intent = new Intent(MainActivity.this, SettingActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Options.ChangePage(MainActivity.this, intent);
                break;
            case R.id.action_abouts:
                Options.showMessage(MainActivity.this, "Create program for example invention!", 2);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    public static int getNoti() {
        return noti;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (optionDialog != null && optionDialog.isShowing()) optionDialog.dismiss();
        noti = 0;
    }
}
