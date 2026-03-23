package com.example.rentanbo;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;
    private FirebaseFirestore db;

    private static final String COLLECTION_LISTINGS = "listings";
    private static final String COLLECTION_FAVORITES = "favorites";
    private static final String COLLECTION_TOUR_REQUESTS = "tourRequests";
    private static final String COLLECTION_SAVED_SEARCHES = "savedSearches";

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

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface FavoriteStatusCallback {
        void onResult(boolean isFavorite);
        void onFailure(String error);
    }

    public interface FavoriteIdsCallback {
        void onSuccess(List<String> listingIds);
        void onFailure(String error);
    }

    // =========================
    // MAIN FILTER METHOD
    // =========================
    public void getFilteredListings(FilterState filterState, ListingsCallback callback) {

        Query query = db.collection(COLLECTION_LISTINGS);
        boolean attemptedNeighborhoodQuery = false;

        // Apply Firestore-supported filters first

        // Price range
        query = query.whereGreaterThanOrEqualTo("price", filterState.getMinPrice())
                .whereLessThanOrEqualTo("price", filterState.getMaxPrice());

        // Neighborhood filter
        if (!filterState.getNeighborhood().isEmpty()) {
            query = query.whereEqualTo("neighborhood", filterState.getNeighborhood());
            attemptedNeighborhoodQuery = true;
        }

        final boolean neighborhoodQueryUsed = attemptedNeighborhoodQuery;

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Listing> listings = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = mapListingDocument(document);

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
                    if (neighborhoodQueryUsed && isMissingCompositeIndex(e)) {
                        Log.w(TAG, "Missing index for neighborhood + price query. Falling back to price-only query.", e);
                        getFilteredListingsWithoutNeighborhoodQuery(filterState, callback);
                        return;
                    }

                    Log.e(TAG, "Error getting listings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    private void getFilteredListingsWithoutNeighborhoodQuery(FilterState filterState, ListingsCallback callback) {
        db.collection(COLLECTION_LISTINGS)
                .whereGreaterThanOrEqualTo("price", filterState.getMinPrice())
                .whereLessThanOrEqualTo("price", filterState.getMaxPrice())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> listings = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = mapListingDocument(document);
                        if (shouldIncludeListing(listing, filterState)) {
                            listings.add(listing);
                        }
                    }

                    // Keep existing UX: if strict filters return no results, fall back to budget matches.
                    if (listings.isEmpty() && hasActiveNonPriceFilters(filterState)) {
                        getBudgetFallbackListings(filterState, callback);
                    } else {
                        callback.onSuccess(listings);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting listings with price-only fallback", e);
                    callback.onFailure(e.getMessage());
                });
    }

    private boolean isMissingCompositeIndex(Exception e) {
        if (!(e instanceof FirebaseFirestoreException)) {
            return false;
        }

        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
        return firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION;
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
                        Listing listing = mapListingDocument(document);

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
                        Listing listing = mapListingDocument(document);
                        if (listing != null) {
                            allListings.add(listing);
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
// IN-MEMORY FILTERS (FIXED)
// =========================
    private boolean shouldIncludeListing(Listing listing, FilterState filterState) {

        if (listing == null) return false;

        // ================= NEIGHBORHOOD =================
        if (!filterState.getNeighborhood().isEmpty()) {
            String selectedNeighborhood = filterState.getNeighborhood().trim().toLowerCase();
            String listingNeighborhood = listing.getNeighborhood() != null
                    ? listing.getNeighborhood().trim().toLowerCase()
                    : "";

            if (!listingNeighborhood.equals(selectedNeighborhood)) {
                return false;
            }
        }

        // ================= HOUSE TYPE =================
        if (!filterState.getSelectedHouseTypes().isEmpty()) {

            if (listing.getHouseType() == null) return false;

            String listingHouseType = listing.getHouseType().trim().toLowerCase();

            boolean match = false;
            for (String selected : filterState.getSelectedHouseTypes()) {
                if (listingHouseType.equals(selected.trim().toLowerCase())) {
                    match = true;
                    break;
                }
            }

            if (!match) return false;
        }

        // ================= AMENITIES =================
        if (!filterState.getSelectedAmenities().isEmpty()) {

            if (listing.getAmenities() == null || listing.getAmenities().isEmpty()) {
                return false;
            }

            // Normalize listing amenities
            List<String> listingAmenitiesLower = new ArrayList<>();
            for (String amenity : listing.getAmenities()) {
                if (amenity != null) {
                    listingAmenitiesLower.add(amenity.trim().toLowerCase());
                }
            }

            // Check ALL selected amenities exist
            for (String selectedAmenity : filterState.getSelectedAmenities()) {

                boolean found = false;

                for (String a : listingAmenitiesLower) {
                    if (a.equals(selectedAmenity.trim().toLowerCase())) {
                        found = true;
                        break;
                    }
                }

                if (!found) return false;
            }
        }

        // ================= SEARCH =================
        if (!filterState.getSearchQuery().isEmpty()) {

            String queryLower = filterState.getSearchQuery().trim().toLowerCase();

            String title = listing.getTitle() != null
                    ? listing.getTitle().toLowerCase()
                    : "";

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
                        Listing listing = mapListingDocument(documentSnapshot);

                        onSuccess.onSuccess(listing);
                    } else {
                        onFailure.onFailure(new Exception("Listing not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    public void getSimilarListings(Listing referenceListing, int maxResults, ListingsCallback callback) {
        if (referenceListing == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        Query query = db.collection(COLLECTION_LISTINGS).limit(40);
        if (!referenceListing.getNeighborhood().trim().isEmpty()) {
            query = query.whereEqualTo("neighborhood", referenceListing.getNeighborhood());
        }

        final int priceWindow = Math.max(3000, (int) (referenceListing.getPrice() * 0.3f));

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Listing> similarListings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = mapListingDocument(document);
                        if (listing == null) {
                            continue;
                        }

                        if (listing.getId().equals(referenceListing.getId())) {
                            continue;
                        }

                        if (referenceListing.getPrice() > 0) {
                            int priceDifference = Math.abs(listing.getPrice() - referenceListing.getPrice());
                            if (priceDifference > priceWindow) {
                                continue;
                            }
                        }

                        similarListings.add(listing);
                        if (similarListings.size() >= Math.max(1, maxResults)) {
                            break;
                        }
                    }

                    callback.onSuccess(similarListings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting similar listings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void isListingFavorite(String userId, String listingId, FavoriteStatusCallback callback) {
        if (callback == null) {
            return;
        }

        if (userId == null || userId.trim().isEmpty() || listingId == null || listingId.trim().isEmpty()) {
            callback.onResult(false);
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> listingIds = (List<String>) documentSnapshot.get("listingIds");
                    boolean isFavorite = listingIds != null && listingIds.contains(listingId);
                    callback.onResult(isFavorite);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking favorite status", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void setFavoriteListing(String userId, String listingId, boolean shouldFavorite, SimpleCallback callback) {
        if (callback == null) {
            return;
        }

        if (userId == null || userId.trim().isEmpty() || listingId == null || listingId.trim().isEmpty()) {
            callback.onFailure("Missing user or listing id");
            return;
        }

        DocumentReference favoriteRef = db.collection(COLLECTION_FAVORITES).document(userId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("listingIds", shouldFavorite
                ? FieldValue.arrayUnion(listingId)
                : FieldValue.arrayRemove(listingId));
        payload.put("updatedAt", Timestamp.now());

        favoriteRef.set(payload, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating favorite", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void getFavoriteListingIds(String userId, FavoriteIdsCallback callback) {
        if (callback == null) {
            return;
        }

        if (userId == null || userId.trim().isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> listingIds = (List<String>) documentSnapshot.get("listingIds");
                    callback.onSuccess(listingIds != null ? listingIds : new ArrayList<>());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting favorite listing IDs", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void getListingsByIds(List<String> listingIds, ListingsCallback callback) {
        if (callback == null) {
            return;
        }

        if (listingIds == null || listingIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<String> sanitizedIds = new ArrayList<>();
        for (String id : listingIds) {
            if (id != null && !id.trim().isEmpty()) {
                sanitizedIds.add(id.trim());
            }
        }

        if (sanitizedIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_LISTINGS)
                .whereIn(FieldPath.documentId(), sanitizedIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Listing> listingMap = new HashMap<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Listing listing = mapListingDocument(document);
                        if (listing != null) {
                            listingMap.put(listing.getId(), listing);
                        }
                    }

                    List<Listing> ordered = new ArrayList<>();
                    for (String id : sanitizedIds) {
                        Listing listing = listingMap.get(id);
                        if (listing != null) {
                            ordered.add(listing);
                        }
                    }
                    callback.onSuccess(ordered);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting listings by ids", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void saveSearchCriteria(String userId, FilterState filterState, SimpleCallback callback) {
        if (callback == null) {
            return;
        }

        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure("Missing user id");
            return;
        }

        if (filterState == null) {
            callback.onFailure("Missing filter state");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("neighborhood", filterState.getNeighborhood());
        payload.put("budgetMin", filterState.getMinPrice());
        payload.put("budgetMax", filterState.getMaxPrice());
        payload.put("amenities", new ArrayList<>(filterState.getSelectedAmenities()));
        payload.put("houseTypes", new ArrayList<>(filterState.getSelectedHouseTypes()));
        payload.put("searchQuery", filterState.getSearchQuery());
        payload.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_SAVED_SEARCHES)
                .document(userId)
                .collection("items")
                .add(payload)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving search criteria", e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void saveVirtualTourRequest(String userId, Listing listing, String preferredSlot, SimpleCallback callback) {
        if (callback == null) {
            return;
        }

        if (userId == null || userId.trim().isEmpty() || listing == null || listing.getId().trim().isEmpty()) {
            callback.onFailure("Missing user or listing info");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("listingId", listing.getId());
        payload.put("listingTitle", listing.getTitle());
        payload.put("landlordPhone", listing.getLandlord() != null ? listing.getLandlord().getPhone() : "");
        payload.put("preferredSlot", preferredSlot != null ? preferredSlot : "Anytime");
        payload.put("status", "requested");
        payload.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_TOUR_REQUESTS)
                .add(payload)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving virtual tour request", e);
                    callback.onFailure(e.getMessage());
                });
    }

    private Listing mapListingDocument(DocumentSnapshot document) {
        Listing listing = null;
        try {
            listing = document.toObject(Listing.class);
            if (listing != null) listing.setId(document.getId());
        } catch (Exception e) {
            Log.w(TAG, "Automatic mapping failed for document " + document.getId() + ", falling back to manual mapping", e);
            listing = new Listing();
            listing.setId(document.getId());

            listing.setTitle(document.getString("title"));
            Long priceLong = document.getLong("price");
            listing.setPrice(priceLong != null ? priceLong.intValue() : 0);
            listing.setNeighborhood(document.getString("neighborhood"));
            listing.setHouseType(document.getString("houseType"));
            listing.setDescription(document.getString("description"));

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

            try {
                com.google.firebase.firestore.GeoPoint gp = document.getGeoPoint("location");
                listing.setLocation(gp);
            } catch (Exception ignore) {}

            listing.setPhysicalAddress(document.getString("physicalAddress"));

            try {
                Object landlordObj = document.get("landlord");
                if (landlordObj instanceof java.util.Map) {
                    Map map = (Map) landlordObj;
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

            Object createdObj = document.get("createdAt");
            if (createdObj != null) {
                listing.setCreatedAt(createdObj);
            }
        }
        return listing;
    }
}