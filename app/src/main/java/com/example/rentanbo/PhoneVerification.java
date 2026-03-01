// PhoneVerification.java
package com.example.rentanbo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashMap;
import java.util.Map;

public class PhoneVerification extends AppCompatActivity {

    private EditText phoneEditText;
    private Button sendNumberButton;
    private ImageView backButton;
    private SwitchMaterial languageSwitch;
    private TextView verification_title,verification_sub_title,verification_number_title,
                      verification_tip;

    private TranslationHelper translationHelper;
    private boolean isSwahiliMode = false;
    private Map<Integer, String> originalTexts = new HashMap<>();
    private Map<Integer, String> swahiliTexts = new HashMap<>(); // Cache translations

    private static final int VIEW_TITLE = 1001;
    private static final int VIEW_SUBTITLE = 1002;
    private static final int VIEW_NUMBER_TITLE = 1003;
    private static final int VIEW_TIP = 1004;
    private static final int VIEW_BUTTON = 1005;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setLightStatusBar(this);
        setContentView(R.layout.activity_phone_verification);



        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        setTitle(" ");

        translationHelper = new TranslationHelper(this);
        initViews();
        storeOriginalTexts();
        setupLanguageSwitch();

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
    private void initViews() {
        // Initialize views
        phoneEditText = findViewById(R.id.phoneNumber);
        sendNumberButton = findViewById(R.id.sendNumber);
        backButton=findViewById(R.id.backButton);

        verification_title=findViewById(R.id.textView3);
        verification_sub_title=findViewById(R.id.textView);
        verification_number_title=findViewById(R.id.textView2);
        verification_tip=findViewById(R.id.textView4);
        languageSwitch=findViewById(R.id.switchlanguage);

        languageSwitch.setText(getString(R.string.switch_language_en));
    }
    private void storeOriginalTexts() {
        originalTexts.put(VIEW_TITLE,getString(R.string.phone_verification_title));
        originalTexts.put(VIEW_SUBTITLE,getString(R.string.phone_verification_sub_title));
        originalTexts.put(VIEW_NUMBER_TITLE,getString(R.string.phone_verification_number));
        originalTexts.put(VIEW_TIP,getString(R.string.verification_tip));
        originalTexts.put(VIEW_BUTTON,getString(R.string.send_number_button));


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
    private void setupLanguageSwitch() {
        languageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isSwahiliMode = true;
                    languageSwitch.setText(R.string.switch_language_sw);

                    // Check if we already have cached translations
                    if (swahiliTexts.isEmpty()) {
                        translateAllTexts();
                    } else {
                        applyCachedTranslations();
                    }
                } else {
                    isSwahiliMode = false;
                    languageSwitch.setText(R.string.switch_language_en);
                    restoreEnglishTexts();
                }
            }
        });
    }
    private void translateAllTexts() {
        Toast.makeText(this, "Translating to Swahili...", Toast.LENGTH_SHORT).show();

        // Translate subtitle (this will definitely change)
        translationHelper.translateText(originalTexts.get(VIEW_TITLE), VIEW_TITLE,
                new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText, int viewId) {
                        runOnUiThread(() -> {
                            verification_title.setText(translatedText);
                            swahiliTexts.put(viewId, translatedText);
                        });
                    }

                    @Override
                    public void onError(String error, int viewId) {
                        Log.e("Translation", "Subtitle error: " + error);
                    }
                });

        // Translate button
        translationHelper.translateText(originalTexts.get(VIEW_SUBTITLE), VIEW_SUBTITLE,
                new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText, int viewId) {
                        runOnUiThread(() -> {
                            verification_sub_title.setText(translatedText);
                            swahiliTexts.put(viewId, translatedText);
                        });
                    }

                    @Override
                    public void onError(String error, int viewId) {
                        Log.e("Translation", "Button error: " + error);
                    }
                });

        // Translate brand names (they might not change)
        translationHelper.translateText(originalTexts.get(VIEW_NUMBER_TITLE), VIEW_NUMBER_TITLE,
                new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText, int viewId) {
                        runOnUiThread(() -> {
                            verification_number_title.setText(translatedText);
                            swahiliTexts.put(viewId, translatedText);
                        });
                    }

                    @Override
                    public void onError(String error, int viewId) {
                        Log.e("Translation", "Brand1 error: " + error);
                    }
                });

        translationHelper.translateText(originalTexts.get(VIEW_TIP), VIEW_TIP,
                new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText, int viewId) {
                        runOnUiThread(() -> {
                            verification_tip.setText(translatedText);
                            swahiliTexts.put(viewId, translatedText);
                        });
                    }

                    @Override
                    public void onError(String error, int viewId) {
                        Log.e("Translation", "Brand2 error: " + error);
                    }
                });

        translationHelper.translateText(originalTexts.get(VIEW_BUTTON), VIEW_BUTTON,
                new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText, int viewId) {
                        runOnUiThread(() -> {
                            sendNumberButton.setText(translatedText);
                            swahiliTexts.put(viewId, translatedText);
                        });
                    }

                    @Override
                    public void onError(String error, int viewId) {
                        Log.e("Translation", "Brand2 error: " + error);
                    }
                });
    }

    private void applyCachedTranslations() {
        verification_title.setText(swahiliTexts.get(VIEW_TITLE));
        verification_sub_title.setText(swahiliTexts.get(VIEW_SUBTITLE));
        verification_number_title.setText(swahiliTexts.get(VIEW_NUMBER_TITLE));
        verification_tip.setText(swahiliTexts.get(VIEW_NUMBER_TITLE));
        sendNumberButton.setText(swahiliTexts.get(VIEW_BUTTON));

    }

    private void restoreEnglishTexts() {

        verification_title.setText(originalTexts.get(VIEW_TITLE));
        verification_sub_title.setText(originalTexts.get(VIEW_SUBTITLE));
        verification_number_title.setText(originalTexts.get(VIEW_NUMBER_TITLE));
        verification_tip.setText(originalTexts.get(VIEW_NUMBER_TITLE));
        sendNumberButton.setText(originalTexts.get(VIEW_BUTTON));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translationHelper != null) {
            translationHelper.closeTranslator();
        }
    }
}