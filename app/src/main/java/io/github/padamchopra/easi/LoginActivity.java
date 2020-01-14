package io.github.padamchopra.easi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    public EditText mEmailET;
    public EditText mPasswordET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //initialise Firebase variables
        mAuth = FirebaseAuth.getInstance();

        //initialise views
        mEmailET = findViewById(R.id.login_email_edit_text);
        mPasswordET = findViewById(R.id.login_password_edit_text);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if(firebaseUser!=null){
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    public void signUpNewUser(View view){
        final View finalView = view;
        String email = mEmailET.getText().toString();
        String password = mPasswordET.getText().toString();
        if(email.length()<1 || password.length()<1){
            Snackbar.make(view, "Please fill all the details to proceed", Snackbar.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Snackbar.make(finalView, "Could not sign you up. Try again later.", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void signIn(View view){
        final View finalView = view;
        String email = mEmailET.getText().toString();
        String password = mPasswordET.getText().toString();
        if(email.length()<1 || password.length()<1){
            Snackbar.make(view, "Please fill all the details to proceed", Snackbar.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Snackbar.make(finalView, "Could not sign you in. Try again later.", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
