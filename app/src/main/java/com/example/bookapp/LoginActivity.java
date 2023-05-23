package com.example.bookapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    //view binding
    private ActivityLoginBinding binding;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    private ProgressBar progressBar;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();


        // Create a LinearLayout and set its orientation
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        //setup progressBar
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.VISIBLE);


        // Create a TextView for the message and set its text
        TextView messageTextView = new TextView(this);
        messageTextView.setText("Please wait...");


        // Set the LinearLayout as the content view of your activity
        setContentView(layout);


        //handle click , go to register screen
        binding.noAccountTv.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        //handle click , begin login
        binding.loginBtn.setOnClickListener(view -> validateData());
    }

    private String email ="" ,password ="";
    private void validateData() {
        /*before logged in let do some data validation*/
        //get data
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString().trim();
        //validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email pattern", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();

        }
        else{
            loginUser();
        }

    }

    private void loginUser() {
        progressBar.setVisibility(View.GONE);

        //login user
        firebaseAuth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener(authResult -> checkUser())
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                });


    }

    private void checkUser() {
        progressBar.setVisibility(View.GONE);
        //check if user is user or admin from realtime database
        //check current user
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        //check in db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        //get user type
                        String userType = ""+snapshot.child("userType").getValue();
                        //check user type
                        if (userType.equals("user")){
                            //this is simple user , open user dashboard
                            startActivity(new Intent(LoginActivity.this, DashBoardUserActivity.class));
                            finish();
                        } else if (userType.equals("admin")) {
                            //this is admin , open admin dashboard
                            startActivity(new Intent(LoginActivity.this, DashboardAdminActivity.class));
                            finish();
                            
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                    }
                });
    }
}