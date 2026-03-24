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

import com.google.android.material.chip.Chip;
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
    private int userBudgetMin = 10000;
    private int userBudgetMax = 50000;
    private String userLanguage = "en";

    // ================= UI =================
    private TextView welcomeName;
    private android.widget.SearchView searchBar;
    private ImageButton filterButton;
    private ImageButton favoritesButton;
    private LinearLayout filterDetailsLayout;
    private SeekBar budgetSeekBar;
    private TextView budgetRangeText;
    private Chip chipWifi, chipParking, chipSecurity, chipBorehole;
    private Chip chipStudio, chipBedsitter, chipOneBed;
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

    // Local filter state — single source of truth for both list and map
    private int minPrice = 10000;
    private int maxPrice = 50000;
    private Set<String> selectedAmenities = new HashSet<>();
    private Set<String> selectedHouseTypes = new HashSet<>();
    private String searchQuery = "";
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
        setupChips();
        setupSearchBar();
        setupViewToggle();
        setupNeighborhoodButtons();
        setupLanguageSwitch();
        setupFavoritesButton();
        setupSaveSearchButton();
        setupTooltipsAndFirstTimeHelp();

        db = FirebaseFirestore.getInstance();
        loadProfileFiltersAndListings();
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
            userBudgetMin         = getIntent().getIntExtra("budgetMin", 10000);
            userBudgetMax         = getIntent().getIntExtra("budgetMax", 50000);
            userLanguage          = getIntent().getStringExtra("language");

            if (userName == null)              userName = "";
            if (userId == null)                userId = "";
            if (preferredNeighborhood == null) preferredNeighborhood = "";
            if (phoneNumber == null)           phoneNumber = "";
            if (userLanguage == null)          userLanguage = "en";
        }

        // Fallback 1: SharedData
        if (phoneNumber.isEmpty()) {
            phoneNumber = SharedData.getCurrentPhoneNumber();
        }

        // Fallback 2: SessionManager
        if (phoneNumber.isEmpty()) {
            SessionManager session = SessionManager.getInstance(this);
            if (session.isUserLoggedIn()) {
                phoneNumber = session.getPhoneNumber();
                if (userName.isEmpty())               userName = session.getUserName();
                if (userId.isEmpty())                 userId   = session.getUserId();
                if (preferredNeighborhood.isEmpty())  preferredNeighborhood = session.getPreferredNeighborhood();
                if (userBudgetMin <= 0)               userBudgetMin = session.getBudgetMin();
                if (userBudgetMax <= 0)               userBudgetMax = session.getBudgetMax();
            }
        }

        // Seed local filter state from profile values
        minPrice = userBudgetMin;
        maxPrice = userBudgetMax;
        if (!preferredNeighborhood.isEmpty()) {
            currentNeighborhood = preferredNeighborhood;
        }
    }

    // ================= INIT VIEWS =================

    private void initViews() {
        welcomeName          = findViewById(R.id.name);
        searchBar            = findViewById(R.id.search_bar);
        filterButton         = findViewById(R.id.imageFilterButton);
        filterDetailsLayout  = findViewById(R.id.filterdetails);
        budgetSeekBar        = findViewById(R.id.seekBar2);
        budgetRangeText      = findViewById(R.id.textView13);

        chipWifi      = findViewById(R.id.chip);
        chipParking   = findViewById(R.id.chip2);
        chipSecurity  = findViewById(R.id.chip3);
        chipBorehole  = findViewById(R.id.chip4);
        chipStudio    = findViewById(R.id.chip5);
        chipBedsitter = findViewById(R.id.chip6);
        chipOneBed    = findViewById(R.id.chip7);

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

    private void loadProfileFiltersAndListings() {
        String phoneKey = getPhoneKey();
        if (phoneKey.isEmpty()) {
            applyProfileAndLoadListings();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(phoneKey)
                .get()
                .addOnSuccessListener(this::applyProfileSnapshot)
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Profile load failed: " + e.getMessage());
                    applyProfileAndLoadListings();
                });
    }

    private void applyProfileSnapshot(DataSnapshot snapshot) {
        if (snapshot.exists()) {
            String profileName         = snapshot.child("name").getValue(String.class);
            String profileNeighborhood = snapshot.child("preferredNeighborhood").getValue(String.class);
            Long   monthlyBudget       = snapshot.child("monthlyBudget").getValue(Long.class);

            if (profileName != null && !profileName.trim().isEmpty()) {
                userName = profileName.trim();
            }
            if (profileNeighborhood != null && !profileNeighborhood.trim().isEmpty()) {
                preferredNeighborhood = profileNeighborhood.trim();
            }
            if (monthlyBudget != null) {
                userBudgetMax = (int) Math.max(10000, Math.min(50000, monthlyBudget));
                userBudgetMin = 10000;
            }
        }
        applyProfileAndLoadListings();
    }

    private void applyProfileAndLoadListings() {
        minPrice = userBudgetMin;
        maxPrice = userBudgetMax;
        if (!preferredNeighborhood.isEmpty() && currentNeighborhood.isEmpty()) {
            currentNeighborhood = preferredNeighborhood;
        }

        welcomeName.setText(" " + (userName.isEmpty() ? getLocalizedGuestName() : userName));

        // Sync seekbar to loaded budget values
        if (budgetSeekBar != null) {
            budgetSeekBar.setProgress((maxPrice - 10000) / 1000);
            updateBudgetText(minPrice, maxPrice);
        }

        loadAllListings();
    }

    private String getPhoneKey() {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return "";
        return phoneNumber.replace("+", "").trim();
    }

    // ================= LOAD ALL LISTINGS FROM FIRESTORE =================

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
     * Single source of truth — always call this to refresh both
     * the RecyclerView and the MapFragment together.
     */
    private void applyFiltersAndDisplay() {
        List<Listing> filtered = new ArrayList<>();
        for (Listing listing : allListings) {
            if (matchesFilters(listing)) filtered.add(listing);
        }

        adapter.setListings(filtered);
        updateResultsCount(filtered.size());

        // Core map fix: push filtered data to fragment instead of letting
        // the fragment query Firestore independently
        if (mapFragment != null) {
            mapFragment.setListings(filtered);
        }

        Log.d(TAG, "Displaying " + filtered.size() + " listings");
    }

    private boolean matchesFilters(Listing listing) {
        if (!currentNeighborhood.isEmpty()
                && !currentNeighborhood.equalsIgnoreCase(listing.getNeighborhood())) {
            return false;
        }
        if (listing.getPrice() < minPrice || listing.getPrice() > maxPrice) {
            return false;
        }
        if (!selectedHouseTypes.isEmpty()
                && !selectedHouseTypes.contains(listing.getHouseType().toLowerCase().trim())) {
            return false;
        }
        if (!selectedAmenities.isEmpty()) {
            for (String needed : selectedAmenities) {
                boolean found = false;
                for (String has : listing.getAmenities()) {
                    if (has.toLowerCase().trim().equals(needed)) { found = true; break; }
                }
                if (!found) return false;
            }
        }
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

    // ================= VIEW TOGGLE (List ↔ Map) =================

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
                        .commitAllowingStateLoss();
            }

            // Always push current filtered listings when switching to map view
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

    /** Show/hide filter panel and change filter button tint to signal active state. */
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

    // ================= BUDGET =================

    private void setupBudgetSeekBar() {
        budgetSeekBar.setMax(40);
        budgetSeekBar.setProgress((maxPrice - 10000) / 1000);
        updateBudgetText(minPrice, maxPrice);

        budgetSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxPrice = 10000 + (progress * 1000);
                minPrice = 10000;
                updateBudgetText(minPrice, maxPrice);
                if (fromUser) applyFiltersAndDisplay();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                applyFiltersAndDisplay();
            }
        });
    }

    private void updateBudgetText(int min, int max) {
        budgetRangeText.setText(isSwahiliModeEnabled()
                ? "KSh " + min + " hadi " + max
                : "KSh " + min + " - " + max);
    }

    // ================= CHIPS =================

    private void setupChips() {
        setupChip(chipWifi,      "wifi",            selectedAmenities);
        setupChip(chipParking,   "parking",         selectedAmenities);
        setupChip(chipSecurity,  "security lights", selectedAmenities);
        setupChip(chipBorehole,  "borehole water",  selectedAmenities);
        setupChip(chipStudio,    "studio",          selectedHouseTypes);
        setupChip(chipBedsitter, "bedsitter",       selectedHouseTypes);
        setupChip(chipOneBed,    "1 bedroom",       selectedHouseTypes);
    }

    private void setupChip(Chip chip, String value, Set<String> set) {
        if (chip == null) return;
        updateChipStyle(chip, set.contains(value.toLowerCase()));
        chip.setOnClickListener(v -> {
            String key = value.toLowerCase();
            if (set.contains(key)) { set.remove(key); updateChipStyle(chip, false); }
            else                   { set.add(key);    updateChipStyle(chip, true);  }
            applyFiltersAndDisplay();
        });
    }

    private void updateChipStyle(Chip chip, boolean selected) {
        if (selected) {
            chip.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.yellow));
            chip.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            chip.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.grey));
            chip.setTextColor(getResources().getColor(android.R.color.black));
        }
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
            String resolvedUserId = resolveUserId();
            if (resolvedUserId.isEmpty()) {
                showToast(getString(R.string.saved_search_missing_user));
                return;
            }
            saveCurrentSearch(resolvedUserId);
        });
    }

    /**
     * Saves current filter state directly to Firestore.
     * Uses local filter variables — no FilterState singleton dependency.
     */
    private void saveCurrentSearch(String resolvedUserId) {
        if (saveSearchButton != null) saveSearchButton.setEnabled(false);

        Map<String, Object> search = new HashMap<>();
        search.put("minPrice",     minPrice);
        search.put("maxPrice",     maxPrice);
        search.put("neighborhood", currentNeighborhood);
        search.put("amenities",    new ArrayList<>(selectedAmenities));
        search.put("houseTypes",   new ArrayList<>(selectedHouseTypes));
        search.put("query",        searchQuery);
        search.put("savedAt",      System.currentTimeMillis());

        db.collection("users")
                .document(resolvedUserId)
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
        if (filterButton != null) {
            TooltipCompat.setTooltipText(filterButton, getString(R.string.tip_filter_button));
        }
        if (favoritesButton != null) {
            TooltipCompat.setTooltipText(favoritesButton, getString(R.string.tip_open_favorites));
        }
        if (saveSearchButton != null) {
            TooltipCompat.setTooltipText(saveSearchButton, getString(R.string.tip_save_search));
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(KEY_TIPS_SHOWN, false)) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.quick_tips_title)
                .setMessage(getString(R.string.quick_tips_message))
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        prefs.edit().putBoolean(KEY_TIPS_SHOWN, true).apply())
                .setOnCancelListener(dialog ->
                        prefs.edit().putBoolean(KEY_TIPS_SHOWN, true).apply())
                .show();
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
        registerForTranslation(findViewById(R.id.chip),          R.string.wifi);
        registerForTranslation(findViewById(R.id.chip2),         R.string.parking);
        registerForTranslation(findViewById(R.id.chip3),         R.string.security);
        registerForTranslation(findViewById(R.id.chip4),         R.string.borehole);
        registerForTranslation(findViewById(R.id.textView15),    R.string.house_type);
        registerForTranslation(findViewById(R.id.chip5),         R.string.studio);
        registerForTranslation(findViewById(R.id.chip6),         R.string.bedsitter);
        registerForTranslation(findViewById(R.id.chip7),         R.string.onebed);
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

    /** Returns a non-null userId, falling back to SessionManager. */
    private String resolveUserId() {
        if (userId != null && !userId.trim().isEmpty()) return userId;
        String fromSession = SessionManager.getInstance(this).getUserId();
        return fromSession != null ? fromSession : "";
    }

    protected void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}