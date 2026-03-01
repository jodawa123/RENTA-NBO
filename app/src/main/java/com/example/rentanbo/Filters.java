package com.example.rentanbo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class Filters extends BaseActivity {

    // UI Components
    private ImageButton backButton;
    private EditText nameEditText;
    private Button rongaiButton;
    private Button kahawaButton;
    private Button langataButton;
    private EditText amountEditText;
    private SeekBar seekBar;
    private Button createProfileButton;
    private TextView minBudgetText;
    private TextView maxBudgetText;
    private SwitchMaterial languageSwitch;
    private TextView title,subtitle,name_text,neighbourhood_text,
                    budget_text,month_text;

    // Data variables
    private String selectedNeighborhood = "";
    private int budgetValue = 10; // Default 10k

    // Flag to prevent recursive updates
    private boolean isUpdating = false;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StatusBarUtil.setLightStatusBar(this);
        setContentView(R.layout.activity_filters);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initViews();

        // Setup all functionality
        setupNameField();
        setupNeighborhoodButtons();
        setupBudgetSeekBar();
        setupCreateProfileButton();
        setupBackButton();
        registerAllViewsForTranslation();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        nameEditText = findViewById(R.id.nameBox);
        rongaiButton = findViewById(R.id.rongaiBtn);
        kahawaButton = findViewById(R.id.kahawaBtn);
        langataButton = findViewById(R.id.langataBtn);
        amountEditText = findViewById(R.id.amount);
        seekBar = findViewById(R.id.seekBar);
        createProfileButton = findViewById(R.id.sendProfile);


        title=findViewById(R.id.textView3);
        subtitle=findViewById(R.id.textView);
        name_text=findViewById(R.id.textView9);
        neighbourhood_text=findViewById(R.id.textView2);
        budget_text=findViewById(R.id.textView8);
        month_text=findViewById(R.id.textView10);


        languageSwitch=findViewById(R.id.switchlanguage);
        setupLanguageSwitch(languageSwitch);

        // Find the min/max text views
        LinearLayout linear3 = findViewById(R.id.linear3);
        if (linear3 != null && linear3.getChildCount() >= 2) {
            minBudgetText = (TextView) linear3.getChildAt(0);
            maxBudgetText = (TextView) linear3.getChildAt(1);
        }

        // Set min/max text values
        if (minBudgetText != null) {
            minBudgetText.setText("10k");
        }
        if (maxBudgetText != null) {
            maxBudgetText.setText("50k");
        }

        // Default no neighborhood selected
        selectedNeighborhood = "";

        // DO NOT set progress or text here - let setupBudgetSeekBar handle it
    }

    /**
     * Handle name field - only show cursor when actively typing
     */
    private void setupNameField() {
        nameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                nameEditText.setCursorVisible(hasFocus);
            }
        });

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                nameEditText.setCursorVisible(true);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Handle neighborhood selection - only selected button turns yellow
     */
    private void setupNeighborhoodButtons() {
        rongaiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNeighborhood(rongaiButton, "Rongai");
            }
        });

        kahawaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNeighborhood(kahawaButton, "Kahawa");
            }
        });

        langataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNeighborhood(langataButton, "Langata");
            }
        });

        resetAllNeighborhoodButtons();
    }

    /**
     * Helper method to handle neighborhood selection with visual feedback
     */
    private void selectNeighborhood(Button selectedButton, String neighborhood) {
        resetAllNeighborhoodButtons();

        selectedButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F4B400") // Yellow
                )
        );

        selectedNeighborhood = neighborhood;
    }

    /**
     * Reset all neighborhood buttons to light grey
     */
    private void resetAllNeighborhoodButtons() {
        int lightGrey = android.graphics.Color.parseColor("#DEDBDB");

        rongaiButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(lightGrey)
        );
        kahawaButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(lightGrey)
        );
        langataButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(lightGrey)
        );
    }

    /**
     * Handle budget seekbar - sync with edittext WITHOUT recursive loops
     */
    private void setupBudgetSeekBar() {
        // Set seekbar range: 0 to 40 (representing 10 to 50)
        seekBar.setMax(40);

        // Set initial values
        budgetValue = 10;
        amountEditText.setText("10");

        // IMPORTANT: Set progress without triggering listener
        seekBar.setProgress(0);

        // Log initial state for debugging
        android.util.Log.d("Budget", "Initial - Progress: 0, Value: 10");

        // SeekBar change listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean isFirstTracking = true;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Log every progress change to see what's happening
                android.util.Log.d("Budget", "Progress: " + progress + ", fromUser: " + fromUser);

                if (fromUser) {
                    // Calculate actual budget: 10 + progress
                    int newValue = 10 + progress;

                    // Update budget value
                    budgetValue = newValue;

                    // Update EditText without triggering its listener
                    isUpdating = true;
                    amountEditText.setText(String.valueOf(newValue));
                    isUpdating = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // When user starts touching, ensure we have the correct progress
                int currentProgress = seekBar.getProgress();

                // Fix for the jump from 10 to 20 issue
                if (currentProgress == 0) {
                    // If at minimum, set to 1 (which represents 11) to avoid jump
                    // This helps when user first starts scrolling
                    budgetValue = 11;
                    isUpdating = true;
                    amountEditText.setText("11");
                    isUpdating = false;
                    seekBar.setProgress(1);
                }

                android.util.Log.d("Budget", "Start tracking - Progress: " + seekBar.getProgress());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                android.util.Log.d("Budget", "Stop tracking - Progress: " + seekBar.getProgress());
            }
        });

        // EditText change listener
        amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Skip if we're already updating from SeekBar
                if (isUpdating) return;

                try {
                    if (s.length() > 0) {
                        int value = Integer.parseInt(s.toString());

                        // Clamp value between 10 and 50
                        if (value < 10) {
                            value = 10;
                            amountEditText.setText(String.valueOf(value));
                            amountEditText.setSelection(amountEditText.getText().length());
                            return;
                        } else if (value > 50) {
                            value = 50;
                            amountEditText.setText(String.valueOf(value));
                            amountEditText.setSelection(amountEditText.getText().length());
                            return;
                        }

                        // Update budget value
                        budgetValue = value;

                        // Calculate progress (value - 10)
                        int newProgress = value - 10;

                        // Log the update
                        android.util.Log.d("Budget", "EditText update - Value: " + value + ", Progress: " + newProgress);

                        // Update SeekBar without triggering its listener
                        seekBar.setProgress(newProgress);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
            }
        });

    }
    /**
     * Setup back button
     */
    private void setupBackButton() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    /**
     * Create user profile in Firebase Database (NO EMAIL/PASSWORD AUTH)
     */
    private void setupCreateProfileButton() {
        createProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserProfile();
            }
        });
    }

    private void saveUserProfile() {
        // Get all the data
        String name = nameEditText.getText().toString().trim();
        String phoneNumber = SharedData.getCurrentPhoneNumber();

        // Validate inputs
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            nameEditText.requestFocus();
            return;
        }

        if (selectedNeighborhood.isEmpty()) {
            Toast.makeText(this, "Please select your preferred neighborhood", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!SharedData.hasPhoneNumber()) {
            Toast.makeText(this, "Session expired. Please restart.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Filters.this, PhoneVerification.class));
            finish();
            return;
        }

        // Show loading
        createProfileButton.setEnabled(false);
        createProfileButton.setText("Saving...");

        // Get current anonymous user or create one
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Sign in anonymously first
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Now save profile with the new UID
                            saveProfileToDatabase(mAuth.getCurrentUser().getUid(), name, phoneNumber);
                        } else {
                            createProfileButton.setEnabled(true);
                            createProfileButton.setText("Create Profile");
                            Toast.makeText(this, "Auth failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Already have anonymous user
            saveProfileToDatabase(currentUser.getUid(), name, phoneNumber);
        }
    }

    /**
     * Save profile to database using PHONE NUMBER as the KEY
     * This allows easy lookup when user returns
     */
    private void saveProfileToDatabase(String uid, String name, String phoneNumber) {
        // Clean phone number for use as database key (remove +)
        String phoneKey = phoneNumber.replace("+", "");

        // Create user profile map
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", uid);                    // Firebase Auth UID (anonymous)
        userProfile.put("name", name);                   // User's name
        userProfile.put("phoneNumber", phoneNumber);     // Original phone with +
        userProfile.put("preferredNeighborhood", selectedNeighborhood);
        userProfile.put("monthlyBudget", budgetValue * 1000); // Convert to actual KSh
        userProfile.put("createdAt", System.currentTimeMillis());
        userProfile.put("lastLogin", System.currentTimeMillis());

        // Also store the phoneKey for easy reference
        userProfile.put("phoneKey", phoneKey);

        // Save under phone number for easy future lookup
        databaseReference.child("users").child(phoneKey).setValue(userProfile)
                .addOnCompleteListener(task -> {
                    createProfileButton.setEnabled(true);
                    createProfileButton.setText("Create Profile");

                    if (task.isSuccessful()) {
                        Toast.makeText(Filters.this,
                                "Profile created successfully!", Toast.LENGTH_LONG).show();

                        // Also save a reference from UID to phone (optional, for reverse lookup)
                        databaseReference.child("userPhones").child(uid).setValue(phoneKey);

                        // Navigate to main app
                        Intent intent = new Intent(Filters.this, HomePage.class);
                        intent.putExtra("isNewUser", true);
                        intent.putExtra("name", name);
                        intent.putExtra("phoneNumber", phoneNumber);
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(Filters.this,
                                "Failed to save profile: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Optional: Logout method for testing
     * Add this to a settings menu or debug button
     */
    private void logout() {
        mAuth.signOut();
        SharedData.clearPhoneNumber();
        startActivity(new Intent(this, PhoneVerification.class));
        finish();
    }
    private void registerAllViewsForTranslation() {

        registerForTranslation(title, R.string.complete_profile);
        registerForTranslation(subtitle, R.string.help_text);
        registerForTranslation(name_text, R.string.full_name);
        registerForTranslation(neighbourhood_text, R.string.preferred_neighborhood);
        registerForTranslation(budget_text, R.string.monthly_budget);
        registerForTranslation(month_text, R.string.per_month);
        registerForTranslation(createProfileButton, R.string.create_profile);
    }
}