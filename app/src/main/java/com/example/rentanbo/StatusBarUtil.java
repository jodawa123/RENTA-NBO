package com.example.rentanbo;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import androidx.core.content.ContextCompat;

public class StatusBarUtil {

    public static void setLightStatusBar(Activity activity) {
        // Make status bar icons dark (for API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        // Set status bar color to white (for API 21+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(
                    ContextCompat.getColor(activity, android.R.color.white)
            );
        }
    }
}
