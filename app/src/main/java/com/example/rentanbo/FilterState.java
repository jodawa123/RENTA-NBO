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
        return selectedAmenities;
    }

    public void toggleAmenity(String amenity) {
        if (amenity == null) return;

        String normalized = amenity.toLowerCase().trim();

        if (selectedAmenities.contains(normalized)) {
            selectedAmenities.remove(normalized);
        } else {
            selectedAmenities.add(normalized);
        }
    }

    // ================= HOUSE TYPES =================
    public Set<String> getSelectedHouseTypes() {
        return selectedHouseTypes;
    }

    public void toggleHouseType(String houseType) {
        if (houseType == null) return;

        String normalized = houseType.toLowerCase().trim();

        if (selectedHouseTypes.contains(normalized)) {
            selectedHouseTypes.remove(normalized);
        } else {
            selectedHouseTypes.add(normalized);
        }
    }

    // ================= SEARCH =================
    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        if (searchQuery == null) {
            this.searchQuery = "";
        } else {
            this.searchQuery = searchQuery.trim().toLowerCase();
        }
    }

    // ================= NEIGHBORHOOD =================
    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        if (neighborhood == null) {
            this.neighborhood = "";
        } else {
            this.neighborhood = neighborhood.trim();
        }
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