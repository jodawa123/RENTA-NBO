package com.example.rentanbo;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public abstract class BaseActivity extends AppCompatActivity {

    protected TranslationManager translationManager;
    protected SwitchMaterial languageSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        translationManager = TranslationManager.getInstance(this);
    }

    /**
     * Call this after setting content view to setup language switch
     */
    protected void setupLanguageSwitch(SwitchMaterial switchView) {
        this.languageSwitch = switchView;

        // Set initial state
        if (translationManager.isSwahiliMode()) {
            languageSwitch.setText(R.string.switch_language_sw);
            languageSwitch.setChecked(true);
        } else {
            languageSwitch.setText(R.string.switch_language_en);
            languageSwitch.setChecked(false);
        }

        languageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    languageSwitch.setText(R.string.switch_language_sw);
                    translationManager.switchToSwahili(BaseActivity.this);
                } else {
                    languageSwitch.setText(R.string.switch_language_en);
                    translationManager.switchToEnglish(BaseActivity.this);
                }
            }
        });
    }

    /**
     * Helper method to register a view for translation
     */
    protected void registerForTranslation(View view, int stringResId) {
        translationManager.registerView(this, view, stringResId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        translationManager.clearActivity(this);
    }
}