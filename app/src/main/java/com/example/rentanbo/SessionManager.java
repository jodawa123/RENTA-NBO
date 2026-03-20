package com.example.rentanbo;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager handles persistent user authentication and session storage.
 * Once a user verifies their phone and completes their profile, their session is saved.
 * On subsequent app launches, users are automatically logged in if their session is valid.
 */
public class SessionManager {

    private static final String PREF_NAME = "RentaNBO_Session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PREFERRED_NEIGHBORHOOD = "preferred_neighborhood";
    private static final String KEY_BUDGET_MIN = "budget_min";
    private static final String KEY_BUDGET_MAX = "budget_max";
    private static final String KEY_LOGIN_TIME = "login_time";

    private static SessionManager instance;
    private final SharedPreferences sharedPreferences;

    private SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get singleton instance of SessionManager
     */
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * Save user session after successful verification and profile creation
     */
    public void saveSession(String phoneNumber, String userName, String userId,
                           String neighborhood, int budgetMin, int budgetMax) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_PHONE_NUMBER, phoneNumber);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_PREFERRED_NEIGHBORHOOD, neighborhood);
        editor.putInt(KEY_BUDGET_MIN, budgetMin);
        editor.putInt(KEY_BUDGET_MAX, budgetMax);
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Check if user has a valid active session
     */
    public boolean isUserLoggedIn() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        if (!isLoggedIn) {
            return false;
        }

        String phone = getPhoneNumber();
        return phone != null && !phone.trim().isEmpty();
    }

    /**
     * Get saved phone number
     */
    public String getPhoneNumber() {
        return sharedPreferences.getString(KEY_PHONE_NUMBER, "");
    }

    /**
     * Get saved user name
     */
    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    /**
     * Get saved user ID
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    /**
     * Get preferred neighborhood
     */
    public String getPreferredNeighborhood() {
        return sharedPreferences.getString(KEY_PREFERRED_NEIGHBORHOOD, "");
    }

    /**
     * Get budget min
     */
    public int getBudgetMin() {
        return sharedPreferences.getInt(KEY_BUDGET_MIN, 10000);
    }

    /**
     * Get budget max
     */
    public int getBudgetMax() {
        return sharedPreferences.getInt(KEY_BUDGET_MAX, 50000);
    }

    /**
     * Clear user session (logout)
     */
    public void clearSession() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Update login timestamp to extend session
     */
    public void updateSessionTimestamp() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();
    }
}

