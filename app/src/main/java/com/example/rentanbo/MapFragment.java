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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private static final float DEFAULT_ZOOM = 12f;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLocation;

    // Listings passed in from HomePage — no separate Firestore query needed
    private List<Listing> listings = new ArrayList<>();
    private String currentNeighborhood = "";

    public interface OnListingSelectedListener {
        void onListingSelected(String listingId, String title);
    }

    private OnListingSelectedListener listener;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Info window click → notify host activity + open directions
        mMap.setOnInfoWindowClickListener(marker -> {
            if (marker.getTag() instanceof Listing) {
                Listing listing = (Listing) marker.getTag();
                if (listener != null) {
                    listener.onListingSelected(listing.getId(), listing.getTitle());
                }
                if (listing.getLocation() != null) {
                    openGoogleMapsDirections(listing.getLatitude(), listing.getLongitude());
                }
            }
        });

        // Default camera position: central Nairobi
        LatLng nairobi = new LatLng(-1.286389, 36.817223);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nairobi, DEFAULT_ZOOM));

        getUserLocation();

        // Render whatever listings have already been passed in
        displayMarkers();
    }

    // -----------------------------------------------------------------------
    // Called by HomePage after it loads or filters listings
    // -----------------------------------------------------------------------
    public void setListings(List<Listing> listings) {
        this.listings = listings != null ? listings : new ArrayList<>();
        Log.d(TAG, "setListings() called with " + this.listings.size() + " listings");
        if (mMap != null) {
            displayMarkers();
        }
    }

    public void filterByNeighborhood(String neighborhood) {
        this.currentNeighborhood = neighborhood != null ? neighborhood : "";
        if (mMap != null) displayMarkers();
    }

    public void resetToAllListings() {
        this.currentNeighborhood = "";
        if (mMap != null) displayMarkers();
    }

    // -----------------------------------------------------------------------
    // Draw markers from the in-memory listings list
    // -----------------------------------------------------------------------
    private void displayMarkers() {
        if (mMap == null) {
            Log.w(TAG, "displayMarkers() called but mMap is null — skipping");
            return;
        }

        mMap.clear();

        // Re-add "You are here" marker
        if (userLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_YELLOW)));
        }

        int markerCount = 0;
        LatLng firstLocation = null;

        for (Listing listing : listings) {
            // Must have a valid GeoPoint
            if (listing.getLocation() == null) {
                Log.w(TAG, "Skipping listing '" + listing.getTitle()
                        + "' — location is null");
                continue;
            }

            // Apply neighbourhood filter
            if (!currentNeighborhood.isEmpty()
                    && !currentNeighborhood.equalsIgnoreCase(listing.getNeighborhood())) {
                continue;
            }

            LatLng latLng = new LatLng(listing.getLatitude(), listing.getLongitude());

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(listing.getTitle())
                    .snippet("KSh " + listing.getFormattedPrice()
                            + "  •  " + listing.getNeighborhood()
                            + "\nTap for directions")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN)));

            if (marker != null) {
                marker.setTag(listing);   // store the whole Listing for click handling
                markerCount++;
                if (firstLocation == null) firstLocation = latLng;
            }
        }

        Log.d(TAG, "displayMarkers() added " + markerCount + " markers");

        // Centre map
        if (userLocation == null && firstLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, DEFAULT_ZOOM));
        }

        // Only show toast if fragment is still attached
        if (!isAdded()) return;

        if (markerCount > 0) {
            Toast.makeText(requireContext(),
                    markerCount + " listings shown", Toast.LENGTH_SHORT).show();
        } else if (!listings.isEmpty()) {
            String area = currentNeighborhood.isEmpty() ? "this area" : currentNeighborhood;
            Toast.makeText(requireContext(),
                    "No listings found in " + area, Toast.LENGTH_SHORT).show();
        }
    }

    // -----------------------------------------------------------------------
    // User location
    // -----------------------------------------------------------------------


    private void getUserLocation() {
        if (!isAdded()) return;
        if (ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && mMap != null && isAdded()) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                // ✅ Don't add marker directly here — just redraw everything
                // This ensures the yellow marker is never wiped by a concurrent clear()
                displayMarkers();

                // Move camera to user
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
            }
        });
    }

    // -----------------------------------------------------------------------
    // Open Google Maps directions
    // -----------------------------------------------------------------------
    private void openGoogleMapsDirections(double destLat, double destLng) {
        if (!isAdded()) return;
        String uri;
        if (userLocation != null) {
            uri = String.format(Locale.ENGLISH,
                    "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                    userLocation.latitude, userLocation.longitude, destLat, destLng);
        } else {
            uri = String.format(Locale.ENGLISH,
                    "https://www.google.com/maps/dir/?api=1&destination=%f,%f&travelmode=driving",
                    destLat, destLng);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}