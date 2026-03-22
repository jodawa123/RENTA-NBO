package com.example.rentanbo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteListingDao {

    @Query("SELECT * FROM favorite_listings ORDER BY cachedAt DESC")
    List<FavoriteListingEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<FavoriteListingEntity> entities);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FavoriteListingEntity entity);

    @Query("DELETE FROM favorite_listings WHERE id = :listingId")
    void deleteById(String listingId);

    @Query("DELETE FROM favorite_listings")
    void clearAll();
}

