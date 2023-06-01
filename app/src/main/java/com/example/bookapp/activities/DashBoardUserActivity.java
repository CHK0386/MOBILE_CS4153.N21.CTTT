package com.example.bookapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.bookapp.databinding.ActivityDashboardUserBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashBoardUserActivity extends AppCompatActivity {

    //view binding
    private ActivityDashboardUserBinding binding;
    //firebase auth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());//init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();

        //handle click , logout
        binding.logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();
                checkUser();
            }
        });
    }

    private void checkUser() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            //user not loggin in
            //start main screen
            startActivity(new Intent(this, MainActivity.class));
            finish();//finish this activity
        }
        else {
            //logged in get user info
            String email = firebaseUser.getEmail();
            binding.subTitleTv.setText(email);
        }

    }
}