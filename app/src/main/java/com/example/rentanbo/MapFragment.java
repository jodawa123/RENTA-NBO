package com.example.rentanbo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * MapFragment displays listings on a Google Map.
 * Shows markers for each property with pricing and location information.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private List<Listing> listings = new ArrayList<>();
    private FilterState filterState;

    // Default location (Nairobi, Kenya center)
    private static final LatLng NAIROBI_CENTER = new LatLng(-1.2866, 36.8172);
    private static final float DEFAULT_ZOOM = 12f;

    public MapFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize FilterState for filtering listings
        filterState = FilterState.getInstance();

        // Get map fragment and initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // Set default camera to Nairobi
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(NAIROBI_CENTER, DEFAULT_ZOOM));

        // Load and display listings
        if (listings != null && !listings.isEmpty()) {
            displayListingsOnMap(listings);
        } else {
            loadListingsOnMap();
        }

        // Set up marker click listener
        googleMap.setOnMarkerClickListener(marker -> {
            // Show listing details on marker click
            String title = marker.getTitle();
            String snippet = marker.getSnippet();
            Toast.makeText(requireContext(), title + "\n" + snippet, Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    public void submitListings(List<Listing> listingItems) {
        listings = listingItems != null ? new ArrayList<>(listingItems) : new ArrayList<>();
        if (googleMap != null) {
            if (listings.isEmpty()) {
                googleMap.clear();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(NAIROBI_CENTER, DEFAULT_ZOOM));
            } else {
                displayListingsOnMap(listings);
            }
        }
    }

    /**
     * Load listings from Firestore and display them as markers on the map
     */
    private void loadListingsOnMap() {
        FirestoreManager firestoreManager = FirestoreManager.getInstance();

        firestoreManager.getFilteredListings(filterState, new FirestoreManager.ListingsCallback() {
            @Override
            public void onSuccess(List<Listing> loadedListings) {
                listings = loadedListings;
                if (listings != null && !listings.isEmpty()) {
                    displayListingsOnMap(listings);
                } else {
                    Toast.makeText(requireContext(), "No listings found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), "Failed to load listings: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Display all listings as markers on the map
     */
    private void displayListingsOnMap(List<Listing> listingsToDisplay) {
        if (googleMap == null || listingsToDisplay.isEmpty()) {
            return;
        }

        // Clear existing markers
        googleMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (Listing listing : listingsToDisplay) {
            // Use location from listing if available, otherwise use a default area
            LatLng position;
            if (listing.getLocation() != null) {
                position = new LatLng(
                        listing.getLocation().getLatitude(),
                        listing.getLocation().getLongitude()
                );
            } else {
                // Default to Nairobi center + slight random offset based on neighborhood
                position = getLocationForNeighborhood(listing.getNeighborhood());
            }

            // Create marker for listing
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(listing.getTitle())
                    .snippet("KSh " + listing.getPrice() + "/month | " + listing.getNeighborhood())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));

            // Add marker to map
            googleMap.addMarker(markerOptions);

            boundsBuilder.include(position);
        }

        if (listingsToDisplay.size() > 1) {
            LatLngBounds bounds = boundsBuilder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
        } else if (!listingsToDisplay.isEmpty()) {
            LatLng firstPosition = listingsToDisplay.get(0).getLocation() != null
                    ? new LatLng(
                    listingsToDisplay.get(0).getLocation().getLatitude(),
                    listingsToDisplay.get(0).getLocation().getLongitude())
                    : getLocationForNeighborhood(listingsToDisplay.get(0).getNeighborhood());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, DEFAULT_ZOOM));
        }
    }

    /**
     * Get approximate coordinates for Nairobi neighborhoods
     * In production, store actual GPS coordinates in Firestore
     */
    private LatLng getLocationForNeighborhood(String neighborhood) {
        if (neighborhood == null) {
            return NAIROBI_CENTER;
        }

        // Sample coordinates for common Nairobi neighborhoods
        switch (neighborhood.toLowerCase()) {
            case "rongai":
                return new LatLng(-1.3521, 36.8090);
            case "kahawa":
                return new LatLng(-1.2400, 36.9100);
            case "langata":
                return new LatLng(-1.3667, 36.7667);
            case "westlands":
                return new LatLng(-1.2667, 36.8000);
            case "karen":
                return new LatLng(-1.3833, 36.6833);
            case "kilimani":
                return new LatLng(-1.3000, 36.7667);
            default:
                return NAIROBI_CENTER;
        }
    }

    /**
     * Refresh map with new listings (call when filters change)
     */
    public void refreshMap() {
        if (googleMap != null) {
            loadListingsOnMap();
        }
    }
}

