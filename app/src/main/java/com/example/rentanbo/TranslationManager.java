package com.example.rentanbo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationManager {

    private static TranslationManager instance;
    private SharedPreferences prefs;
    private Context context;
    private Translator translator;
    private boolean isSwahiliMode = false;
    private boolean isTranslatorReady = false;

    // Store all translatable views across the app
    private Map<String, List<TranslatableView>> activityViews = new HashMap<>();

    // Cache translations
    private Map<String, String> translationCache = new HashMap<>();

    // Queue for pending translations while translator is downloading
    private List<PendingTranslation> pendingTranslations = new ArrayList<>();

    private TranslationManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);

        // Load saved language preference
        this.isSwahiliMode = prefs.getBoolean("is_swahili_mode", false);

        // Initialize translator if in Swahili mode
        if (isSwahiliMode) {
            initializeTranslator();
        }
    }

    public static synchronized TranslationManager getInstance(Context context) {
        if (instance == null) {
            instance = new TranslationManager(context);
        }
        return instance;
    }

    private void initializeTranslator() {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SWAHILI)
                .build();

        translator = Translation.getClient(options);

        // Download model if needed
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    isTranslatorReady = true;
                    Log.d("TranslationManager", "Translator ready");

                    // Process any pending translations
                    processPendingTranslations();
                })
                .addOnFailureListener(e -> {
                    Log.e("TranslationManager", "Failed to download model", e);
                    isTranslatorReady = false;
                });
    }

    private void processPendingTranslations() {
        for (PendingTranslation pending : pendingTranslations) {
            translateText(pending.text, pending.viewId, pending.callback);
        }
        pendingTranslations.clear();
    }

    /**
     * Register a view for translation
     */
    public void registerView(Activity activity, View view, @StringRes int stringResId) {
        if (activity == null || view == null) return;

        String activityName = activity.getClass().getSimpleName();

        if (!activityViews.containsKey(activityName)) {
            activityViews.put(activityName, new ArrayList<>());
        }

        // Get the actual string from resources
        String originalText = activity.getString(stringResId);

        // Check if view already registered
        List<TranslatableView> views = activityViews.get(activityName);
        for (TranslatableView tv : views) {
            if (tv.viewId == view.getId()) {
                // Update original text if needed
                tv.originalText = originalText;
                return;
            }
        }

        activityViews.get(activityName).add(new TranslatableView(
                new WeakReference<>(view),
                originalText,
                stringResId,
                view.getId()
        ));

        // If already in Swahili mode, translate immediately
        if (isSwahiliMode) {
            translateView(activity, view, originalText, stringResId, view.getId());
        }
    }

    /**
     * Switch to Swahili
     */
    public void switchToSwahili(Activity currentActivity) {
        isSwahiliMode = true;
        prefs.edit().putBoolean("is_swahili_mode", true).apply();

        if (translator == null) {
            initializeTranslator();
        }

        translateAllViews(currentActivity);
    }

    /**
     * Switch to English
     */
    public void switchToEnglish(Activity currentActivity) {
        isSwahiliMode = false;
        prefs.edit().putBoolean("is_swahili_mode", false).apply();
        restoreEnglishTexts(currentActivity);
    }

    /**
     * Check current mode
     */
    public boolean isSwahiliMode() {
        return isSwahiliMode;
    }

    private void translateAllViews(Activity activity) {
        if (activity == null) return;

        String activityName = activity.getClass().getSimpleName();
        List<TranslatableView> views = activityViews.get(activityName);

        if (views == null || views.isEmpty()) return;

        for (TranslatableView tv : views) {
            View view = activity.findViewById(tv.viewId);
            if (view != null) {
                translateView(activity, view, tv.originalText, tv.stringResId, tv.viewId);
            }
        }
    }

    private void translateView(Activity activity, View view, String originalText,
                               int stringResId, int viewId) {
        if (!isSwahiliMode) return;

        String cacheKey = activity.getClass().getSimpleName() + "_" + stringResId;

        // Check cache first
        if (translationCache.containsKey(cacheKey)) {
            View viewToUpdate = view != null ? view : activity.findViewById(viewId);
            if (viewToUpdate != null) {
                setViewText(viewToUpdate, translationCache.get(cacheKey));
            }
            return;
        }

        // Translate
        translateText(originalText, viewId, new TranslationCallback() {
            @Override
            public void onTranslated(String translatedText, int viewId) {
                View translatedView = activity.findViewById(viewId);
                if (translatedView != null) {
                    activity.runOnUiThread(() -> {
                        setViewText(translatedView, translatedText);
                    });
                    translationCache.put(cacheKey, translatedText);
                }
            }

            @Override
            public void onError(String error, int viewId) {
                Log.e("TranslationManager", "Error translating: " + error);
            }
        });
    }

    private void translateText(String text, int viewId, TranslationCallback callback) {
        if (!isTranslatorReady) {
            // Queue for later when translator is ready
            pendingTranslations.add(new PendingTranslation(text, viewId, callback));
            return;
        }

        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    callback.onTranslated(translatedText, viewId);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage(), viewId);
                });
    }

    private void restoreEnglishTexts(Activity activity) {
        if (activity == null) return;

        String activityName = activity.getClass().getSimpleName();
        List<TranslatableView> views = activityViews.get(activityName);

        if (views == null || views.isEmpty()) return;

        for (TranslatableView tv : views) {
            View view = activity.findViewById(tv.viewId);
            if (view != null) {
                activity.runOnUiThread(() -> {
                    setViewText(view, tv.originalText);
                });
            }
        }
    }

    private void setViewText(View view, String text) {
        if (view == null) return;

        if (view instanceof TextView) {
            ((TextView) view).setText(text);
        } else if (view instanceof Button) {
            ((Button) view).setText(text);
        } else if (view instanceof EditText) {
            ((EditText) view).setHint(text);
        } else if (view instanceof SearchView) {
            ((SearchView) view).setQueryHint(text);
        } else if (view instanceof SwitchMaterial) {
            ((SwitchMaterial) view).setText(text);
        }
    }

    /**
     * Clear cache for an activity (call in onDestroy)
     */
    public void clearActivity(Activity activity) {
        if (activity == null) return;

        String activityName = activity.getClass().getSimpleName();
        activityViews.remove(activityName);
    }

    /**
     * Clear entire translation cache
     */
    public void clearCache() {
        translationCache.clear();
    }

    /**
     * Inner class to hold view references
     */
    private static class TranslatableView {
        WeakReference<View> viewRef;
        String originalText;
        int stringResId;
        int viewId;

        TranslatableView(WeakReference<View> viewRef, String originalText,
                         int stringResId, int viewId) {
            this.viewRef = viewRef;
            this.originalText = originalText;
            this.stringResId = stringResId;
            this.viewId = viewId;
        }
    }

    private static class PendingTranslation {
        String text;
        int viewId;
        TranslationCallback callback;

        PendingTranslation(String text, int viewId, TranslationCallback callback) {
            this.text = text;
            this.viewId = viewId;
            this.callback = callback;
        }
    }

    public interface TranslationCallback {
        void onTranslated(String translatedText, int viewId);
        void onError(String error, int viewId);
    }
}