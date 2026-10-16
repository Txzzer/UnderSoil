package com.example.undersoil.auth;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.undersoil.R;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.db.UserDatabaseHelper;
import com.example.undersoil.data.model.User;
import com.example.undersoil.home.HomeScreenActivity;
import com.example.undersoil.network.ApiClient;
import com.example.undersoil.network.ApiService;
import com.example.undersoil.network.AuthResponse;
import com.example.undersoil.network.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLoginPrompt;
    private ImageButton backButton;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        etName = findViewById(R.id.nameInput);
        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        etConfirmPassword = findViewById(R.id.confirmPasswordInput);
        btnSignUp = findViewById(R.id.signUpButton);
        tvLoginPrompt = findViewById(R.id.tvLoginPrompt);
    }

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        btnSignUp.setOnClickListener(v -> handleRegistration());

        if (tvLoginPrompt != null) {
            tvLoginPrompt.setOnClickListener(v -> finish());
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void handleRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 4) {
            etPassword.setError("Password should be at least 4 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // Accounts are created on the server, so sign-up cannot work offline
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "You need an internet connection to create an account", Toast.LENGTH_LONG).show();
            return;
        }

        btnSignUp.setEnabled(false);
        btnSignUp.setText("Creating Account...");

        RegisterRequest request = new RegisterRequest(name, email, password);
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        apiService.registerUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnSignUp.setEnabled(true);
                btnSignUp.setText("Sign Up");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authData = response.body();

                    sessionManager.createLoginSession(
                            authData.getUserId(),
                            authData.getName(),
                            email,
                            "Farmer",
                            authData.getToken(),
                            authData.getExpiresAt()
                    );

                    User localUser = new User(authData.getUserId(), authData.getName(), email, "", "Farmer", true);
                    UserDatabaseHelper.getInstance(SignUpActivity.this).registerUser(localUser);

                    Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(SignUpActivity.this, HomeScreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else if (response.code() == 409) {
                    etEmail.setError("An account with this email already exists");
                    etEmail.requestFocus();
                } else {
                    Toast.makeText(SignUpActivity.this, "Registration failed (" + response.code() + "). Please try again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnSignUp.setEnabled(true);
                btnSignUp.setText("Sign Up");
                Toast.makeText(SignUpActivity.this, "No connection. Please try again when online.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
