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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaos.view.PinView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private TextView otp_title,otp_code_to,otp_didnt_recieve,otp_second_title,
                     otp_tip;

    // OTP and Timer variables
    private String generatedOTP = "";
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
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
            Toast.makeText(this, "Session expired. Please enter your phone number again.", Toast.LENGTH_LONG).show();
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

        // IMPORTANT: Make sure PinView is focusable and clickable
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
        languageSwitch=findViewById(R.id.switchlanguage);

        otp_title=findViewById(R.id.textView5);
        otp_code_to=findViewById(R.id.text);
        otp_didnt_recieve=findViewById(R.id.textdidnt);
        otp_tip=findViewById(R.id.textView4);



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
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Continue button - ONLY verification happens here now
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyOtp();
            }
        });

        // Resend button
        resendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = SharedData.getCurrentPhoneNumber();
                resendOtp(phoneNumber);
            }
        });

        // Optional: Request focus for PinView
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
                Toast.makeText(this, "SMS permission is required to send verification code",
                        Toast.LENGTH_LONG).show();
                // You could still let them test by showing the OTP in a toast
                if (isEmulator()) {
                    generatedOTP = generateOtp();
                    Toast.makeText(this, "TEST MODE - OTP: " + generatedOTP,
                            Toast.LENGTH_LONG).show();
                    startCountdownTimer();
                }
            }
        }
    }

    private void sendOtpViaSms(String phoneNumber) {
        // Generate 4-digit OTP
        generatedOTP = generateOtp();

        String message = "Your RentaNBO verification code is: " + generatedOTP;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            Toast.makeText(this, "Verification code sent to " + phoneNumber,
                    Toast.LENGTH_SHORT).show();

            // Start the countdown timer
            startCountdownTimer();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();

            // For testing purposes, if SMS fails (emulator), show the OTP
            if (isEmulator()) {
                Toast.makeText(this, "TEST MODE - OTP: " + generatedOTP,
                        Toast.LENGTH_LONG).show();
                startCountdownTimer();
            }
        }
    }

    private void resendOtp(String phoneNumber) {
        // Cancel existing timer if running
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Hide resend button, show timer
        resendButton.setVisibility(View.GONE);
        if (timerLayout != null) {
            timerLayout.setVisibility(View.VISIBLE);
        }

        // Clear PinView
        otpPinView.setText("");

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
                // Timer finished - show resend button, hide timer
                secondCountEditText.setText("0");
                if (timerLayout != null) {
                    timerLayout.setVisibility(View.GONE);
                }
                resendButton.setVisibility(View.VISIBLE);
            }
        }.start();

        isTimerRunning = true;
    }

    private void verifyOtp() {
        String enteredOtp = otpPinView.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter the verification code",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredOtp.length() < 4) {
            Toast.makeText(this, "Please enter complete 4-digit code",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Compare entered OTP with generated OTP
        if (enteredOtp.equals(generatedOTP)) {
            // OTP is correct
            Toast.makeText(this, "Verification successful!", Toast.LENGTH_SHORT).show();

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
            Toast.makeText(this, "Invalid verification code. Please try again.",
                    Toast.LENGTH_SHORT).show();

            // Clear PinView
            otpPinView.setText("");

            // Optionally request focus again
            otpPinView.requestFocus();
        }
    }

    /**
     * Check if this phone number already has a profile
     */
    private void checkIfUserExists(String phoneNumber) {
        // Clean phone number for database key
        String cleanPhone = phoneNumber.replace("+", "");

        // Show loading
        continueButton.setEnabled(false);
        continueButton.setText("Checking...");

        databaseReference.child("users").child(cleanPhone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        continueButton.setEnabled(true);
                        continueButton.setText("Continue");

                        if (snapshot.exists()) {
                            // RETURNING USER - profile exists
                            Toast.makeText(Otp.this, "Welcome back!", Toast.LENGTH_SHORT).show();

                            // Get existing user data
                            String name = snapshot.child("name").getValue(String.class);
                            String uid = snapshot.child("uid").getValue(String.class);

                            // Handle anonymous linking (more on this in Filters)
                            // For now, just go to Main App


                            Intent intent = new Intent(Otp.this, HomePage.class);
                            intent.putExtra("isReturningUser", true);
                            intent.putExtra("phoneNumber", phoneNumber);
                            intent.putExtra("name", name);
                            startActivity(intent);
                            finish();

                        } else {
                            // NEW USER - needs to complete profile
                            Toast.makeText(Otp.this, "Please complete your profile", Toast.LENGTH_SHORT).show();

                            // Go to Filters page
                            Intent intent = new Intent(Otp.this, Filters.class);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        continueButton.setEnabled(true);
                        continueButton.setText("Continue");
                        Toast.makeText(Otp.this, "Database error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();

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