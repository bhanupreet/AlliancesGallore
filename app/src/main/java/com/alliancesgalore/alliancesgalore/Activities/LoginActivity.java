package com.alliancesgalore.alliancesgalore.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.alliancesgalore.alliancesgalore.R;
import com.alliancesgalore.alliancesgalore.Utils.AESUtils;
import com.alliancesgalore.alliancesgalore.Utils.Functions;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout mLoginEmail;
    private TextInputLayout mLoginPassword;
    private FirebaseAuth mAuth;
    private Button mLoginBtn;
    private String email, password;
    private Button mForgotPasswordbtn;
    private Toolbar mLoginToolbar;
    private CredentialsClient mCredentialsClient;
    private ProgressBar mprogressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FindIds();
        SetmToolbar();
        credentialsclient();
        LoginClick();
        ForgotPasswordClick();
    }

    private void FindIds() {
        mForgotPasswordbtn = findViewById(R.id.forgotpasswordbtn);
        mLoginToolbar = findViewById(R.id.login_toolbar);
        mLoginBtn = findViewById(R.id.login_login_btn);
        mprogressBar = findViewById(R.id.login_progress);
        mLoginEmail = findViewById(R.id.login_email);
        mLoginPassword = findViewById(R.id.login_password);
    }

    private void SetmToolbar() {
        setSupportActionBar(mLoginToolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void credentialsclient() {
        mCredentialsClient = Credentials.getClient(this);
    }

    private void LoginClick() {
        mLoginBtn.setOnClickListener(loginclicklistener);
    }

    private void ForgotPasswordClick() {
        mForgotPasswordbtn.setOnClickListener(ForgotOnClickListener);
    }

    private void LogIn(final String email, final String password) {
        mAuth = FirebaseAuth.getInstance();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, LoginOnComplete);
    }


    private OnCompleteListener LoginOnComplete = new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
                FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(tokenOnComplete);
            } else {
                mprogressBar.setVisibility(View.GONE);
                Functions.toast(task);
            }
        }
    };


    private OnSuccessListener successListener = new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
            String encrypted = Functions.encrypt(password);
            mainIntent.putExtra("email", email);
            mainIntent.putExtra("password", encrypted);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainIntent);
            finish();
        }
    };

    private OnCompleteListener tokenOnComplete = (OnCompleteListener<InstanceIdResult>) task -> {
        if (!task.isSuccessful()) {
            Functions.toast(task);
        } else {
            // Get new Instance ID token
            String token = Objects.requireNonNull(task.getResult()).getToken();
            FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseDatabase.getInstance().getReference()
                    .child("Users")
                    .child(current_user.getUid())
                    .child("TokenID")
                    .setValue(token)
                    .addOnSuccessListener(successListener);
        }
    };

    private View.OnClickListener loginclicklistener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            email = Functions.TextOf(mLoginEmail);
            password = Functions.TextOf(mLoginPassword);
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Email or Password cannot be left blank", Toast.LENGTH_SHORT).show();
            } else {
                mprogressBar.setVisibility(View.VISIBLE);
                Credential credential = new Credential.Builder(email).setPassword(password).build();
                mCredentialsClient.save(credential);
                LogIn(email, password);
            }
        }
    };
    private View.OnClickListener ForgotOnClickListener = v -> {
        Intent PasswordIntent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(PasswordIntent);
    };

}