package com.example.rentanbo;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.StringRes;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationManager {

    private static TranslationManager instance;
    private TranslationHelper translationHelper;
    private boolean isSwahiliMode = false;

    // Store all translatable views across the app
    private Map<String, List<TranslatableView>> activityViews = new HashMap<>();

    // Cache translations
    private Map<String, String> translationCache = new HashMap<>();

    private TranslationManager(Activity activity) {
        translationHelper = new TranslationHelper(activity);
    }

    public static synchronized TranslationManager getInstance(Activity activity) {
        if (instance == null) {
            instance = new TranslationManager(activity);
        }
        return instance;
    }

    /**
     * Register a view for translation
     */
    public void registerView(Activity activity, View view, @StringRes int stringResId) {
        String activityName = activity.getClass().getSimpleName();

        if (!activityViews.containsKey(activityName)) {
            activityViews.put(activityName, new ArrayList<>());
        }

        activityViews.get(activityName).add(new TranslatableView(
                new WeakReference<>(view),
                activity.getString(stringResId),
                stringResId,
                view.getId()
        ));
    }

    /**
     * Switch to Swahili
     */
    public void switchToSwahili(Activity currentActivity) {
        if (isSwahiliMode) return;

        isSwahiliMode = true;
        translateAllViews(currentActivity);
    }

    /**
     * Switch to English
     */
    public void switchToEnglish(Activity currentActivity) {
        if (!isSwahiliMode) return;

        isSwahiliMode = false;
        restoreEnglishTexts(currentActivity);
    }

    /**
     * Check current mode
     */
    public boolean isSwahiliMode() {
        return isSwahiliMode;
    }

    private void translateAllViews(Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        List<TranslatableView> views = activityViews.get(activityName);

        if (views == null || views.isEmpty()) return;

        for (TranslatableView tv : views) {
            View view = tv.viewRef.get();
            if (view == null) continue;

            String cacheKey = activityName + "_" + tv.stringResId;

            // Check cache first
            if (translationCache.containsKey(cacheKey)) {
                setViewText(view, translationCache.get(cacheKey));
                continue;
            }

            // Translate
            translationHelper.translateText(tv.originalText, tv.viewId,
                    new TranslationHelper.TranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText, int viewId) {
                            View translatedView = activity.findViewById(viewId);
                            if (translatedView != null) {
                                setViewText(translatedView, translatedText);
                                translationCache.put(cacheKey, translatedText);
                            }
                        }

                        @Override
                        public void onError(String error, int viewId) {
                            Log.e("TranslationManager", "Error translating: " + error);
                        }
                    });
        }
    }

    private void restoreEnglishTexts(Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        List<TranslatableView> views = activityViews.get(activityName);

        if (views == null || views.isEmpty()) return;

        for (TranslatableView tv : views) {
            View view = activity.findViewById(tv.viewId);
            if (view != null) {
                setViewText(view, tv.originalText);
            }
        }
    }

    private void setViewText(View view, String text) {
        if (view instanceof TextView) {
            ((TextView) view).setText(text);
        } else if (view instanceof Button) {
            ((Button) view).setText(text);
        } else if (view instanceof EditText) {
            ((EditText) view).setHint(text);
        } else if (view instanceof SwitchMaterial) {
            ((SwitchMaterial) view).setText(text);
        }
    }

    /**
     * Clear cache for an activity (call in onDestroy)
     */
    public void clearActivity(Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        activityViews.remove(activityName);
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
}