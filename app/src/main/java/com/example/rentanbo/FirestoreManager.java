package com.example.rentanbo;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;
    private FirebaseFirestore db;
    private static final String COLLECTION_LISTINGS = "listings";

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    public interface ListingsCallback {
        void onSuccess(List<Listing> listings);
        void onFailure(String error);
    }


    public void getFilteredListings(FilterState filterState, ListingsCallback callback) {
        Query query = db.collection(COLLECTION_LISTINGS);

        // Apply price filter
        if (filterState.getMinPrice() > 0) {
            query = query.whereGreaterThanOrEqualTo("price", filterState.getMinPrice());
        }
        if (filterState.getMaxPrice() < 50000) {
            query = query.whereLessThanOrEqualTo("price", filterState.getMaxPrice());
        }

        // Apply house type filter
        if (!filterState.getSelectedHouseTypes().isEmpty()) {
            // Firestore doesn't support array-contains-any on multiple values easily
            // We'll handle this in memory for now
        }

        // Apply search query (title or neighborhood)
        if (!filterState.getSearchQuery().isEmpty()) {
            // Firestore doesn't support full-text search natively
            // We'll use a combination of whereGreaterThan/whereLessThan for basic prefix search
            String searchLower = filterState.getSearchQuery().toLowerCase();
            String searchUpper = searchLower + '\uf8ff';

            // This is a basic implementation - consider using Algolia or Elasticsearch for production
            query = query.orderBy("title").startAt(searchLower).endAt(searchUpper);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = document.toObject(Listing.class);
                        listing.setId(document.getId());

                        // Apply in-memory filtering for house types and amenities
                        if (shouldIncludeListing(listing, filterState)) {
                            listings.add(listing);
                        }
                    }
                    callback.onSuccess(listings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting listings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    private boolean shouldIncludeListing(Listing listing, FilterState filterState) {
        // Apply house type filter
        if (!filterState.getSelectedHouseTypes().isEmpty()) {
            if (!filterState.getSelectedHouseTypes().contains(listing.getHouseType())) {
                return false;
            }
        }

        // Apply amenities filter
        if (!filterState.getSelectedAmenities().isEmpty()) {
            if (listing.getAmenities() == null) return false;

            boolean hasAllAmenities = true;
            for (String amenity : filterState.getSelectedAmenities()) {
                if (!listing.getAmenities().contains(amenity)) {
                    hasAllAmenities = false;
                    break;
                }
            }
            if (!hasAllAmenities) return false;
        }

        return true;
    }

    public void getListingById(String listingId, OnSuccessListener<Listing> onSuccess,
                               OnFailureListener onFailure) {
        db.collection(COLLECTION_LISTINGS).document(listingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Listing listing = documentSnapshot.toObject(Listing.class);
                        listing.setId(documentSnapshot.getId());
                        onSuccess.onSuccess(listing);
                    } else {
                        onFailure.onFailure(new Exception("Listing not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }
}