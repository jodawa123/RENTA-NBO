package com.example.rentanbo;

import java.util.HashSet;
import java.util.Set;

public class FilterState {
    private int minPrice = 10000;
    private int maxPrice = 50000;
    private Set<String> selectedAmenities = new HashSet<>();
    private Set<String> selectedHouseTypes = new HashSet<>();
    private String searchQuery = "";

    // Singleton pattern
    private static FilterState instance;

    private FilterState() {}

    public static FilterState getInstance() {
        if (instance == null) {
            instance = new FilterState();
        }
        return instance;
    }

    // Getters and Setters
    public int getMinPrice() { return minPrice; }
    public void setMinPrice(int minPrice) { this.minPrice = minPrice; }

    public int getMaxPrice() { return maxPrice; }
    public void setMaxPrice(int maxPrice) { this.maxPrice = maxPrice; }

    public Set<String> getSelectedAmenities() { return selectedAmenities; }

    public void toggleAmenity(String amenity) {
        if (selectedAmenities.contains(amenity)) {
            selectedAmenities.remove(amenity);
        } else {
            selectedAmenities.add(amenity);
        }
    }

    public Set<String> getSelectedHouseTypes() { return selectedHouseTypes; }

    public void toggleHouseType(String houseType) {
        if (selectedHouseTypes.contains(houseType)) {
            selectedHouseTypes.remove(houseType);
        } else {
            selectedHouseTypes.add(houseType);
        }
    }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public void clearFilters() {
        minPrice = 10000;
        maxPrice = 50000;
        selectedAmenities.clear();
        selectedHouseTypes.clear();
        searchQuery = "";
    }
}