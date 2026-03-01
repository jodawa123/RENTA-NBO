package com.example.rentanbo;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends BaseActivity {

    private TextView textBrandName;
    private TextView textBrandName2;
    private TextView textSubtitle;
    private Button buttonStart;
    private ImageView imageBrand;

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
        setTitle(" ");

        initViews();
        setupAnimations();
        registerAllViewsForTranslation();

        buttonStart.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PhoneVerification.class))
        );
    }

    private void initViews() {
        SwitchMaterial languageSwitch = findViewById(R.id.switchlanguage);
        imageBrand = findViewById(R.id.imageBrand);
        textBrandName = findViewById(R.id.textBrandName);
        textBrandName2 = findViewById(R.id.textBrandName2);
        textSubtitle = findViewById(R.id.textSubtitle);
        buttonStart = findViewById(R.id.buttonStart);

        // Setup language switch (from BaseActivity)
        setupLanguageSwitch(languageSwitch);
    }

    private void setupAnimations() {
        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);

        textBrandName.setAnimation(slideDownAnimation);
        textBrandName2.setAnimation(slideDownAnimation);
        textSubtitle.setAnimation(slideDownAnimation);

        imageBrand.setAnimation(fadeInAnimation);
        buttonStart.setAnimation(fadeInAnimation);
    }

    private void registerAllViewsForTranslation() {
        // One line per view - that's it!
        registerForTranslation(textSubtitle, R.string.subtitle);
        registerForTranslation(buttonStart, R.string.get_started);
    }
}