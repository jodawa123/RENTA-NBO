package com.example.rentanbo;

public class SharedData {
    private static String currentPhoneNumber = "";

    // Private constructor to prevent instantiation
    private SharedData() {}

    public static String getCurrentPhoneNumber() {
        return currentPhoneNumber;
    }

    public static void setCurrentPhoneNumber(String phoneNumber) {
        currentPhoneNumber = phoneNumber;
    }

    public static void clearPhoneNumber() {
        currentPhoneNumber = "";
    }

    public static boolean hasPhoneNumber() {
        return currentPhoneNumber != null && !currentPhoneNumber.isEmpty();
    }
}