package com.example.undersoil.auth;

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

import com.example.undersoil.R;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.db.UserDatabaseHelper;
import com.example.undersoil.data.model.User;
import com.example.undersoil.data.sync.SyncManager;
import com.example.undersoil.home.HomeScreenActivity;
import com.example.undersoil.network.ApiClient;
import com.example.undersoil.network.ApiService;
import com.example.undersoil.network.AuthResponse;
import com.example.undersoil.network.LoginRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

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
            startActivity(new Intent(LoginActivity.this, HomeScreenActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_log_in);

        SessionManager.InvalidReason invalidReason = sessionManager.getLastInvalidReason();
        boolean sessionExpiredExtra = getIntent().getBooleanExtra("session_expired", false);
        if (sessionExpiredExtra || invalidReason == SessionManager.InvalidReason.EXPIRED || invalidReason == SessionManager.InvalidReason.CLOCK_ROLLBACK) {
            Toast.makeText(this, "Your session has expired. Connect to the internet and log in again.", Toast.LENGTH_LONG).show();
            sessionManager.setLastInvalidReason(SessionManager.InvalidReason.NONE);
        }

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
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
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

        btnSignIn.setEnabled(false);
        btnSignIn.setText("Logging in...");

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        LoginRequest request = new LoginRequest(email, password);

        apiService.loginUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText("Sign In");

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
                    UserDatabaseHelper.getInstance(LoginActivity.this).registerUser(localUser);

                    // Push anything recorded offline before this login
                    SyncManager.scheduleSync(LoginActivity.this);

                    Toast.makeText(LoginActivity.this, "Welcome back, " + authData.getName() + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, HomeScreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid email or password.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText("Sign In");
                Toast.makeText(LoginActivity.this, "No connection. Please check your internet and try again.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
