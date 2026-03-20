package com.example.rentanbo;

import java.util.HashSet;
import java.util.Set;

public class FilterState {

    // ================= PRICE =================
    private int minPrice = 10000;
    private int maxPrice = 50000;

    // ================= FILTERS =================
    private Set<String> selectedAmenities = new HashSet<>();
    private Set<String> selectedHouseTypes = new HashSet<>();
    private String searchQuery = "";
    private String neighborhood = "";

    // ================= SINGLETON =================
    private static FilterState instance;

    private FilterState() {}

    public static synchronized FilterState getInstance() {
        if (instance == null) {
            instance = new FilterState();
        }
        return instance;
    }

    // ================= NORMALIZATION HELPER =================
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    // ================= PRICE =================
    public int getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(int minPrice) {
        this.minPrice = minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }

    // ================= AMENITIES =================
    public Set<String> getSelectedAmenities() {
        return selectedAmenities != null ? selectedAmenities : new HashSet<>();
    }

    public void toggleAmenity(String amenity) {
        String normalized = normalize(amenity);

        if (normalized.isEmpty()) return;

        if (selectedAmenities.contains(normalized)) {
            selectedAmenities.remove(normalized);
        } else {
            selectedAmenities.add(normalized);
        }
    }

    // ================= HOUSE TYPES =================
    public Set<String> getSelectedHouseTypes() {
        return selectedHouseTypes != null ? selectedHouseTypes : new HashSet<>();
    }

    public void toggleHouseType(String houseType) {
        String normalized = normalize(houseType);

        if (normalized.isEmpty()) return;

        if (selectedHouseTypes.contains(normalized)) {
            selectedHouseTypes.remove(normalized);
        } else {
            selectedHouseTypes.add(normalized);
        }
    }

    // ================= SEARCH =================
    public String getSearchQuery() {
        return searchQuery != null ? searchQuery : "";
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = normalize(searchQuery);
    }

    // ================= NEIGHBORHOOD =================
    public String getNeighborhood() {
        return neighborhood != null ? neighborhood : "";
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood == null ? "" : neighborhood.trim();
    }

    // ================= RESET =================
    public void clearFilters() {
        minPrice = 10000;
        maxPrice = 50000;
        selectedAmenities.clear();
        selectedHouseTypes.clear();
        searchQuery = "";
        neighborhood = "";
    }
}