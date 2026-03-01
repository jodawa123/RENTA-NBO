package com.example.rentanbo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaos.view.PinView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class Otp extends BaseActivity {

    // UI Components
    private ImageButton backButton;
    private TextView phoneDisplayTextView;
    private PinView otpPinView;
    private TextView timerText;
    private EditText secondCountEditText;
    private Button resendButton;
    private Button continueButton;
    private LinearLayout timerLayout;
    private SwitchMaterial languageSwitch;
    private TextView otp_title, otp_code_to, otp_didnt_recieve, otp_second_title, otp_tip;

    // OTP and Timer variables
    private String generatedOTP = "";
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isOtpExpired = false;
    private boolean isVerifying = false;
    private final int TOTAL_TIME = 30000; // 30 seconds in milliseconds
    private final int INTERVAL = 1000; // 1 second intervals

    // Permission request code
    private static final int SMS_PERMISSION_CODE = 123;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setLightStatusBar(this);
        setContentView(R.layout.activity_otp);

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
        registerAllViewsForTranslation();

        // Get phone number from SharedData
        String phoneNumber = SharedData.getCurrentPhoneNumber();

        // Check if we have a phone number
        if (!SharedData.hasPhoneNumber()) {
            showToast(R.string.error_session_expired);
            startActivity(new Intent(Otp.this, PhoneVerification.class));
            finish();
            return;
        }

        // Display the phone number
        phoneDisplayTextView.setText(phoneNumber);

        // Set up click listeners
        setupClickListeners();

        // Check SMS permission and send OTP
        checkSmsPermissionAndSendOtp(phoneNumber);

        // Make sure PinView is focusable and clickable
        otpPinView.setFocusable(true);
        otpPinView.setFocusableInTouchMode(true);
        otpPinView.setClickable(true);
    }

    private void initViews() {
        backButton = findViewById(R.id.imageButton2);
        phoneDisplayTextView = findViewById(R.id.phoneDisplay);
        otpPinView = findViewById(R.id.otpbox);
        timerText = findViewById(R.id.secondtext);
        secondCountEditText = findViewById(R.id.secondCount);
        resendButton = findViewById(R.id.buttonResend);
        continueButton = findViewById(R.id.sendNumber);
        languageSwitch = findViewById(R.id.switchlanguage);

        otp_title = findViewById(R.id.textView5);
        otp_code_to = findViewById(R.id.text);
        otp_didnt_recieve = findViewById(R.id.textdidnt);
        otp_tip = findViewById(R.id.textView4);

        setupLanguageSwitch(languageSwitch);

        // Find the timer layout (LinearLayout containing secondCount)
        if (secondCountEditText.getParent() instanceof LinearLayout) {
            timerLayout = (LinearLayout) secondCountEditText.getParent();
        }

        // Initially hide resend button and show timer
        resendButton.setVisibility(View.GONE);
        secondCountEditText.setEnabled(false); // Make it read-only
        secondCountEditText.setFocusable(false);

        // Enable PinView for input
        otpPinView.setEnabled(true);

        // Ensure continue button is enabled initially
        continueButton.setEnabled(true);
        continueButton.setClickable(true);
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            onBackPressed();
        });

        // Continue button - with proper state management
        continueButton.setOnClickListener(v -> {
            // Prevent multiple clicks
            if (isVerifying) {
                return;
            }

            // Check if OTP is expired
            if (isOtpExpired) {
                showToast("Verification code expired. Please request a new one.");
                // Highlight resend button
                resendButton.requestFocus();
                return;
            }

            verifyOtp();
        });

        // Resend button - always enabled when visible
        resendButton.setOnClickListener(v -> {
            String phoneNumber = SharedData.getCurrentPhoneNumber();
            resendOtp(phoneNumber);
        });

        // Request focus for PinView
        otpPinView.requestFocus();
    }

    private void checkSmsPermissionAndSendOtp(String phoneNumber) {
        // Check if we have SMS permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, send OTP
            sendOtpViaSms(phoneNumber);
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, send OTP
                String phoneNumber = SharedData.getCurrentPhoneNumber();
                sendOtpViaSms(phoneNumber);
            } else {
                // Permission denied
                showToast("SMS permission is required to send verification code");

                // For emulator testing, show OTP
                if (isEmulator()) {
                    generatedOTP = generateOtp();
                    showToast("TEST MODE - OTP: " + generatedOTP);
                    startCountdownTimer();
                } else {
                    // If permission denied on real device, show resend button as fallback
                    resendButton.setVisibility(View.VISIBLE);
                    if (timerLayout != null) {
                        timerLayout.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private void sendOtpViaSms(String phoneNumber) {
        // Reset expired flag
        isOtpExpired = false;
        isVerifying = false;

        // Generate 4-digit OTP
        generatedOTP = generateOtp();

        String message = "Your RentaNBO verification code is: " + generatedOTP;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            showToast(R.string.otp_sent);

            // Start the countdown timer
            startCountdownTimer();

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Failed to send SMS: " + e.getMessage());

            // For testing purposes, if SMS fails (emulator), show the OTP
            if (isEmulator()) {
                showToast("TEST MODE - OTP: " + generatedOTP);
                startCountdownTimer();
            } else {
                // If SMS fails on real device, show resend button
                resendButton.setVisibility(View.VISIBLE);
                if (timerLayout != null) {
                    timerLayout.setVisibility(View.GONE);
                }
            }
        }
    }

    private void resendOtp(String phoneNumber) {
        // Cancel existing timer if running
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Reset flags
        isOtpExpired = false;
        isVerifying = false;

        // Hide resend button, show timer
        resendButton.setVisibility(View.GONE);
        if (timerLayout != null) {
            timerLayout.setVisibility(View.VISIBLE);
        }

        // Clear PinView
        otpPinView.setText("");

        // Re-enable continue button
        continueButton.setEnabled(true);
        continueButton.setText(R.string.continue_button);

        // Send OTP again
        sendOtpViaSms(phoneNumber);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 1000 + random.nextInt(9000); // Generates 1000-9999
        return String.valueOf(otp);
    }

    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(TOTAL_TIME, INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                secondCountEditText.setText(String.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                // Timer finished - OTP expired
                secondCountEditText.setText("0");
                isOtpExpired = true;
                isVerifying = false;

                // Show resend button, hide timer
                if (timerLayout != null) {
                    timerLayout.setVisibility(View.GONE);
                }
                resendButton.setVisibility(View.VISIBLE);

                // Show toast notification that OTP expired
                showToast("Verification code expired. Please request a new one.");

                // Clear the PinView
                otpPinView.setText("");

                // Keep continue button enabled but it will show expiration message
                continueButton.setEnabled(true);
            }
        }.start();

        isTimerRunning = true;
    }

    private void verifyOtp() {
        // Prevent multiple verification attempts
        if (isVerifying) {
            return;
        }

        // Double-check expiration
        if (isOtpExpired) {
            showToast("Verification code expired. Please request a new one.");
            return;
        }

        String enteredOtp = otpPinView.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            showToast(R.string.error_enter_otp);
            return;
        }

        if (enteredOtp.length() < 4) {
            showToast("Please enter complete 4-digit code");
            return;
        }

        // Set verifying flag to prevent multiple clicks
        isVerifying = true;

        // Show loading state
        continueButton.setEnabled(false);
        continueButton.setText(R.string.checking);

        // Compare entered OTP with generated OTP
        if (enteredOtp.equals(generatedOTP)) {
            // OTP is correct
            showToast(R.string.verification_success);

            // Cancel timer
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            // Get phone number
            String phoneNumber = SharedData.getCurrentPhoneNumber();

            // Check if user exists in database
            checkIfUserExists(phoneNumber);

        } else {
            // Wrong OTP
            showToast(R.string.error_invalid_otp);

            // Clear PinView
            otpPinView.setText("");

            // Reset button state
            continueButton.setEnabled(true);
            continueButton.setText(R.string.continue_button);
            isVerifying = false;

            // Request focus again
            otpPinView.requestFocus();
        }
    }

    /**
     * Check if this phone number already has a profile
     */
    private void checkIfUserExists(String phoneNumber) {
        // Clean phone number for database key
        String cleanPhone = phoneNumber.replace("+", "");

        databaseReference.child("users").child(cleanPhone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // RETURNING USER - profile exists
                            showToast(R.string.welcome_back_toast);

                            // Get existing user data
                            String name = snapshot.child("name").getValue(String.class);

                            Intent intent = new Intent(Otp.this, HomePage.class);
                            intent.putExtra("isReturningUser", true);
                            intent.putExtra("phoneNumber", phoneNumber);
                            intent.putExtra("name", name);
                            startActivity(intent);
                            finish();

                        } else {
                            // NEW USER - needs to complete profile
                            showToast(R.string.please_complete_profile);

                            // Go to Filters page
                            Intent intent = new Intent(Otp.this, Filters.class);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Reset button state
                        continueButton.setEnabled(true);
                        continueButton.setText(R.string.continue_button);
                        isVerifying = false;

                        showToast("Database error: " + error.getMessage());

                        // On error, still go to Filters as fallback
                        startActivity(new Intent(Otp.this, Filters.class));
                        finish();
                    }
                });
    }

    private boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void showToast(int stringResId) {
        Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel timer to prevent memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void registerAllViewsForTranslation() {
        registerForTranslation(otp_title, R.string.enter_verification_code);
        registerForTranslation(otp_code_to, R.string.code_sent_to);
        registerForTranslation(otp_didnt_recieve, R.string.didnt_receive_code);
        registerForTranslation(timerText, R.string.seconds_remaining);
        registerForTranslation(otp_tip, R.string.paste_hint);
        registerForTranslation(resendButton, R.string.resend);
        registerForTranslation(continueButton, R.string.continue_button);
    }
}