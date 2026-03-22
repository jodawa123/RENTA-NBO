package com.example.rentanbo;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {FavoriteListingEntity.class}, version = 1, exportSchema = false)
public abstract class FavoritesDatabase extends RoomDatabase {

    public abstract FavoriteListingDao favoriteListingDao();

    private static volatile FavoritesDatabase instance;

    public static FavoritesDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (FavoritesDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    FavoritesDatabase.class,
                                    "favorites_cache.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}

