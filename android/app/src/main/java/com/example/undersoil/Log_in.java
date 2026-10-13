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

import com.example.undersoil.data.SessionManager;

// Import our new network files
import network.ApiClient;
import network.ApiService;
import network.AuthResponse;
import network.LoginRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Log_in extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ImageView ivTogglePassword;
    private Button btnSignIn;
    private TextView tvSignUpPrompt;

    private boolean isPasswordVisible = false;
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

        // Disable button so they don't spam click it while loading
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Logging in...");

        // 1. Create the API Service
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // 2. Package the email and password into the request model
        LoginRequest request = new LoginRequest(email, password);

        // 3. Send the request to the C# Backend
        apiService.loginUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText("Sign In");

                if (response.isSuccessful() && response.body() != null) {
                    // SUCCESS! The backend gave us a JWT Token & User GUID.
                    AuthResponse authData = response.body();

                    // Save session with String GUID userId and JWT Token
                    sessionManager.createLoginSession(authData.getUserId(), authData.getName(), email, "Farmer", authData.getToken());

                    // Persist user record locally in SQLite for offline login
                    com.example.undersoil.data.User localUser = new com.example.undersoil.data.User(authData.getUserId(), authData.getName(), email, "", "Farmer", true);
                    com.example.undersoil.data.UserDatabaseHelper.getInstance(Log_in.this).registerUser(localUser);

                    Toast.makeText(Log_in.this, "Welcome back, " + authData.getName() + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Log_in.this, HomeScreenActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Backend responded, but passwords didn't match (401 Unauthorized)
                    Toast.makeText(Log_in.this, "Invalid email or password.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // The app couldn't reach the backend at all (server offline, bad IP, etc)
                btnSignIn.setEnabled(true);
                btnSignIn.setText("Sign In");
                Toast.makeText(Log_in.this, "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}