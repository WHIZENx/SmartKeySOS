package com.cmu.project.sosapp.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cmu.project.sosapp.Model.PersonList;
import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.cmu.project.sosapp.PersonActivity;
import com.cmu.project.sosapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.MyViewHolder> {

    private Context mContext;
    private List<Users> mData;

    public UsersAdapter(Context mContext, List<Users> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.recycle_person, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
        final Users usersList = mData.get(position);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postsnap: dataSnapshot.getChildren()) {
                    Users users = postsnap.getValue(Users.class);
                    if (users.getId().equals(usersList.getId()) && users.getStatus().equals("online")) {
                        holder.img_on.setVisibility(View.VISIBLE);
                        holder.img_off.setVisibility(View.GONE);
                    } else {
                        holder.img_off.setVisibility(View.VISIBLE);
                        holder.img_on.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(Options.isValidContextForGlide(mContext)) Glide.with(mContext).load("https://lh3.googleusercontent.com/proxy/h3i58nVrLESe4Yf8kpoib2xFzW0Huxj0_GeIUB7X2ucS7hqlsB_BU9Unq0oTubgy3nGUmRsjOi-g9iMaHKH27u3WRQCEcrItHY0d6O_ihzk_8n8sStwXTbTqrubYOphdKo8QF0k").into(holder.profile_img);
        holder.name.setText(usersList.getName());
        holder.id.setText("ID: " + usersList.getId());
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView id, name;
        CircleImageView profile_img, img_on, img_off;
        RelativeLayout rev_person;

        public MyViewHolder(View itemView) {
            super(itemView);

            id = itemView.findViewById(R.id.id);
            name = itemView.findViewById(R.id.name);
            profile_img = itemView.findViewById(R.id.profile_img);
            rev_person = itemView.findViewById(R.id.rev_person);

            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
        }
    }

}
