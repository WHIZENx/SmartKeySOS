package com.cmu.project.sosapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.cmu.project.sosapp.Options.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class SetIDActivity extends AppCompatActivity {

    DatabaseReference reference;

    private EditText Name, Id;
    private ProgressBar loadingProgress;
    private Button okBtn;

    private FirebaseAuth mAuth;
    FirebaseUser user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_id);

        Name = findViewById(R.id.name);
        Id = findViewById(R.id.id);
        loadingProgress = findViewById(R.id.reg_progress);
        okBtn = findViewById(R.id.ok);
        loadingProgress.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                okBtn.setVisibility(View.INVISIBLE);
                loadingProgress.setVisibility(View.VISIBLE);
                final String id = Id.getText().toString();
                final String name = Name.getText().toString();

                if( id.isEmpty() || name.isEmpty()) {
                    // something goes wrong : all fields must be filled
                    // we need to display an error message
                    Options.showMessage(SetIDActivity.this, "Please Verify All fields", 2) ;
                    okBtn.setVisibility(View.VISIBLE);
                    loadingProgress.setVisibility(View.INVISIBLE);
                }
                else {
                    CreateUserAccount(name, id);
                }
            }
        });
    }

    boolean check = false;
    boolean check_online = false;
    boolean succes = false;

    private void CreateUserAccount(final String name, final String id) {
        check = false;
        check_online = false;
        succes = false;
        mAuth.signInWithEmailAndPassword(name+"@gmail.com",id).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot postsnap: dataSnapshot.getChildren()) {
                                Users users = postsnap.getValue(Users.class);
                                if (users.getId().equals(id) && users.getStatus().equals("online")) {
                                    check_online = true;
                                }
                            }

                            if (check_online && user == null) {
                                Options.showMessage(SetIDActivity.this, "This ID is online!", 1);
                                okBtn.setVisibility(View.VISIBLE);
                                loadingProgress.setVisibility(View.INVISIBLE);
                            } else {
                                user = mAuth.getCurrentUser();
                                succes = true;
                                Options.showMessage(SetIDActivity.this, "Welcome to Smart Key SOS!", 1);
                                Intent intent = new Intent(SetIDActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                SetIDActivity.this.overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
                else {
                    // this method create user account with specific email and password
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");

                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot postsnap: dataSnapshot.getChildren()) {
                                Users users = postsnap.getValue(Users.class);
                                if (users.getId().equals(id)) {
                                    Options.showMessage(SetIDActivity.this, "Name invalid!", 1);
                                    okBtn.setVisibility(View.VISIBLE);
                                    loadingProgress.setVisibility(View.INVISIBLE);
                                    check = true;
                                }
                            }

                            if (!check && user == null) createUser(name, id);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
                }
            }
        });


    }

    private void createUser(final String name, final String id) {
        mAuth.createUserWithEmailAndPassword(name+"@gmail.com",id)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful() && user == null) {
                            SetUser(name, id, mAuth.getCurrentUser());
                        }
                        else
                        {
                            // account creation failed
                            Options.showMessage(SetIDActivity.this, "Name has already use or ID must be more 6 characters!", 1);
                            okBtn.setVisibility(View.VISIBLE);
                            loadingProgress.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    private void SetUser(final String name, final String id, final FirebaseUser currentUser) {

        UserProfileChangeRequest profleUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        currentUser.updateProfile(profleUpdate)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            assert firebaseUser != null;
                            String userid = firebaseUser.getUid();
                            reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("name", name);
                            hashMap.put("id", id);
                            hashMap.put("userid", userid);
                            hashMap.put("status", "online");

                            reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        Options.showMessage(SetIDActivity.this, "Set ID Complete!", 1);
                                        Intent intent = new Intent(SetIDActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        SetIDActivity.this.overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
                                        finish();
                                    }
                                }
                            });
                        }
                    }
                });


    }
}
