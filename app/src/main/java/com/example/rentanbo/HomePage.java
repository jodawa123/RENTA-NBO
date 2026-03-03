package com.example.rentanbo;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends BaseActivity {

    private static final String TAG = "HomePage";

    // UI Components
    private TextView welcomeName;
    private EditText searchBar;
    private ImageButton filterButton;
    private LinearLayout filterDetailsLayout;
    private SeekBar budgetSeekBar;
    private TextView budgetRangeText;
    private Chip chipWifi, chipParking, chipSecurity, chipBorehole;
    private Chip chipStudio, chipBedsitter, chipOneBed;
    private Button listButton, mapButton;
    private TextView resultsCount;
    private RecyclerView recyclerView;
    private SwitchMaterial languageSwitch;
    private ChipGroup amenityChipGroup, houseTypeChipGroup;

    // State
    private boolean isFilterVisible = false;
    private ListingsAdapter adapter;
    private FirestoreManager firestoreManager;
    private FilterState filterState;
    private String userName = "";
    private boolean isViewDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            StatusBarUtil.setLightStatusBar(this);
            setContentView(R.layout.activity_home_page);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // Get user data from intent
            if (getIntent() != null) {
                userName = getIntent().getStringExtra("name");
                if (userName == null) userName = "";
            }

            // Initialize
            firestoreManager = FirestoreManager.getInstance();
            filterState = FilterState.getInstance();

            // Initialize views first
            initViews();

            if (!isViewDestroyed) {

                // Setup components
                setupRecyclerView();
                setupFilterToggle();
                setupBudgetSeekBar();
                setupAmenityChips();
                setupHouseTypeChips();
                setupSearchBar();
                setupButtons();
                setupLanguageSwitch();

                // Load initial listings
                loadListings();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
            showToast("Error loading page: " + e.getMessage());
        }
    }

    private void initViews() {
        try {
            // Initialize all views with null safety
            welcomeName = findViewById(R.id.name);
            searchBar = findViewById(R.id.search_bar);
            filterButton = findViewById(R.id.imageFilterButton);
            filterDetailsLayout = findViewById(R.id.filterdetails);
            budgetSeekBar = findViewById(R.id.seekBar2);
            budgetRangeText = findViewById(R.id.textView13);

            // Amenity chips
            chipWifi = findViewById(R.id.chip);
            chipParking = findViewById(R.id.chip2);
            chipSecurity = findViewById(R.id.chip3);
            chipBorehole = findViewById(R.id.chip4);

            // House type chips
            chipStudio = findViewById(R.id.chip5);
            chipBedsitter = findViewById(R.id.chip6);
            chipOneBed = findViewById(R.id.chip7);

            // Buttons
            listButton = findViewById(R.id.button);
            mapButton = findViewById(R.id.button2);

            // Results
            resultsCount = findViewById(R.id.editTextText2);

            // RecyclerView
            recyclerView = findViewById(R.id.recyclerView);

            // Language switch
            languageSwitch = findViewById(R.id.switchlanguage);

            // Set welcome name with null check
            if (welcomeName != null) {
                String displayName = userName.isEmpty() ? " Guest" : " " + userName;
                welcomeName.setText(displayName);
            }

            // Initially hide filter details
            if (filterDetailsLayout != null) {
                filterDetailsLayout.setVisibility(View.GONE);
            }

            // Set initial budget range text
            if (budgetRangeText != null) {
                budgetRangeText.setText("Ksh 10,000 - 50,000");
            }

            Log.d(TAG, "All views initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            e.printStackTrace();
            isViewDestroyed = true;
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView is null");
            return;
        }

        try {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ListingsAdapter(this, listing -> {
                // Handle view details click
                showToast("Viewing: " + listing.getTitle());
                // TODO: Navigate to listing details page
            });
            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView: " + e.getMessage());
        }
    }

    private void setupFilterToggle() {
        if (filterButton == null || filterDetailsLayout == null) return;

        filterButton.setOnClickListener(v -> {
            try {
                isFilterVisible = !isFilterVisible;
                filterDetailsLayout.setVisibility(isFilterVisible ? View.VISIBLE : View.GONE);

                // Animate rotation
                filterButton.animate()
                        .rotation(isFilterVisible ? 180 : 0)
                        .setDuration(300)
                        .start();
            } catch (Exception e) {
                Log.e(TAG, "Error toggling filter: " + e.getMessage());
            }
        });
    }

    private void setupBudgetSeekBar() {
        if (budgetSeekBar == null || budgetRangeText == null) return;

        try {
            budgetSeekBar.setMax(40); // 10k to 50k in 1k steps
            budgetSeekBar.setProgress(0);
            updateBudgetRangeText(10000, 50000);

            budgetSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && filterState != null && budgetRangeText != null) {
                        int minPrice = 10000;
                        int maxPrice = 10000 + (progress * 1000);
                        filterState.setMinPrice(minPrice);
                        filterState.setMaxPrice(maxPrice);
                        updateBudgetRangeText(minPrice, maxPrice);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    loadListings();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up budget seekbar: " + e.getMessage());
        }
    }

    private void updateBudgetRangeText(int min, int max) {
        if (budgetRangeText != null) {
            budgetRangeText.setText(String.format("Ksh %d - %d", min, max));
        }
    }

    private void setupAmenityChips() {
        // Create a list of chips and their corresponding amenity names
        Chip[] amenityChips = {chipWifi, chipParking, chipSecurity, chipBorehole};
        String[] amenityNames = {"WiFi", "Parking", "Security Lights", "Borehole Water"};

        for (int i = 0; i < amenityChips.length; i++) {
            final Chip chip = amenityChips[i];
            final String amenityName = amenityNames[i];

            if (chip == null) continue;

            // Set initial state
            chip.setChecked(false);
            chip.setChipBackgroundColorResource(R.color.lightgrey);
            chip.setTextColor(ContextCompat.getColor(this, R.color.darkgray));

            chip.setOnClickListener(v -> {
                try {
                    boolean isChecked = chip.isChecked();

                    // Change background tint based on checked state
                    if (isChecked) {
                        chip.setChipBackgroundColorResource(R.color.green);
                        chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                    } else {
                        chip.setChipBackgroundColorResource(R.color.lightgrey);
                        chip.setTextColor(ContextCompat.getColor(this, R.color.darkgray));
                    }

                    if (filterState != null) {
                        filterState.toggleAmenity(amenityName);
                        loadListings();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in amenity chip click: " + e.getMessage());
                }
            });
        }
    }

    private void setupHouseTypeChips() {
        // Create a list of chips and their corresponding house type names
        Chip[] houseTypeChips = {chipStudio, chipBedsitter, chipOneBed};
        String[] houseTypeNames = {"studio", "bedsitter", "1 bedroom"};

        for (int i = 0; i < houseTypeChips.length; i++) {
            final Chip chip = houseTypeChips[i];
            final String houseTypeName = houseTypeNames[i];

            if (chip == null) continue;

            // Set initial state
            chip.setChecked(false);
            chip.setChipBackgroundColorResource(R.color.lightgrey);
            chip.setTextColor(ContextCompat.getColor(this, R.color.darkgray));

            chip.setOnClickListener(v -> {
                try {
                    boolean isChecked = chip.isChecked();

                    // Change background tint based on checked state
                    if (isChecked) {
                        chip.setChipBackgroundColorResource(R.color.green);
                        chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                    } else {
                        chip.setChipBackgroundColorResource(R.color.lightgrey);
                        chip.setTextColor(ContextCompat.getColor(this, R.color.darkgray));
                    }

                    if (filterState != null) {
                        filterState.toggleHouseType(houseTypeName);
                        loadListings();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in house type chip click: " + e.getMessage());
                }
            });
        }
    }

    private void setupSearchBar() {
        if (searchBar == null) return;

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (filterState != null) {
                    filterState.setSearchQuery(s.toString());
                    loadListings();
                }
            }
        });
    }

    private void setupButtons() {
        if (listButton != null) {
            listButton.setOnClickListener(v -> {
                try {
                    // Switch to list view (already showing)
                    listButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
                    if (mapButton != null) {
                        mapButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lightgrey));
                    }
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in list button click: " + e.getMessage());
                }
            });
        }

        if (mapButton != null) {
            mapButton.setOnClickListener(v -> {
                try {
                    // Switch to map view
                    mapButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
                    if (listButton != null) {
                        listButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lightgrey));
                    }
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.GONE);
                    }
                    showToast("Map view coming soon");
                } catch (Exception e) {
                    Log.e(TAG, "Error in map button click: " + e.getMessage());
                }
            });
        }
    }

    private void setupLanguageSwitch() {
        if (languageSwitch != null) {
            setupLanguageSwitch(languageSwitch);
        }
    }

    private void loadListings() {
        if (firestoreManager == null || filterState == null || adapter == null) {
            Log.e(TAG, "Cannot load listings: dependencies null");
            return;
        }

        firestoreManager.getFilteredListings(filterState, new FirestoreManager.ListingsCallback() {
            @Override
            public void onSuccess(List<Listing> listings) {
                runOnUiThread(() -> {
                    try {
                        if (adapter != null) {
                            adapter.setListings(listings != null ? listings : new ArrayList<>());
                            updateResultsCount(listings != null ? listings.size() : 0);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI with listings: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Failed to load listings: " + error);
                    if (adapter != null) {
                        adapter.setListings(new ArrayList<>());
                        updateResultsCount(0);
                    }
                    showToast("Error loading listings: " + error);
                });
            }
        });
    }

    private void updateResultsCount(int count) {
        if (resultsCount != null) {
            String resultText = count + " " + (count == 1 ? "result" : "results") + " found";
            resultsCount.setText(resultText);
        }
    }

    protected void showToast(String message) {
        if (!isViewDestroyed) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isViewDestroyed = true;
        adapter = null;
    }
}