package com.example.rentanbo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    //private static final int SPLASH_SCREEN_DELAY = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Ensure the title is not spoken by TalkBack
        setTitle(" ");

        // Only hide ActionBar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Load animations
        // UI Elements
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);

        // Initialize UI elements
        ImageView imageBrand = findViewById(R.id.imageBrand);
        TextView textBrandName = findViewById(R.id.textBrandName);
        TextView textSubtitle = findViewById(R.id.textSubtitle);
        Button buttonStart = findViewById(R.id.buttonStart);

        // Set animations to UI elements
        textBrandName.setAnimation(slideDownAnimation);
        textSubtitle.setAnimation(slideDownAnimation);

        imageBrand.setAnimation(fadeInAnimation);
        buttonStart.setAnimation(fadeInAnimation);


        buttonStart.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this,
                        PhoneVerification.class)));

    }



    }
