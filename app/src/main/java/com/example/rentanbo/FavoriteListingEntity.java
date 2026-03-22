package com.example.rentanbo;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_listings")
public class FavoriteListingEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    public String title;
    public int price;
    public String neighborhood;
    public String houseType;
    public String description;
    public String imageUrl;
    public double latitude;
    public double longitude;
    public String physicalAddress;
    public String landlordName;
    public String landlordPhone;
    public String landlordEmail;
    public long cachedAt;
}

