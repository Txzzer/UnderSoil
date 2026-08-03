package com.example.undersoil;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button testApiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testApiButton = findViewById(R.id.testApiButton);

        testApiButton.setOnClickListener(view -> {
            Toast.makeText(
                    MainActivity.this,
                    "Button works!",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}