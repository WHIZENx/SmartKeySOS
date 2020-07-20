package com.cmu.project.sosapp.Options;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Status {

    private static DatabaseReference reference;

    public static void setStatus(String uid, String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);
        reference.updateChildren(hashMap);
    }
}
