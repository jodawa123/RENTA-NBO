package com.example.rentanbo;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class Listing {
    private String id;
    private String title;
    private int price;
    private String neighborhood;
    private String houseType;
    private List<String> amenities;
    private List<String> images;
    private GeoPoint location;
    private String physicalAddress;
    private Landlord landlord;
    // Store createdAt as Object to accept both Firestore Timestamps and numeric values
    private Object createdAt;

    // Empty constructor for Firestore
    public Listing() {}

    public Listing(String title, int price, String neighborhood, String houseType,
                   List<String> amenities, List<String> images) {
        this.title = title;
        this.price = price;
        this.neighborhood = neighborhood;
        this.houseType = houseType;
        this.amenities = amenities;
        this.images = images;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }

    public String getHouseType() { return houseType; }
    public void setHouseType(String houseType) { this.houseType = houseType; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    public String getPhysicalAddress() { return physicalAddress; }
    public void setPhysicalAddress(String physicalAddress) { this.physicalAddress = physicalAddress; }

    public Landlord getLandlord() { return landlord; }
    public void setLandlord(Landlord landlord) { this.landlord = landlord; }

    /**
     * Returns createdAt in milliseconds since epoch. Handles Firestore Timestamp and numeric types.
     */
    @PropertyName("createdAt")
    public long getCreatedAt() {
        if (createdAt == null) return 0L;

        // If stored as Firestore Timestamp
        if (createdAt instanceof Timestamp) {
            try {
                return ((Timestamp) createdAt).toDate().getTime();
            } catch (Exception e) {
                return 0L;
            }
        }

        // If stored as Long, Integer, Double, etc.
        if (createdAt instanceof Long) {
            return (Long) createdAt;
        }
        if (createdAt instanceof Integer) {
            return ((Integer) createdAt).longValue();
        }
        if (createdAt instanceof Double) {
            return ((Double) createdAt).longValue();
        }

        // Fallback: attempt to parse string
        try {
            return Long.parseLong(createdAt.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Setter used by Firestore mapping. Accepts Timestamp or numeric values.
     */
    @PropertyName("createdAt")
    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    // Inner Landlord class
    public static class Landlord {
        private String name;
        private String phone;
        private String email;

        public Landlord() {}

        public Landlord(String name, String phone, String email) {
            this.name = name;
            this.phone = phone;
            this.email = email;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // Helper method to get formatted price
    public String getFormattedPrice() {
        return String.format("%,d", price);
    }
}