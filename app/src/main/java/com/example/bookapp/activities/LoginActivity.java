package com.example.bookapp.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.R;
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

    // AlertDialog
    private AlertDialog dialog;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        //setup AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please wait...")
                .setMessage("Please wait...")
                .setIcon(R.drawable.dialog_icon)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Action to perform when OK button is clicked
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Action to perform when Cancel button is clicked
                    }
                });
        dialog = builder.create();



        //handle click , go to register screen
        binding.noAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        //handle click , begin login
        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
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
        dialog.setMessage("Logging in...");
        dialog.show();

        //login user
        firebaseAuth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        checkUser();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });


    }

    private void checkUser() {
//        progressBar.setVisibility(View.GONE);
        //check if user is user or admin from realtime database
        //check current user
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        //check in db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        dialog.dismiss();
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