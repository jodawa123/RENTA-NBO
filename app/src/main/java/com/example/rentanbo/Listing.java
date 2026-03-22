package com.example.rentanbo;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class Listing {
    private String id;
    private String title;
    private int price;
    private String neighborhood;
    private String houseType;
    private String description;
    private List<String> amenities;
    private List<String> images;
    private GeoPoint location;
    private String physicalAddress;
    private Landlord landlord;
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

    // ================= GETTERS & SETTERS =================

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getNeighborhood() { return neighborhood != null ? neighborhood : ""; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }

    public String getHouseType() { return houseType != null ? houseType : ""; }
    public void setHouseType(String houseType) { this.houseType = houseType; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getAmenities() {
        return amenities != null ? amenities : new ArrayList<>();
    }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public List<String> getImages() {
        return images != null ? images : new ArrayList<>();
    }
    public void setImages(List<String> images) { this.images = images; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    public String getPhysicalAddress() {
        return physicalAddress != null ? physicalAddress : "";
    }
    public void setPhysicalAddress(String physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

    public Landlord getLandlord() { return landlord; }
    public void setLandlord(Landlord landlord) { this.landlord = landlord; }

    // ================= CREATED AT =================

    @PropertyName("createdAt")
    public long getCreatedAt() {
        if (createdAt == null) return 0L;

        if (createdAt instanceof Timestamp) {
            try {
                return ((Timestamp) createdAt).toDate().getTime();
            } catch (Exception e) {
                return 0L;
            }
        }

        if (createdAt instanceof Long) return (Long) createdAt;
        if (createdAt instanceof Integer) return ((Integer) createdAt).longValue();
        if (createdAt instanceof Double) return ((Double) createdAt).longValue();

        try {
            return Long.parseLong(createdAt.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    // ================= GEO HELPERS (NEW) =================

    public double getLatitude() {
        return location != null ? location.getLatitude() : 0.0;
    }

    public double getLongitude() {
        return location != null ? location.getLongitude() : 0.0;
    }

    // ================= LANDLORD =================

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

        public String getName() { return name != null ? name : ""; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone != null ? phone : ""; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email != null ? email : ""; }
        public void setEmail(String email) { this.email = email; }
    }

    // ================= UTIL =================

    public String getFormattedPrice() {
        return String.format("%,d", price);
    }
}