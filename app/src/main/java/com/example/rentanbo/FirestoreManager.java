package com.example.rentanbo;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    // =========================
    // MAIN FILTER METHOD
    // =========================
    public void getFilteredListings(FilterState filterState, ListingsCallback callback) {

        Query query = db.collection(COLLECTION_LISTINGS);

        // Apply Firestore-supported filters first

        // Price range
        query = query.whereGreaterThanOrEqualTo("price", filterState.getMinPrice())
                .whereLessThanOrEqualTo("price", filterState.getMaxPrice());

        // Neighborhood filter
        if (!filterState.getNeighborhood().isEmpty()) {
            query = query.whereEqualTo("neighborhood", filterState.getNeighborhood());
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Listing> listings = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        Listing listing = null;
                        try {
                            listing = document.toObject(Listing.class);
                            if (listing != null) listing.setId(document.getId());
                        } catch (Exception e) {
                            // Fallback manual mapping to avoid Timestamp -> long errors
                            Log.w(TAG, "Automatic mapping failed for document " + document.getId() + ", falling back to manual mapping", e);
                            listing = new Listing();
                            listing.setId(document.getId());

                            // Basic fields
                            listing.setTitle(document.getString("title"));
                            Long priceLong = document.getLong("price");
                            listing.setPrice(priceLong != null ? priceLong.intValue() : 0);
                            listing.setNeighborhood(document.getString("neighborhood"));
                            listing.setHouseType(document.getString("houseType"));

                            // Lists (amenities, images)
                            Object amenitiesObj = document.get("amenities");
                            if (amenitiesObj instanceof java.util.List) {
                                //noinspection unchecked
                                listing.setAmenities((java.util.List<String>) amenitiesObj);
                            }

                            Object imagesObj = document.get("images");
                            if (imagesObj instanceof java.util.List) {
                                //noinspection unchecked
                                listing.setImages((java.util.List<String>) imagesObj);
                            }

                            // GeoPoint
                            try {
                                com.google.firebase.firestore.GeoPoint gp = document.getGeoPoint("location");
                                listing.setLocation(gp);
                            } catch (Exception ignore) {}

                            listing.setPhysicalAddress(document.getString("physicalAddress"));

                            // Landlord
                            try {
                                Object landlordObj = document.get("landlord");
                                if (landlordObj instanceof java.util.Map) {
                                    java.util.Map map = (java.util.Map) landlordObj;
                                    Listing.Landlord landlord = new Listing.Landlord();
                                    Object n = map.get("name");
                                    Object p = map.get("phone");
                                    Object em = map.get("email");
                                    if (n != null) landlord.setName(n.toString());
                                    if (p != null) landlord.setPhone(p.toString());
                                    if (em != null) landlord.setEmail(em.toString());
                                    listing.setLandlord(landlord);
                                }
                            } catch (Exception ignore) {}

                            // createdAt - accept Timestamp or numeric
                            Object createdObj = document.get("createdAt");
                            if (createdObj != null) {
                                listing.setCreatedAt(createdObj);
                            }
                        }

                        // Apply in-memory filters
                        if (shouldIncludeListing(listing, filterState)) {
                            listings.add(listing);
                        }
                    }

                    // If strict filters produce no hits, return budget-matching listings as fallback.
                    if (listings.isEmpty() && hasActiveNonPriceFilters(filterState)) {
                        getBudgetFallbackListings(filterState, callback);
                    } else {
                        callback.onSuccess(listings);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting listings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    private boolean hasActiveNonPriceFilters(FilterState filterState) {
        return filterState != null
                && (!filterState.getNeighborhood().isEmpty()
                || !filterState.getSelectedAmenities().isEmpty()
                || !filterState.getSelectedHouseTypes().isEmpty()
                || !filterState.getSearchQuery().isEmpty());
    }

    private void getBudgetFallbackListings(FilterState filterState, ListingsCallback callback) {
        db.collection(COLLECTION_LISTINGS)
                .whereGreaterThanOrEqualTo("price", filterState.getMinPrice())
                .whereLessThanOrEqualTo("price", filterState.getMaxPrice())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> fallbackListings = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = null;
                        try {
                            listing = document.toObject(Listing.class);
                            if (listing != null) listing.setId(document.getId());
                        } catch (Exception e) {
                            Log.w(TAG, "Fallback mapping failed for document " + document.getId(), e);
                        }

                        if (listing != null) {
                            fallbackListings.add(listing);
                        }
                    }

                    if (fallbackListings.isEmpty()) {
                        getAllListingsFallback(callback);
                    } else {
                        callback.onSuccess(fallbackListings);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting fallback listings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    private void getAllListingsFallback(ListingsCallback callback) {
        db.collection(COLLECTION_LISTINGS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> allListings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Listing listing = document.toObject(Listing.class);
                            listing.setId(document.getId());
                            allListings.add(listing);
                        } catch (Exception e) {
                            Log.w(TAG, "All-listings fallback mapping failed for document " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(allListings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting all-listings fallback", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // =========================
    // IN-MEMORY FILTERS
    // =========================
    private boolean shouldIncludeListing(Listing listing, FilterState filterState) {

        if (listing == null) return false;

        // House Type Filter (case-insensitive)
        if (!filterState.getSelectedHouseTypes().isEmpty()) {

            if (listing.getHouseType() == null) return false;

            String listingHouseType = listing.getHouseType().toLowerCase();

            if (!filterState.getSelectedHouseTypes().contains(listingHouseType)) {
                return false;
            }
        }

        // Amenities Filter (case-insensitive)
        if (!filterState.getSelectedAmenities().isEmpty()) {

            if (listing.getAmenities() == null) return false;

            List<String> listingAmenitiesLower = new ArrayList<>();

            for (String amenity : listing.getAmenities()) {
                listingAmenitiesLower.add(amenity.toLowerCase());
            }

            for (String selectedAmenity : filterState.getSelectedAmenities()) {
                if (!listingAmenitiesLower.contains(selectedAmenity.toLowerCase())) {
                    return false;
                }
            }
        }

        // Search Filter (title OR neighborhood)
        if (!filterState.getSearchQuery().isEmpty()) {

            String queryLower = filterState.getSearchQuery().toLowerCase();

            String title = listing.getTitle() != null ? listing.getTitle().toLowerCase() : "";
            String neighborhood = listing.getNeighborhood() != null
                    ? listing.getNeighborhood().toLowerCase()
                    : "";

            if (!(title.contains(queryLower) || neighborhood.contains(queryLower))) {
                return false;
            }
        }

        return true;
    }

    // =========================
    // SINGLE LISTING FETCH
    // =========================
    public void getListingById(String listingId,
                               OnSuccessListener<Listing> onSuccess,
                               OnFailureListener onFailure) {

        db.collection(COLLECTION_LISTINGS)
                .document(listingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {
                        Listing listing = null;
                        try {
                            listing = documentSnapshot.toObject(Listing.class);
                            if (listing != null) listing.setId(documentSnapshot.getId());
                        } catch (Exception e) {
                            Log.w(TAG, "Automatic mapping failed for document " + documentSnapshot.getId() + ", falling back to manual mapping", e);
                            listing = new Listing();
                            listing.setId(documentSnapshot.getId());
                            listing.setTitle(documentSnapshot.getString("title"));
                            Long priceLong = documentSnapshot.getLong("price");
                            listing.setPrice(priceLong != null ? priceLong.intValue() : 0);
                            listing.setNeighborhood(documentSnapshot.getString("neighborhood"));
                            listing.setHouseType(documentSnapshot.getString("houseType"));

                            Object amenitiesObj = documentSnapshot.get("amenities");
                            if (amenitiesObj instanceof java.util.List) {
                                //noinspection unchecked
                                listing.setAmenities((java.util.List<String>) amenitiesObj);
                            }

                            Object imagesObj = documentSnapshot.get("images");
                            if (imagesObj instanceof java.util.List) {
                                //noinspection unchecked
                                listing.setImages((java.util.List<String>) imagesObj);
                            }

                            try {
                                com.google.firebase.firestore.GeoPoint gp = documentSnapshot.getGeoPoint("location");
                                listing.setLocation(gp);
                            } catch (Exception ignore) {}

                            listing.setPhysicalAddress(documentSnapshot.getString("physicalAddress"));

                            try {
                                Object landlordObj = documentSnapshot.get("landlord");
                                if (landlordObj instanceof java.util.Map) {
                                    java.util.Map map = (java.util.Map) landlordObj;
                                    Listing.Landlord landlord = new Listing.Landlord();
                                    Object n = map.get("name");
                                    Object p = map.get("phone");
                                    Object em = map.get("email");
                                    if (n != null) landlord.setName(n.toString());
                                    if (p != null) landlord.setPhone(p.toString());
                                    if (em != null) landlord.setEmail(em.toString());
                                    listing.setLandlord(landlord);
                                }
                            } catch (Exception ignore) {}

                            Object createdObj = documentSnapshot.get("createdAt");
                            if (createdObj != null) listing.setCreatedAt(createdObj);
                        }

                        onSuccess.onSuccess(listing);
                    } else {
                        onFailure.onFailure(new Exception("Listing not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }
}