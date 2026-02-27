// PhoneVerification.java
package com.example.rentanbo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PhoneVerification extends AppCompatActivity {

    private EditText phoneEditText;
    private Button sendNumberButton;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setLightStatusBar(this);
        setContentView(R.layout.activity_phone_verification);

        // Initialize views
        phoneEditText = findViewById(R.id.phoneNumber);
        sendNumberButton = findViewById(R.id.sendNumber);
        backButton=findViewById(R.id.backButton);

        // Clear any existing phone number when starting fresh
        SharedData.clearPhoneNumber();

        // Set click listener
        sendNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndSendPhoneNumber();
            }
        });

        // Back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void validateAndSendPhoneNumber() {
        String rawPhoneNumber = phoneEditText.getText().toString().trim();

        // Step 1: Check if empty
        if (TextUtils.isEmpty(rawPhoneNumber)) {
            showToast("Please enter your phone number");
            return;
        }

        // Step 2: Clean the number (remove spaces, dashes, etc.)
        String cleanedNumber = rawPhoneNumber.replaceAll("[^0-9]", "");

        // Step 3: Validate Kenyan phone number format
        String formattedNumber = formatKenyanNumber(cleanedNumber);

        if (formattedNumber == null) {
            showToast("Please enter a valid Kenyan phone number (e.g., 0712345678 or 712345678)");
            return;
        }

        // Step 4: Save to SharedData
        SharedData.setCurrentPhoneNumber(formattedNumber);

        // Step 5: Navigate to OTP page
        Intent intent = new Intent(PhoneVerification.this, Otp.class);
        startActivity(intent);

        // Optional: Finish this activity so user can't go back
        finish();
    }

    private String formatKenyanNumber(String cleanedNumber) {
        if (cleanedNumber == null) return null;

        // Case 1: 0712345678 (10 digits starting with 0) -> +254712345678
        if (cleanedNumber.length() == 10 && cleanedNumber.startsWith("0")) {
            return "+254" + cleanedNumber.substring(1);
        }
        // Case 2: 712345678 (9 digits, no leading zero) -> +254712345678
        else if (cleanedNumber.length() == 9) {
            return "+254" + cleanedNumber;
        }
        // Case 3: 254712345678 (12 digits, has country code but no plus) -> +254712345678
        else if (cleanedNumber.length() == 12 && cleanedNumber.startsWith("254")) {
            return "+" + cleanedNumber;
        }
        // Case 4: 712345678 (already handled above)
        // Case 5: Invalid format
        else {
            return null;
        }
    }

    private void showToast(String message) {
        Toast.makeText(PhoneVerification.this, message, Toast.LENGTH_SHORT).show();
    }
}