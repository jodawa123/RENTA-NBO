package com.example.rentanbo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    protected TranslationManager translationManager;
    protected SwitchMaterial languageSwitch;
    private SharedPreferences prefs;
    private static boolean isLanguageChanging = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Load saved language preference
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);

        translationManager = TranslationManager.getInstance(this);
    }

    protected void setupLanguageSwitch(SwitchMaterial switchView) {
        this.languageSwitch = switchView;

        if (languageSwitch == null) return;

        // Set initial state
        boolean isSwahili = translationManager.isSwahiliMode();
        languageSwitch.setChecked(isSwahili);
        updateSwitchText(isSwahili);

        languageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isLanguageChanging) return;

                isLanguageChanging = true;

                // Update switch text immediately
                updateSwitchText(isChecked);

                // Perform translation
                if (isChecked) {
                    translationManager.switchToSwahili(BaseActivity.this);
                } else {
                    translationManager.switchToEnglish(BaseActivity.this);
                }

                // Show toast
                String message = isChecked ?
                        "Language switched to Swahili" :
                        "Language switched to English";
                Toast.makeText(BaseActivity.this, message, Toast.LENGTH_SHORT).show();

                isLanguageChanging = false;
            }
        });
    }

    private void updateSwitchText(boolean isSwahili) {
        if (languageSwitch != null) {
            languageSwitch.setText(isSwahili ?
                    R.string.switch_language_sw :
                    R.string.switch_language_en);
        }
    }

    protected void registerForTranslation(View view, int stringResId) {
        if (translationManager != null && view != null) {
            translationManager.registerView(this, view, stringResId);
        }
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void showToast(@StringRes int stringResId) {
        Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translationManager != null) {
            translationManager.clearActivity(this);
        }
    }
}