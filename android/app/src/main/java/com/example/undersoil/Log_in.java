package com.example.undersoil;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.undersoil.data.SecurityUtils;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.User;
import com.example.undersoil.data.UserDatabaseHelper;

public class Log_in extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ImageView ivTogglePassword;
    private Button btnSignIn;
    private TextView tvSignUpPrompt;

    private boolean isPasswordVisible = false;
    private UserDatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(Log_in.this, HomeScreenActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_log_in);

        dbHelper = UserDatabaseHelper.getInstance(this);
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        btnSignIn = findViewById(R.id.signInButton);
        tvSignUpPrompt = findViewById(R.id.tvSignUpPrompt);
    }

    private void setupClickListeners() {
        if (ivTogglePassword != null) {
            ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        btnSignIn.setOnClickListener(v -> handleLogin());

        if (tvSignUpPrompt != null) {
            tvSignUpPrompt.setOnClickListener(v -> {
                Intent intent = new Intent(Log_in.this, Sign_up.class);
                startActivity(intent);
            });
        }
    }

    private void togglePasswordVisibility() {
        if (etPassword == null) return;
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            isPasswordVisible = false;
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            isPasswordVisible = true;
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        String passwordHash = SecurityUtils.hashPassword(password);
        User user = dbHelper.getAuthUser(email, passwordHash);

        if (user != null) {
            sessionManager.createLoginSession(user.getId(), user.getName(), user.getEmail(), user.getRole());
            Toast.makeText(this, "Welcome back, " + user.getName() + "!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(Log_in.this, HomeScreenActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Invalid email or password. Please try again.", Toast.LENGTH_LONG).show();
        }
    }
}
