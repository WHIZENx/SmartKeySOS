package com.cmu.project.sosapp.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cmu.project.sosapp.Model.PersonList;
import com.cmu.project.sosapp.Model.Users;
import com.cmu.project.sosapp.Options.Options;
import com.cmu.project.sosapp.PersonActivity;
import com.cmu.project.sosapp.R;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.MyViewHolder> {

    private Context mContext;
    private List<PersonList> mData;
    private FirebaseUser curUser;

    public PersonAdapter(Context mContext, List<PersonList> mData, FirebaseUser curUser) {
        this.mContext = mContext;
        this.mData = mData;
        this.curUser = curUser;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.recycle_person, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
        final PersonList personList = mData.get(position);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(personList.getUserid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Users users = dataSnapshot.getValue(Users.class);
                if (users.getStatus().equals("online")) {
                    holder.img_on.setVisibility(View.VISIBLE);
                    holder.img_off.setVisibility(View.GONE);
                } else {
                    holder.img_off.setVisibility(View.VISIBLE);
                    holder.img_on.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(Options.isValidContextForGlide(mContext)) Glide.with(mContext).load(personList.getImageURL()).into(holder.profile_img);
        holder.name.setText(personList.getName());
        holder.id.setText("ID: " + personList.getId());

        holder.rev_person.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, PersonActivity.class);
                intent.putExtra("name", personList.getName());
                intent.putExtra("id", personList.getId());
                intent.putExtra("imgurl", personList.getImageURL());
                intent.putExtra("userid", personList.getUserid());
                mContext.startActivity(intent);
                ((Activity) mContext).overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
            }
        });

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Person").child(curUser.getUid()).child(personList.getPersonKey());
                reference.removeValue();
            }
        });

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView id, name;
        CircleImageView profile_img, img_on, img_off;
        RelativeLayout rev_person;
        ImageView delete;

        public MyViewHolder(View itemView) {
            super(itemView);

            id = itemView.findViewById(R.id.id);
            name = itemView.findViewById(R.id.name);
            profile_img = itemView.findViewById(R.id.profile_img);
            rev_person = itemView.findViewById(R.id.rev_person);

            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);

            delete = itemView.findViewById(R.id.delete);
        }
    }

}
