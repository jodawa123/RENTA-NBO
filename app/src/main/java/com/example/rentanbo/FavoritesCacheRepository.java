package com.example.rentanbo;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritesCacheRepository {

    public interface FavoritesLocalCallback {
        void onResult(List<Listing> listings);
    }

    private static FavoritesCacheRepository instance;

    private final FavoriteListingDao dao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private FavoritesCacheRepository(Context context) {
        dao = FavoritesDatabase.getInstance(context).favoriteListingDao();
    }

    public static synchronized FavoritesCacheRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesCacheRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void cacheFavorites(List<Listing> listings) {
        ioExecutor.execute(() -> {
            dao.clearAll();
            if (listings == null || listings.isEmpty()) {
                return;
            }

            List<FavoriteListingEntity> entities = new ArrayList<>();
            for (Listing listing : listings) {
                FavoriteListingEntity entity = toEntity(listing);
                if (entity != null) {
                    entities.add(entity);
                }
            }
            if (!entities.isEmpty()) {
                dao.upsertAll(entities);
            }
        });
    }

    public void upsertFavorite(Listing listing) {
        ioExecutor.execute(() -> {
            FavoriteListingEntity entity = toEntity(listing);
            if (entity != null) {
                dao.upsert(entity);
            }
        });
    }

    public void removeFavorite(String listingId) {
        ioExecutor.execute(() -> dao.deleteById(listingId));
    }

    public void getCachedFavorites(@NonNull FavoritesLocalCallback callback) {
        ioExecutor.execute(() -> callback.onResult(fromEntities(dao.getAll())));
    }

    private FavoriteListingEntity toEntity(Listing listing) {
        if (listing == null || listing.getId().trim().isEmpty()) {
            return null;
        }

        FavoriteListingEntity entity = new FavoriteListingEntity();
        entity.id = listing.getId();
        entity.title = listing.getTitle();
        entity.price = listing.getPrice();
        entity.neighborhood = listing.getNeighborhood();
        entity.houseType = listing.getHouseType();
        entity.description = listing.getDescription();
        entity.imageUrl = listing.getImages().isEmpty() ? "" : listing.getImages().get(0);

        GeoPoint location = listing.getLocation();
        if (location != null) {
            entity.latitude = location.getLatitude();
            entity.longitude = location.getLongitude();
        }

        entity.physicalAddress = listing.getPhysicalAddress();

        Listing.Landlord landlord = listing.getLandlord();
        if (landlord != null) {
            entity.landlordName = landlord.getName();
            entity.landlordPhone = landlord.getPhone();
            entity.landlordEmail = landlord.getEmail();
        }

        entity.cachedAt = System.currentTimeMillis();
        return entity;
    }

    private List<Listing> fromEntities(List<FavoriteListingEntity> entities) {
        List<Listing> listings = new ArrayList<>();
        if (entities == null) {
            return listings;
        }

        for (FavoriteListingEntity entity : entities) {
            Listing listing = new Listing();
            listing.setId(entity.id);
            listing.setTitle(entity.title);
            listing.setPrice(entity.price);
            listing.setNeighborhood(entity.neighborhood);
            listing.setHouseType(entity.houseType);
            listing.setDescription(entity.description);

            List<String> images = new ArrayList<>();
            if (entity.imageUrl != null && !entity.imageUrl.trim().isEmpty()) {
                images.add(entity.imageUrl);
            }
            listing.setImages(images);

            if (entity.latitude != 0.0d || entity.longitude != 0.0d) {
                listing.setLocation(new GeoPoint(entity.latitude, entity.longitude));
            }

            listing.setPhysicalAddress(entity.physicalAddress);

            Listing.Landlord landlord = new Listing.Landlord();
            landlord.setName(entity.landlordName);
            landlord.setPhone(entity.landlordPhone);
            landlord.setEmail(entity.landlordEmail);
            listing.setLandlord(landlord);

            listings.add(listing);
        }

        return listings;
    }
}

