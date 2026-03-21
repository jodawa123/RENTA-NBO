package com.example.rentanbo;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomePage extends BaseActivity implements MapFragment.OnListingSelectedListener {

    private static final String TAG = "HomePage";

    // ================= USER DATA =================
    private String userName = "";

    // ================= UI =================
    private TextView welcomeName;
    private android.widget.SearchView searchBar;
    private ImageButton filterButton;
    private LinearLayout filterDetailsLayout;
    private SeekBar budgetSeekBar;
    private TextView budgetRangeText;
    private Chip chipWifi, chipParking, chipSecurity, chipBorehole;
    private Chip chipStudio, chipBedsitter, chipOneBed;
    private Button listButton, mapButton;
    private Button langataButton, rongaiButton, kahawaButton;
    private TextView resultsCount;
    private RecyclerView recyclerView;
    private SwitchMaterial languageSwitch;
    private LinearLayout mapButtonsLayout;

    // ================= STATE =================
    private ListingsAdapter adapter;
    private FirebaseFirestore db;
    private List<Listing> allListings = new ArrayList<>();
    private boolean isFilterVisible = false;
    private boolean isMapView = false;
    private MapFragment mapFragment;

    // Filter state variables
    private int minPrice = 10000;
    private int maxPrice = 50000;
    private Set<String> selectedAmenities = new HashSet<>();
    private Set<String> selectedHouseTypes = new HashSet<>();
    private String searchQuery = "";
    private String currentNeighborhood = "";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Get user name for greeting only
        if (getIntent() != null) {
            userName = getIntent().getStringExtra("name");
            if (userName == null) userName = "";
        }

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        initViews();
        registerAllViewsForTranslation();
        setupRecyclerView();
        setupFilterToggle();
        setupBudgetSeekBar();
        setupChips();
        setupSearchBar();
        setupViewToggle();
        setupNeighborhoodButtons();
        setupLanguageSwitch();

        // Initialize Firestore and load ALL listings
        db = FirebaseFirestore.getInstance();
        loadAllListings();
    }

    // ================= INIT VIEWS =================
    private void initViews() {
        welcomeName = findViewById(R.id.name);
        searchBar = findViewById(R.id.search_bar);
        filterButton = findViewById(R.id.imageFilterButton);
        filterDetailsLayout = findViewById(R.id.filterdetails);
        budgetSeekBar = findViewById(R.id.seekBar2);
        budgetRangeText = findViewById(R.id.textView13);

        chipWifi = findViewById(R.id.chip);
        chipParking = findViewById(R.id.chip2);
        chipSecurity = findViewById(R.id.chip3);
        chipBorehole = findViewById(R.id.chip4);

        chipStudio = findViewById(R.id.chip5);
        chipBedsitter = findViewById(R.id.chip6);
        chipOneBed = findViewById(R.id.chip7);

        listButton = findViewById(R.id.button);
        mapButton = findViewById(R.id.button2);
        langataButton = findViewById(R.id.langatab);
        rongaiButton = findViewById(R.id.rongaib);
        kahawaButton = findViewById(R.id.kahawab);
        mapButtonsLayout = findViewById(R.id.mapbuttons);
        resultsCount = findViewById(R.id.editTextText2);
        recyclerView = findViewById(R.id.recyclerView);
        languageSwitch = findViewById(R.id.switchlanguage);

        filterDetailsLayout.setVisibility(View.GONE);
        mapButtonsLayout.setVisibility(View.GONE);

        // Set welcome name
        welcomeName.setText(" " + (userName.isEmpty() ? "Guest" : userName));
    }

    // ================= LOAD ALL LISTINGS FROM FIRESTORE =================
    private void loadAllListings() {
        resultsCount.setText("Loading listings...");

        db.collection("listings")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            allListings.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Listing listing = document.toObject(Listing.class);
                                    listing.setId(document.getId());
                                    allListings.add(listing);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing listing: " + e.getMessage());
                                }
                            }
                            Log.d(TAG, "Loaded " + allListings.size() + " listings from Firestore");
                            displayAllListings();

                        } else {
                            Log.e(TAG, "Error getting listings: ", task.getException());
                            resultsCount.setText("Error loading listings");
                            showToast("Failed to load listings");
                        }
                    }
                });
    }

    // ================= DISPLAY ALL LISTINGS =================
    private void displayAllListings() {
        applyFiltersAndDisplay();
    }

    // ================= APPLY FILTERS AND DISPLAY =================
    private void applyFiltersAndDisplay() {
        List<Listing> filteredListings = new ArrayList<>();

        for (Listing listing : allListings) {
            if (matchesFilters(listing)) {
                filteredListings.add(listing);
            }
        }

        // Update recycler view
        adapter.setListings(filteredListings);
        updateResultsCount(filteredListings.size());

        // Update map if it exists (pass current filters to map)
        if (mapFragment != null) {
            List<String> amenitiesList = new ArrayList<>(selectedAmenities);
            List<String> houseTypesList = new ArrayList<>(selectedHouseTypes);


            // Apply neighborhood filter if any
            if (!currentNeighborhood.isEmpty()) {
                mapFragment.filterByNeighborhood(currentNeighborhood);
            }
        }

        Log.d(TAG, "Displaying " + filteredListings.size() + " filtered listings");
    }

    // ================= CHECK IF LISTING MATCHES FILTERS =================
    private boolean matchesFilters(Listing listing) {
        // Neighborhood filter from map buttons
        if (!currentNeighborhood.isEmpty() && !currentNeighborhood.equalsIgnoreCase(listing.getNeighborhood())) {
            return false;
        }

        // Price filter
        if (listing.getPrice() < minPrice || listing.getPrice() > maxPrice) {
            return false;
        }

        // House type filter
        if (!selectedHouseTypes.isEmpty()) {
            String listingHouseType = listing.getHouseType().toLowerCase().trim();
            if (!selectedHouseTypes.contains(listingHouseType)) {
                return false;
            }
        }

        // Amenities filter
        if (!selectedAmenities.isEmpty()) {
            for (String selectedAmenity : selectedAmenities) {
                boolean hasAmenity = false;
                for (String listingAmenity : listing.getAmenities()) {
                    if (listingAmenity.toLowerCase().trim().equals(selectedAmenity)) {
                        hasAmenity = true;
                        break;
                    }
                }
                if (!hasAmenity) {
                    return false;
                }
            }
        }

        // Search query filter
        if (!searchQuery.isEmpty()) {
            String title = listing.getTitle().toLowerCase();
            String neighborhood = listing.getNeighborhood().toLowerCase();
            String houseType = listing.getHouseType().toLowerCase();

            return title.contains(searchQuery) ||
                    neighborhood.contains(searchQuery) ||
                    houseType.contains(searchQuery);
        }

        return true;
    }

    // ================= RECYCLER =================
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListingsAdapter(this, listing -> {
            showToast("Viewing: " + listing.getTitle());
        });
        recyclerView.setAdapter(adapter);
    }

    // ================= VIEW TOGGLE =================
    private void setupViewToggle() {
        listButton.setOnClickListener(v -> {
            isMapView = false;
            recyclerView.setVisibility(View.VISIBLE);
            mapButtonsLayout.setVisibility(View.GONE);
            if (mapFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(mapFragment)
                        .commit();
                mapFragment = null;
            }
            findViewById(R.id.map_container).setVisibility(View.GONE);

            listButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green));
            mapButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.lightgrey));

            currentNeighborhood = "";
            updateNeighborhoodButtons(null);
            applyFiltersAndDisplay();
        });

        mapButton.setOnClickListener(v -> {
            isMapView = true;
            recyclerView.setVisibility(View.GONE);
            mapButtonsLayout.setVisibility(View.VISIBLE);
            findViewById(R.id.map_container).setVisibility(View.VISIBLE);

            mapButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green));
            listButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.lightgrey));

            if (mapFragment == null) {
                mapFragment = new MapFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.map_container, mapFragment)
                        .commit();
            }

            // Apply current neighborhood filter to map
            if (mapFragment != null && !currentNeighborhood.isEmpty()) {
                mapFragment.filterByNeighborhood(currentNeighborhood);
            }
        });
    }
    // ================= NEIGHBORHOOD BUTTONS =================
    private void setupNeighborhoodButtons() {
        langataButton.setOnClickListener(v -> {
            if (currentNeighborhood.equals("Langata")) {
                // Reset to show all listings
                currentNeighborhood = "";
                updateNeighborhoodButtons(null);
                applyFiltersAndDisplay();
                if (mapFragment != null) {
                    mapFragment.resetToAllListings();
                }
            } else {
                currentNeighborhood = "Langata";
                updateNeighborhoodButtons(langataButton);
                applyFiltersAndDisplay();
                if (mapFragment != null) {
                    mapFragment.filterByNeighborhood(currentNeighborhood);
                }
            }
        });

        rongaiButton.setOnClickListener(v -> {
            if (currentNeighborhood.equals("Rongai")) {
                // Reset to show all listings
                currentNeighborhood = "";
                updateNeighborhoodButtons(null);
                applyFiltersAndDisplay();
                if (mapFragment != null) {
                    mapFragment.resetToAllListings();
                }
            } else {
                currentNeighborhood = "Rongai";
                updateNeighborhoodButtons(rongaiButton);
                applyFiltersAndDisplay();
                if (mapFragment != null) {
                    mapFragment.filterByNeighborhood(currentNeighborhood);
                }
            }
        });

        kahawaButton.setOnClickListener(v -> {
            if (currentNeighborhood.equals("Kahawa")) {
                // Reset to show all listings
                currentNeighborhood = "";
                updateNeighborhoodButtons(null);
                applyFiltersAndDisplay();
                if (mapFragment != null) {
                    mapFragment.resetToAllListings();
                }
            } else {
                currentNeighborhood = "Kahawa";
                updateNeighborhoodButtons(kahawaButton);
                applyFiltersAndDisplay();
                if (mapFragment != null) {
                    mapFragment.filterByNeighborhood(currentNeighborhood);
                }
            }
        });
    }

    private void updateNeighborhoodButtons(Button selectedButton) {
        Button[] buttons = {langataButton, rongaiButton, kahawaButton};
        for (Button btn : buttons) {
            btn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
        }
        if (selectedButton != null) {
            selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
        }
    }

    // ================= FILTER TOGGLE =================
    private void setupFilterToggle() {
        filterButton.setOnClickListener(v -> {
            isFilterVisible = !isFilterVisible;
            filterDetailsLayout.setVisibility(
                    isFilterVisible ? View.VISIBLE : View.GONE);
        });
    }

    // ================= BUDGET =================
    private void setupBudgetSeekBar() {
        budgetSeekBar.setMax(40);
        budgetSeekBar.setProgress((maxPrice - 10000) / 1000);
        updateBudgetText(minPrice, maxPrice);

        budgetSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar, int progress, boolean fromUser) {
                        maxPrice = 10000 + (progress * 1000);
                        minPrice = 10000;
                        updateBudgetText(minPrice, maxPrice);

                        if (fromUser) {
                            applyFiltersAndDisplay();
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        applyFiltersAndDisplay();
                    }
                });
    }

    private void updateBudgetText(int min, int max) {
        String budgetText = isSwahiliModeEnabled()
                ? "KSh " + min + " hadi " + max
                : "KSh " + min + " - " + max;
        budgetRangeText.setText(budgetText);
    }

    // ================= CHIPS =================
    private void setupChips() {
        setupChip(chipWifi, "wifi", selectedAmenities);
        setupChip(chipParking, "parking", selectedAmenities);
        setupChip(chipSecurity, "security lights", selectedAmenities);
        setupChip(chipBorehole, "borehole water", selectedAmenities);
        setupChip(chipStudio, "studio", selectedHouseTypes);
        setupChip(chipBedsitter, "bedsitter", selectedHouseTypes);
        setupChip(chipOneBed, "1 bedroom", selectedHouseTypes);
    }

    private void setupChip(Chip chip, String value, Set<String> selectedSet) {
        if (chip == null) return;
        updateChipStyle(chip, selectedSet.contains(value.toLowerCase()));

        chip.setOnClickListener(v -> {
            String normalizedValue = value.toLowerCase();
            if (selectedSet.contains(normalizedValue)) {
                selectedSet.remove(normalizedValue);
                updateChipStyle(chip, false);
            } else {
                selectedSet.add(normalizedValue);
                updateChipStyle(chip, true);
            }
            applyFiltersAndDisplay();
        });
    }

    private void updateChipStyle(Chip chip, boolean isSelected) {
        if (isSelected) {
            chip.setChipBackgroundColorResource(R.color.green);
            chip.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            chip.setChipBackgroundColorResource(R.color.lightgrey);
            chip.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    // ================= SEARCH =================
    private void setupSearchBar() {
        searchBar.setOnQueryTextListener(
                new android.widget.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        searchQuery = query != null ? query.toLowerCase().trim() : "";
                        applyFiltersAndDisplay();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        searchQuery = newText != null ? newText.toLowerCase().trim() : "";
                        applyFiltersAndDisplay();
                        return true;
                    }
                });
    }

    // ================= LANGUAGE =================
    private void setupLanguageSwitch() {
        setupLanguageSwitch(languageSwitch);
    }

    @Override
    protected void onLanguageChanged(boolean isSwahili) {
        welcomeName.setText(" " + (userName.isEmpty() ? getLocalizedGuestName() : userName));
        updateBudgetText(minPrice, maxPrice);
        updateResultsCount(adapter != null ? adapter.getItemCount() : 0);
    }

    // ================= MAP FRAGMENT CALLBACK =================
    @Override
    public void onListingSelected(String listingId, String title) {
        // Switch to list view and scroll to the listing
        if (!isMapView) {
            // Find listing in adapter and scroll to it
            for (int i = 0; i < adapter.getListings().size(); i++) {
                if (adapter.getListings().get(i).getId().equals(listingId)) {
                    recyclerView.smoothScrollToPosition(i);
                    break;
                }
            }
        } else {
            // Switch to list view
            listButton.performClick();
            // Wait for adapter to be set then scroll
            recyclerView.postDelayed(() -> {
                for (int i = 0; i < adapter.getListings().size(); i++) {
                    if (adapter.getListings().get(i).getId().equals(listingId)) {
                        recyclerView.smoothScrollToPosition(i);
                        break;
                    }
                }
            }, 500);
        }
    }

    private void registerAllViewsForTranslation() {
        registerForTranslation(findViewById(R.id.textView6), R.string.home_page_title);
        registerForTranslation(findViewById(R.id.textView11), R.string.welcome_text);
        registerForTranslation(findViewById(R.id.search_bar), R.string.search_bar_hint);
        registerForTranslation(findViewById(R.id.textView12), R.string.home_budget_title);
        registerForTranslation(findViewById(R.id.textView13), R.string.home_budget_range_default);
        registerForTranslation(findViewById(R.id.textView14), R.string.home_amenities_title);
        registerForTranslation(findViewById(R.id.chip), R.string.wifi);
        registerForTranslation(findViewById(R.id.chip2), R.string.parking);
        registerForTranslation(findViewById(R.id.chip3), R.string.security);
        registerForTranslation(findViewById(R.id.chip4), R.string.borehole);
        registerForTranslation(findViewById(R.id.textView15), R.string.house_type);
        registerForTranslation(findViewById(R.id.chip5), R.string.studio);
        registerForTranslation(findViewById(R.id.chip6), R.string.bedsitter);
        registerForTranslation(findViewById(R.id.chip7), R.string.onebed);
        registerForTranslation(findViewById(R.id.button), R.string.list);
        registerForTranslation(findViewById(R.id.button2), R.string.map);
        registerForTranslation(findViewById(R.id.editTextText2), R.string.results);
    }

    private void updateResultsCount(int count) {
        if (isSwahiliModeEnabled()) {
            resultsCount.setText(count + " matokeo yamepatikana");
            return;
        }
        resultsCount.setText(count + (count == 1 ? " result found" : " results found"));
    }

    private boolean isSwahiliModeEnabled() {
        return translationManager != null && translationManager.isSwahiliMode();
    }

    private String getLocalizedGuestName() {
        return isSwahiliModeEnabled() ? "Mgeni" : "Guest";
    }

    protected void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}