package com.cmu.project.sosapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.hdodenhof.circleimageview.CircleImageView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import com.cmu.project.sosapp.Model.PersonList;
import com.cmu.project.sosapp.Model.Sos;
import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.cmu.project.sosapp.Options.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class AddActivity extends AppCompatActivity {

    CircleImageView ImgUserPhoto;
    static int PReqCode = 1 ;
    static int REQUESCODE = 1 ;
    Uri pickedImgUri ;

    DatabaseReference reference;

    private EditText Id,Name;
    private ProgressBar loadingProgress;
    private Button addBtn;

    FirebaseAuth mAuth;
    FirebaseUser curUser;

    boolean check_id = false;
    boolean check_repeat = false;

    String userid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        Id = findViewById(R.id.id);
        Name = findViewById(R.id.name);
        loadingProgress = findViewById(R.id.reg_progress);
        addBtn = findViewById(R.id.add);
        loadingProgress.setVisibility(View.INVISIBLE);

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                check_id = false;
                check_repeat = false;
                addBtn.setVisibility(View.INVISIBLE);
                loadingProgress.setVisibility(View.VISIBLE);
                final String id = Id.getText().toString();
                final String name = Name.getText().toString();

                if( id.isEmpty() || name.isEmpty()) {
                    // something goes wrong : all fields must be filled
                    // we need to display an error message
                    Options.showMessage(AddActivity.this, "Please Verify all fields", 2) ;
                    addBtn.setVisibility(View.VISIBLE);
                    loadingProgress.setVisibility(View.INVISIBLE);
                }
                else {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot postsnap: dataSnapshot.getChildren()) {
                                Users users = postsnap.getValue(Users.class);
                                if (users.getId().equals(id)) {
                                    check_id = true;
                                    userid = users.getUserid();
                                }
                            }
                            if (check_id) {
                                DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("Person").child(curUser.getUid());

                                ref2.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot postsnap: dataSnapshot.getChildren()) {
                                            PersonList personList = postsnap.getValue(PersonList.class);
                                            if (personList.getId().equals(id)) {
                                                check_repeat = true;
                                            }
                                        }
                                        if (!check_repeat) AddUser(id, name, pickedImgUri, userid);
                                        else {
                                            Options.showMessage(AddActivity.this, "You already add this ID", 1);
                                            addBtn.setVisibility(View.VISIBLE);
                                            loadingProgress.setVisibility(View.INVISIBLE);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                            else {
                                Options.showMessage(AddActivity.this, "ID hasn't exist", 1);
                                addBtn.setVisibility(View.VISIBLE);
                                loadingProgress.setVisibility(View.INVISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            }
        });

        ImgUserPhoto = findViewById(R.id.add_img) ;

        ImgUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= 22) {
                    checkAndRequestForPermission();
                }
                else
                {
                    openGallery();
                }
            }
        });

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
                                Intent intent = new Intent(AddActivity.this, SosActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtra("uid", sos.getUserid());
                                intent.putExtra("id", sos.getFrom());
                                intent.putExtra("latitude", sos.getLatitude());
                                intent.putExtra("longitude", sos.getLongitude());
                                intent.putExtra("sosKey", sos.getSosKey());
                                intent.putExtra("check", sos.getCheck());
                                Options.ChangePage(AddActivity.this, intent);
                            }
                            if (users.getId().equals(sos.getFrom()) && sos.getStatus().equals("Yes")) {
                                Intent intent = new Intent(AddActivity.this, SosActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                Options.ChangePage(AddActivity.this, intent);
                                AlertDialog optionDialog = new AlertDialog.Builder(AddActivity.this)
                                        .setTitle("Helper!")
                                        .setMessage(sos.getFromname()+" ("+sos.getTo()+") has already received sos!")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                reference.child(sos.getSosKey()).removeValue();
                                                NotificationManager notificationManager =
                                                        (NotificationManager) AddActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                                assert notificationManager != null;
                                                notificationManager.cancel(1000);
                                            }
                                        })
                                        .setIcon(R.drawable.success)
                                        .create();
                                if(!(AddActivity.this).isFinishing())
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

    // update user photo and name
    private void AddUser(final String id, final String name, Uri pickedImgUri, final String userid) {

        if (pickedImgUri == null) {
            reference = FirebaseDatabase.getInstance().getReference("Person").child(curUser.getUid());
            String personKey = reference.push().getKey();

            // create post Object
            PersonList personList = new PersonList(id, name, "https://i7.pngguru.com/preview/165/652/907/businessperson-computer-icons-avatar-clip-art-avatar.jpg", personKey, userid);

            reference.child(personKey).setValue(personList).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        Options.showMessage(AddActivity.this, "Add Person Complete!", 1);
                        updateUI();
                    }
                }
            });
        } else {
            // first we need to upload user photo to firebase storage and get url
            StorageReference mStorage = FirebaseStorage.getInstance().getReference().child("users_photos");
            final StorageReference imageFilePath = mStorage.child(pickedImgUri.getLastPathSegment());
            imageFilePath.putFile(pickedImgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // image uploaded succesfully
                    // now we can get our image url
                    imageFilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(final Uri uri) {

                            reference = FirebaseDatabase.getInstance().getReference("Person").child(curUser.getUid());
                            String personKey = reference.push().getKey();

                            // create post Object
                            PersonList personList = new PersonList(id, name, ""+uri, personKey, userid);

                            reference.child(personKey).setValue(personList).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        Options.showMessage(AddActivity.this, "Add Person Complete!", 1);
                                        updateUI();
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void updateUI() {
        Intent intent = new Intent(AddActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        AddActivity.this.overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
        finish();
    }

    private void openGallery() {
        //TODO: open gallery intent and wait for user to pick an image !

        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,REQUESCODE);
    }

    private void checkAndRequestForPermission() {
        if (ContextCompat.checkSelfPermission(AddActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(AddActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(AddActivity.this,"Please accept for required permission",Toast.LENGTH_SHORT).show();
            }

            else
            {
                ActivityCompat.requestPermissions(AddActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PReqCode);
            }

        }
        else
            openGallery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUESCODE && data != null ) {
            // the user has successfully picked an image
            // we need to save its reference to a Uri variable
            pickedImgUri = data.getData() ;
            ImgUserPhoto.setImageURI(pickedImgUri);
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
}
