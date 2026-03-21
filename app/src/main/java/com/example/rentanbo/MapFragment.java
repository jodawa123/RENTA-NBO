package com.example.rentanbo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private static final float DEFAULT_ZOOM = 12f;

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLocation;
    private List<LocationData> allLocations = new ArrayList<>();
    private String currentNeighborhood = "";
    private boolean markersAdded = false;

    public interface OnListingSelectedListener {
        void onListingSelected(String listingId, String title);
    }

    private OnListingSelectedListener listener;

    private static class LocationData {
        String id;
        String title;
        String neighborhood;
        GeoPoint location;

        LocationData(String id, String title, String neighborhood, GeoPoint location) {
            this.id = id;
            this.title = title;
            this.neighborhood = neighborhood;
            this.location = location;
        }
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnListingSelectedListener) {
            listener = (OnListingSelectedListener) context;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Set info window click listener
        mMap.setOnInfoWindowClickListener(marker -> {
            LocationData data = (LocationData) marker.getTag();
            if (data != null && listener != null) {
                listener.onListingSelected(data.id, data.title);
                openGoogleMapsDirections(data.location);
            }
        });

        // Center on Nairobi initially
        LatLng nairobi = new LatLng(-1.286389, 36.817223);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nairobi, DEFAULT_ZOOM));

        // Get user location
        getUserLocation();

        // Load locations from Firestore
        loadLocations();
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {
                if (location != null && mMap != null) {
                    userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    mMap.addMarker(new MarkerOptions()
                            .position(userLocation)
                            .title("You are here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
                }
            });
        }
    }

    private void loadLocations() {
        db.collection("listings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        allLocations.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            GeoPoint location = document.getGeoPoint("location");
                            String title = document.getString("title");
                            String neighborhood = document.getString("neighborhood");

                            if (location != null && title != null) {
                                allLocations.add(new LocationData(
                                        document.getId(),
                                        title,
                                        neighborhood != null ? neighborhood : "",
                                        location
                                ));
                            }
                        }

                        // Display markers on map
                        displayMarkers();

                    } else {
                        Toast.makeText(getContext(), "Failed to load listings", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayMarkers() {
        if (mMap == null) return;

        // Clear all markers
        mMap.clear();

        // Re-add user location marker if exists
        if (userLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        }

        // Filter and add markers
        int markerCount = 0;
        LatLng firstLocation = null;

        for (LocationData data : allLocations) {
            // Apply neighborhood filter
            if (!currentNeighborhood.isEmpty() && !currentNeighborhood.equalsIgnoreCase(data.neighborhood)) {
                continue;
            }

            LatLng latLng = new LatLng(data.location.getLatitude(), data.location.getLongitude());

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(data.title)
                    .snippet("Tap for directions")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            if (marker != null) {
                marker.setTag(data);
                markerCount++;
                if (firstLocation == null) {
                    firstLocation = latLng;
                }
            }
        }

        // Center on first location if no user location
        if (userLocation == null && firstLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, DEFAULT_ZOOM));
        }

        // Show toast message
        if (markerCount > 0) {
            Toast.makeText(getContext(), markerCount + " listings shown", Toast.LENGTH_SHORT).show();
        } else if (!allLocations.isEmpty()) {
            Toast.makeText(getContext(), "No listings in " + currentNeighborhood, Toast.LENGTH_SHORT).show();
        }
    }

    public void filterByNeighborhood(String neighborhood) {
        this.currentNeighborhood = neighborhood;
        displayMarkers();
    }

    public void resetToAllListings() {
        this.currentNeighborhood = "";
        displayMarkers();
    }

    private void openGoogleMapsDirections(GeoPoint destination) {
        if (userLocation != null && destination != null) {
            LatLng dest = new LatLng(destination.getLatitude(), destination.getLongitude());

            String uri = String.format(Locale.ENGLISH,
                    "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                    userLocation.latitude, userLocation.longitude, dest.latitude, dest.longitude);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            }
        }
    }
}