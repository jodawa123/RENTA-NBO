package com.example.rentanbo;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomePage extends BaseActivity implements MapFragment.OnListingSelectedListener {

    private static final String TAG = "HomePage";
    private static final String KEY_FILTER_PANEL_VISIBLE = "key_filter_panel_visible";
    private static final String KEY_TIPS_SHOWN = "home_tips_shown";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // ================= USER DATA =================
    private String userId = "";
    private String userName = "";
    private String preferredNeighborhood = "";
    private String phoneNumber = "";
    private String userLanguage = "en";

    // ================= UI =================
    private TextView welcomeName;
    private android.widget.SearchView searchBar;
    private ImageButton filterButton;
    private ImageButton favoritesButton;
    private LinearLayout filterDetailsLayout;
    private SeekBar budgetSeekBar;
    private TextView budgetRangeText;

    // Amenity filter buttons
    private Button btnWifi, btnParking, btnSecurity, btnBorehole;
    // House type filter buttons
    private Button btnStudio, btnBedsitter, btnOneBed;

    private Button listButton, mapButton;
    private Button langataButton, rongaiButton, kahawaButton;
    private Button saveSearchButton;
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

    // Filter state — all start in "show everything" mode
    // Budget: 0 → Integer.MAX_VALUE means no price filter until seekbar is moved
    private int minPrice = 0;
    private int maxPrice = Integer.MAX_VALUE;
    private boolean budgetFilterActive = false;   // only filter price when seekbar touched
    private Set<String> selectedAmenities  = new HashSet<>();
    private Set<String> selectedHouseTypes = new HashSet<>();
    private String searchQuery        = "";
    private String currentNeighborhood = "";

    // ================= LIFECYCLE =================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        receiveUserData();

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        initViews();

        if (savedInstanceState != null) {
            isFilterVisible = savedInstanceState.getBoolean(KEY_FILTER_PANEL_VISIBLE, false);
        }
        setFilterPanelVisible(isFilterVisible);

        registerAllViewsForTranslation();
        setupRecyclerView();
        setupFilterToggle();
        setupBudgetSeekBar();
        setupFilterButtons();
        setupSearchBar();
        setupViewToggle();
        setupNeighborhoodButtons();
        setupLanguageSwitch();
        setupFavoritesButton();
        setupSaveSearchButton();
        setupTooltipsAndFirstTimeHelp();

        db = FirebaseFirestore.getInstance();

        // Load profile for name/greeting only, then fetch ALL listings
        loadProfileThenAllListings();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_FILTER_PANEL_VISIBLE,
                filterDetailsLayout != null
                        && filterDetailsLayout.getVisibility() == View.VISIBLE);
    }

    // ================= RECEIVE USER DATA =================

    private void receiveUserData() {
        if (getIntent() != null) {
            userName              = getIntent().getStringExtra("name");
            userId                = getIntent().getStringExtra("userId");
            preferredNeighborhood = getIntent().getStringExtra("neighborhood");
            phoneNumber           = getIntent().getStringExtra("phoneNumber");
            userLanguage          = getIntent().getStringExtra("language");

            if (userName == null)              userName = "";
            if (userId == null)                userId = "";
            if (preferredNeighborhood == null) preferredNeighborhood = "";
            if (phoneNumber == null)           phoneNumber = "";
            if (userLanguage == null)          userLanguage = "en";
        }

        if (phoneNumber.isEmpty()) phoneNumber = SharedData.getCurrentPhoneNumber();

        if (phoneNumber.isEmpty()) {
            SessionManager session = SessionManager.getInstance(this);
            if (session.isUserLoggedIn()) {
                phoneNumber = session.getPhoneNumber();
                if (userName.isEmpty()) userName = session.getUserName();
                if (userId.isEmpty())   userId   = session.getUserId();
            }
        }
        // NOTE: budget and neighbourhood are intentionally NOT seeded here.
        // All listings must show on load — user applies filters manually.
    }

    // ================= INIT VIEWS =================

    private void initViews() {
        welcomeName         = findViewById(R.id.name);
        searchBar           = findViewById(R.id.search_bar);
        filterButton        = findViewById(R.id.imageFilterButton);
        filterDetailsLayout = findViewById(R.id.filterdetails);
        budgetSeekBar       = findViewById(R.id.seekBar2);
        budgetRangeText     = findViewById(R.id.textView13);

        btnWifi      = findViewById(R.id.btnWifi);
        btnParking   = findViewById(R.id.btnParking);
        btnSecurity  = findViewById(R.id.btnSecurity);
        btnBorehole  = findViewById(R.id.btnBorehole);
        btnStudio    = findViewById(R.id.btnStudio);
        btnBedsitter = findViewById(R.id.btnBedsitter);
        btnOneBed    = findViewById(R.id.btnOneBed);

        listButton       = findViewById(R.id.button);
        mapButton        = findViewById(R.id.button2);
        langataButton    = findViewById(R.id.langatab);
        rongaiButton     = findViewById(R.id.rongaib);
        kahawaButton     = findViewById(R.id.kahawab);
        mapButtonsLayout = findViewById(R.id.mapbuttons);
        resultsCount     = findViewById(R.id.editTextText2);
        recyclerView     = findViewById(R.id.recyclerView);
        languageSwitch   = findViewById(R.id.switchlanguage);
        favoritesButton  = findViewById(R.id.btnOpenFavorites);
        saveSearchButton = findViewById(R.id.btnSaveSearch);

        mapButtonsLayout.setVisibility(View.GONE);
        welcomeName.setText(" " + (userName.isEmpty() ? "Guest" : userName));
    }

    // ================= PROFILE LOADING =================

    /**
     * Loads profile from Firebase for the greeting name ONLY.
     * Budget and neighbourhood from profile are intentionally ignored
     * so ALL listings load without any pre-applied filters.
     */
    private void loadProfileThenAllListings() {
        String phoneKey = getPhoneKey();
        if (phoneKey.isEmpty()) {
            updateWelcomeName();
            loadAllListings();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(phoneKey)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String profileName = snapshot.child("name").getValue(String.class);
                        if (profileName != null && !profileName.trim().isEmpty()) {
                            userName = profileName.trim();
                        }
                    }
                    updateWelcomeName();
                    loadAllListings();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Profile load failed: " + e.getMessage());
                    updateWelcomeName();
                    loadAllListings();
                });
    }

    private void updateWelcomeName() {
        welcomeName.setText(" " + (userName.isEmpty() ? getLocalizedGuestName() : userName));
    }

    private String getPhoneKey() {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return "";
        return phoneNumber.replace("+", "").trim();
    }

    // ================= LOAD ALL LISTINGS FROM FIRESTORE =================

    /**
     * Fetches every document in the "listings" collection with zero
     * server-side filtering. All filtering happens client-side via
     * matchesFilters() only when the user explicitly sets a filter.
     */
    private void loadAllListings() {
        resultsCount.setText("Loading listings...");

        db.collection("listings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allListings.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Listing listing = document.toObject(Listing.class);
                                listing.setId(document.getId());
                                allListings.add(listing);
                                Log.d(TAG, "Loaded: " + listing.getTitle()
                                        + " | location=" + listing.getLocation()
                                        + " | neighbourhood=" + listing.getNeighborhood());
                            } catch (Exception e) {
                                Log.e(TAG, "Parse error: " + e.getMessage());
                            }
                        }
                        Log.d(TAG, "Total loaded: " + allListings.size());
                        applyFiltersAndDisplay();
                    } else {
                        Log.e(TAG, "Firestore error", task.getException());
                        resultsCount.setText("Error loading listings");
                        showToast("Failed to load listings");
                    }
                });
    }

    // ================= FILTER + DISPLAY =================

    /**
     * Applies only the filters the user has explicitly set.
     * On first load all filters are in default/off state so every
     * listing passes through and is shown.
     */
    private void applyFiltersAndDisplay() {
        List<Listing> filtered = new ArrayList<>();
        for (Listing listing : allListings) {
            if (matchesFilters(listing)) filtered.add(listing);
        }
        adapter.setListings(filtered);
        updateResultsCount(filtered.size());
        if (mapFragment != null) mapFragment.setListings(filtered);
        Log.d(TAG, "Displaying " + filtered.size() + " / " + allListings.size() + " listings");
    }

    private boolean matchesFilters(Listing listing) {
        // Neighbourhood — only active when user taps a neighbourhood button
        if (!currentNeighborhood.isEmpty()
                && !currentNeighborhood.equalsIgnoreCase(listing.getNeighborhood())) {
            return false;
        }

        // Budget — only active after the user has moved the seekbar
        if (budgetFilterActive) {
            if (listing.getPrice() < minPrice || listing.getPrice() > maxPrice) {
                return false;
            }
        }

        // House type — only active when at least one type button is selected
        if (!selectedHouseTypes.isEmpty()
                && !selectedHouseTypes.contains(listing.getHouseType().toLowerCase().trim())) {
            return false;
        }

        // Amenities — listing must have ALL selected amenities
        if (!selectedAmenities.isEmpty()) {
            for (String needed : selectedAmenities) {
                boolean found = false;
                for (String has : listing.getAmenities()) {
                    if (has.toLowerCase().trim().equals(needed)) { found = true; break; }
                }
                if (!found) return false;
            }
        }

        // Search text — only active when search bar has input
        if (!searchQuery.isEmpty()) {
            String q = searchQuery;
            return listing.getTitle().toLowerCase().contains(q)
                    || listing.getNeighborhood().toLowerCase().contains(q)
                    || listing.getHouseType().toLowerCase().contains(q);
        }

        return true;
    }

    // ================= RECYCLER =================

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListingsAdapter(this, this::openListingDetails);
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
                        .remove(mapFragment).commit();
                mapFragment = null;
            }
            findViewById(R.id.map_container).setVisibility(View.GONE);

            listButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green));
            listButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            mapButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.transparent));
            mapButton.setTextColor(ContextCompat.getColor(this, R.color.black));

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
            mapButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            listButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.transparent));
            listButton.setTextColor(ContextCompat.getColor(this, R.color.black));

            if (mapFragment == null) {
                mapFragment = new MapFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.map_container, mapFragment)
                        .commitAllowingStateLoss();
            }

            applyFiltersAndDisplay();

            if (!currentNeighborhood.isEmpty()) {
                mapFragment.filterByNeighborhood(currentNeighborhood);
            }
        });
    }

    // ================= NEIGHBOURHOOD BUTTONS =================

    private void setupNeighborhoodButtons() {
        langataButton.setOnClickListener(v -> toggleNeighbourhood("Langata", langataButton));
        rongaiButton.setOnClickListener(v  -> toggleNeighbourhood("Rongai",  rongaiButton));
        kahawaButton.setOnClickListener(v  -> toggleNeighbourhood("Kahawa",  kahawaButton));
    }

    private void toggleNeighbourhood(String name, Button btn) {
        if (currentNeighborhood.equals(name)) {
            currentNeighborhood = "";
            updateNeighborhoodButtons(null);
            if (mapFragment != null) mapFragment.resetToAllListings();
        } else {
            currentNeighborhood = name;
            updateNeighborhoodButtons(btn);
            if (mapFragment != null) mapFragment.filterByNeighborhood(name);
        }
        applyFiltersAndDisplay();
    }

    private void updateNeighborhoodButtons(Button selected) {
        for (Button btn : new Button[]{langataButton, rongaiButton, kahawaButton}) {
            btn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
            btn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
        if (selected != null) {
            selected.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green));
        }
    }

    // ================= FILTER TOGGLE =================

    private void setupFilterToggle() {
        filterButton.setOnClickListener(v -> {
            boolean shouldShow = filterDetailsLayout.getVisibility() != View.VISIBLE;
            setFilterPanelVisible(shouldShow);
        });
    }

    private void setFilterPanelVisible(boolean visible) {
        isFilterVisible = visible;
        if (filterDetailsLayout != null) {
            filterDetailsLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (filterButton != null) {
            filterButton.setBackgroundTintList(ContextCompat.getColorStateList(
                    this, visible ? R.color.green : R.color.yellow));
            filterButton.setColorFilter(ContextCompat.getColor(
                    this, visible ? R.color.white : R.color.black));
        }
    }

    // ================= BUDGET SEEKBAR =================

    private void setupBudgetSeekBar() {
        // Seekbar starts at max (50 000) — visually shows "no restriction"
        budgetSeekBar.setMax(40);
        budgetSeekBar.setProgress(40);
        budgetRangeText.setText(isSwahiliModeEnabled()
                ? "Hakuna kikomo cha bei"
                : "All prices");

        budgetSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                budgetFilterActive = true;
                minPrice = 10000;
                maxPrice = 10000 + (progress * 1000);
                updateBudgetText(minPrice, maxPrice);
                applyFiltersAndDisplay();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                // If dragged back to max, treat as "no budget filter"
                if (budgetSeekBar.getProgress() == budgetSeekBar.getMax()) {
                    budgetFilterActive = false;
                    minPrice = 0;
                    maxPrice = Integer.MAX_VALUE;
                    budgetRangeText.setText(isSwahiliModeEnabled()
                            ? "Hakuna kikomo cha bei"
                            : "All prices");
                    applyFiltersAndDisplay();
                }
            }
        });
    }

    private void updateBudgetText(int min, int max) {
        budgetRangeText.setText(isSwahiliModeEnabled()
                ? "KSh " + min + " hadi " + max
                : "KSh " + min + " - " + max);
    }

    // ================= FILTER BUTTONS =================

    private void setupFilterButtons() {
        setupFilterButton(btnWifi,      "wifi",            selectedAmenities);
        setupFilterButton(btnParking,   "parking",         selectedAmenities);
        setupFilterButton(btnSecurity,  "security lights", selectedAmenities);
        setupFilterButton(btnBorehole,  "borehole water",  selectedAmenities);
        setupFilterButton(btnStudio,    "studio",          selectedHouseTypes);
        setupFilterButton(btnBedsitter, "bedsitter",       selectedHouseTypes);
        setupFilterButton(btnOneBed,    "1 bedroom",       selectedHouseTypes);
    }

    private void setupFilterButton(Button btn, String value, Set<String> set) {
        if (btn == null) return;
        updateFilterButtonStyle(btn, false); // all start inactive
        btn.setOnClickListener(v -> {
            String key = value.toLowerCase();
            boolean nowSelected = !set.contains(key);
            if (nowSelected) set.add(key); else set.remove(key);
            updateFilterButtonStyle(btn, nowSelected);
            applyFiltersAndDisplay();
        });
    }

    /** Yellow = active filter, grey = inactive. */
    private void updateFilterButtonStyle(Button btn, boolean selected) {
        btn.setBackgroundTintList(ContextCompat.getColorStateList(
                this, selected ? R.color.yellow : R.color.grey));
        btn.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    // ================= SEARCH =================

    private void setupSearchBar() {
        searchBar.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) {
                searchQuery = q != null ? q.toLowerCase().trim() : "";
                applyFiltersAndDisplay();
                return true;
            }
            @Override public boolean onQueryTextChange(String t) {
                searchQuery = t != null ? t.toLowerCase().trim() : "";
                applyFiltersAndDisplay();
                return true;
            }
        });
    }

    // ================= FAVORITES =================

    private void setupFavoritesButton() {
        if (favoritesButton == null) return;
        favoritesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, FavoritesActivity.class);
            intent.putExtra(ListingDetailsActivity.EXTRA_USER_ID, resolveUserId());
            startActivity(intent);
        });
    }

    // ================= SAVE SEARCH =================

    private void setupSaveSearchButton() {
        if (saveSearchButton == null) return;
        saveSearchButton.setOnClickListener(v -> {
            String uid = resolveUserId();
            if (uid.isEmpty()) {
                showToast(getString(R.string.saved_search_missing_user));
                return;
            }
            saveCurrentSearch(uid);
        });
    }

    private void saveCurrentSearch(String uid) {
        if (saveSearchButton != null) saveSearchButton.setEnabled(false);

        Map<String, Object> search = new HashMap<>();
        search.put("minPrice",     budgetFilterActive ? minPrice : 0);
        search.put("maxPrice",     budgetFilterActive ? maxPrice : 0);
        search.put("neighborhood", currentNeighborhood);
        search.put("amenities",    new ArrayList<>(selectedAmenities));
        search.put("houseTypes",   new ArrayList<>(selectedHouseTypes));
        search.put("query",        searchQuery);
        search.put("savedAt",      System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("savedSearches")
                .add(search)
                .addOnSuccessListener(ref -> runOnUiThread(() -> {
                    if (saveSearchButton != null) saveSearchButton.setEnabled(true);
                    showToast(getString(R.string.saved_search_success));
                }))
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    if (saveSearchButton != null) saveSearchButton.setEnabled(true);
                    showToast(getString(R.string.saved_search_error));
                    Log.e(TAG, "Save search failed: " + e.getMessage());
                }));
    }

    // ================= TOOLTIPS + FIRST-TIME TIPS =================

    private void setupTooltipsAndFirstTimeHelp() {
        if (filterButton != null)
            TooltipCompat.setTooltipText(filterButton, getString(R.string.tip_filter_button));
        if (favoritesButton != null)
            TooltipCompat.setTooltipText(favoritesButton, getString(R.string.tip_open_favorites));
        if (saveSearchButton != null)
            TooltipCompat.setTooltipText(saveSearchButton, getString(R.string.tip_save_search));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(KEY_TIPS_SHOWN, false)) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.quick_tips_title)
                .setMessage(getString(R.string.quick_tips_message))
                .setPositiveButton(android.R.string.ok, (d, w) ->
                        prefs.edit().putBoolean(KEY_TIPS_SHOWN, true).apply())
                .setOnCancelListener(d ->
                        prefs.edit().putBoolean(KEY_TIPS_SHOWN, true).apply())
                .show();
    }

    // ================= LANGUAGE =================

    private void setupLanguageSwitch() { setupLanguageSwitch(languageSwitch); }

    @Override
    protected void onLanguageChanged(boolean isSwahili) {
        updateWelcomeName();
        if (!budgetFilterActive) {
            budgetRangeText.setText(isSwahili ? "Hakuna kikomo cha bei" : "All prices");
        } else {
            updateBudgetText(minPrice, maxPrice);
        }
        updateResultsCount(adapter != null ? adapter.getItemCount() : 0);
    }

    // ================= MAP FRAGMENT CALLBACK =================

    @Override
    public void onListingSelected(String listingId, String title) {
        if (isMapView) {
            listButton.performClick();
            recyclerView.postDelayed(() -> scrollToListing(listingId), 500);
        } else {
            scrollToListing(listingId);
        }
    }

    private void scrollToListing(String listingId) {
        List<Listing> current = adapter.getListings();
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getId().equals(listingId)) {
                recyclerView.smoothScrollToPosition(i);
                break;
            }
        }
    }

    // ================= OPEN LISTING DETAILS =================

    private void openListingDetails(Listing listing) {
        if (listing == null || listing.getId().trim().isEmpty()) {
            showToast("Could not open listing details");
            return;
        }
        Intent intent = new Intent(this, ListingDetailsActivity.class);
        intent.putExtra(ListingDetailsActivity.EXTRA_LISTING_ID, listing.getId());
        intent.putExtra(ListingDetailsActivity.EXTRA_USER_ID, resolveUserId());
        startActivity(intent);
    }

    // ================= TRANSLATION REGISTRATION =================

    private void registerAllViewsForTranslation() {
        registerForTranslation(findViewById(R.id.textView6),     R.string.home_page_title);
        registerForTranslation(findViewById(R.id.textView11),    R.string.welcome_text);
        registerForTranslation(findViewById(R.id.search_bar),    R.string.search_bar_hint);
        registerForTranslation(findViewById(R.id.textView12),    R.string.home_budget_title);
        registerForTranslation(findViewById(R.id.textView13),    R.string.home_budget_range_default);
        registerForTranslation(findViewById(R.id.textView14),    R.string.home_amenities_title);
        registerForTranslation(findViewById(R.id.btnWifi),       R.string.wifi);
        registerForTranslation(findViewById(R.id.btnParking),    R.string.parking);
        registerForTranslation(findViewById(R.id.btnSecurity),   R.string.security);
        registerForTranslation(findViewById(R.id.btnBorehole),   R.string.borehole);
        registerForTranslation(findViewById(R.id.textView15),    R.string.house_type);
        registerForTranslation(findViewById(R.id.btnStudio),     R.string.studio);
        registerForTranslation(findViewById(R.id.btnBedsitter),  R.string.bedsitter);
        registerForTranslation(findViewById(R.id.btnOneBed),     R.string.onebed);
        registerForTranslation(findViewById(R.id.button),        R.string.list);
        registerForTranslation(findViewById(R.id.button2),       R.string.map);
        registerForTranslation(findViewById(R.id.editTextText2), R.string.results);
        registerForTranslation(findViewById(R.id.btnSaveSearch), R.string.save_this_search);
    }

    // ================= HELPERS =================

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

    private String resolveUserId() {
        if (userId != null && !userId.trim().isEmpty()) return userId;
        String fromSession = SessionManager.getInstance(this).getUserId();
        return fromSession != null ? fromSession : "";
    }

    protected void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}