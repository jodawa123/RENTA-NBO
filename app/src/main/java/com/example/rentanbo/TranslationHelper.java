package com.example.rentanbo;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;

public class TranslationHelper {

    private Translator englishSwahiliTranslator;
    private Context context;
    private boolean isModelDownloaded = false;
    private List<QueuedTranslation> translationQueue = new ArrayList<>();

    private static class QueuedTranslation {
        String text;
        int viewId;
        TranslationCallback callback;

        QueuedTranslation(String text, int viewId, TranslationCallback callback) {
            this.text = text;
            this.viewId = viewId;
            this.callback = callback;
        }
    }

    public interface TranslationCallback {
        void onTranslated(String translatedText, int viewId);
        void onError(String error, int viewId);
    }

    public TranslationHelper(Context context) {
        this.context = context;

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SWAHILI)
                .build();

        englishSwahiliTranslator = Translation.getClient(options);
    }

    public void translateText(String text, int viewId, TranslationCallback callback) {
        // Add to queue
        translationQueue.add(new QueuedTranslation(text, viewId, callback));

        if (isModelDownloaded) {
            // Model already downloaded, process queue
            processQueue();
        } else {
            // Download model first
            downloadModel();
        }
    }

    private void downloadModel() {
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()  // Only download on WiFi
                .build();

        englishSwahiliTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Translation", "Model downloaded successfully");
                        isModelDownloaded = true;
                        processQueue();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Translation", "Model download failed: " + e.getMessage());
                        // Notify all queued translations of failure
                        for (QueuedTranslation queued : translationQueue) {
                            if (queued.callback != null) {
                                queued.callback.onError("Model download failed: " + e.getMessage(),
                                        queued.viewId);
                            }
                        }
                        translationQueue.clear();
                    }
                });
    }

    private void processQueue() {
        if (translationQueue.isEmpty()) return;

        // Process first item in queue
        QueuedTranslation queued = translationQueue.get(0);
        performTranslation(queued.text, queued.viewId, queued.callback);
    }

    private void performTranslation(String text, int viewId, TranslationCallback callback) {
        Log.d("Translation", "Translating: " + text);

        englishSwahiliTranslator.translate(text)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        Log.d("Translation", "Translated to: " + translatedText);

                        // Remove the processed item from queue
                        if (!translationQueue.isEmpty()) {
                            translationQueue.remove(0);
                        }

                        // Notify callback
                        if (callback != null) {
                            callback.onTranslated(translatedText, viewId);
                        }

                        // Process next in queue
                        processQueue();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Translation", "Translation failed: " + e.getMessage());

                        // Remove the failed item from queue
                        if (!translationQueue.isEmpty()) {
                            translationQueue.remove(0);
                        }

                        // Notify callback of error
                        if (callback != null) {
                            callback.onError("Translation failed: " + e.getMessage(), viewId);
                        }

                        // Process next in queue anyway
                        processQueue();
                    }
                });
    }

    public void closeTranslator() {
        if (englishSwahiliTranslator != null) {
            englishSwahiliTranslator.close();
        }
        translationQueue.clear();
    }
}