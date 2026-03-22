package com.example.rentanbo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends BaseActivity {

    private static final String TAG = "HomePage";
    private static final String KEY_FILTER_PANEL_VISIBLE = "key_filter_panel_visible";
    private static final String KEY_TIPS_SHOWN = "home_tips_shown";

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
    private LinearLayout filterDetailsLayout;
    private SeekBar budgetSeekBar;
    private TextView budgetRangeText;
    private Chip chipWifi, chipParking, chipSecurity, chipBorehole;
    private Chip chipStudio, chipBedsitter, chipOneBed;
    private Button listButton, mapButton;
    private TextView resultsCount;
    private RecyclerView recyclerView;
    private SwitchMaterial languageSwitch;
    private ImageButton favoritesButton;
    private Button saveSearchButton;

    // ================= STATE =================
    private ListingsAdapter adapter;
    private FirestoreManager firestoreManager;
    private FilterState filterState;
    private boolean isFilterVisible = false;
    private MapFragment mapFragment;
    private List<Listing> latestListings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        receiveUserData();
        initCore();
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
        setupLanguageSwitch();
        setupFavoritesButton();
        setupSaveSearchButton();
        setupTooltipsAndFirstTimeHelp();
        loadProfileFiltersAndListings();
    }

    // ================= RECEIVE USER DATA =================
    private void receiveUserData() {

        if (getIntent() != null) {

            userName = getIntent().getStringExtra("name");
            userId = getIntent().getStringExtra("userId");
            preferredNeighborhood = getIntent().getStringExtra("neighborhood");
            phoneNumber = getIntent().getStringExtra("phoneNumber");
            userBudgetMin = getIntent().getIntExtra("budgetMin", 10000);
            userBudgetMax = getIntent().getIntExtra("budgetMax", 50000);
            userLanguage = getIntent().getStringExtra("language");

            if (userName == null) userName = "";
            if (userId == null) userId = "";
            if (preferredNeighborhood == null) preferredNeighborhood = "";
            if (phoneNumber == null) phoneNumber = "";
            if (userLanguage == null) userLanguage = "en";
        }

        if (phoneNumber.isEmpty()) {
            phoneNumber = SharedData.getCurrentPhoneNumber();
        }

        if (phoneNumber.isEmpty()) {
            SessionManager sessionManager = SessionManager.getInstance(this);
            if (sessionManager.isUserLoggedIn()) {
                phoneNumber = sessionManager.getPhoneNumber();
                if (userName.isEmpty()) userName = sessionManager.getUserName();
                if (userId.isEmpty()) userId = sessionManager.getUserId();
                if (preferredNeighborhood.isEmpty()) {
                    preferredNeighborhood = sessionManager.getPreferredNeighborhood();
                }

                if (userBudgetMin <= 0) {
                    userBudgetMin = sessionManager.getBudgetMin();
                }
                if (userBudgetMax <= 0) {
                    userBudgetMax = sessionManager.getBudgetMax();
                }
            }
        }
    }

    // ================= INITIALIZE CORE =================
    private void initCore() {
        firestoreManager = FirestoreManager.getInstance();
        filterState = FilterState.getInstance();

        // Reset singleton state so old session filters do not suppress valid listings.
        filterState.clearFilters();

        filterState.setMinPrice(userBudgetMin);
        filterState.setMaxPrice(userBudgetMax);

        if (!preferredNeighborhood.isEmpty()) {
            filterState.setNeighborhood(preferredNeighborhood);
        }
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
        resultsCount = findViewById(R.id.editTextText2);
        recyclerView = findViewById(R.id.recyclerView);
        languageSwitch = findViewById(R.id.switchlanguage);
        favoritesButton = findViewById(R.id.btnOpenFavorites);
        saveSearchButton = findViewById(R.id.btnSaveSearch);
    }

    private void setupFavoritesButton() {
        if (favoritesButton == null) {
            return;
        }

        favoritesButton.setOnClickListener(v -> {
            String resolvedUserId = userId;
            if (resolvedUserId == null || resolvedUserId.trim().isEmpty()) {
                resolvedUserId = SessionManager.getInstance(this).getUserId();
            }

            Intent favoritesIntent = new Intent(this, FavoritesActivity.class);
            favoritesIntent.putExtra(ListingDetailsActivity.EXTRA_USER_ID, resolvedUserId != null ? resolvedUserId : "");
            startActivity(favoritesIntent);
        });
    }

    private void setupSaveSearchButton() {
        if (saveSearchButton == null) {
            return;
        }

        saveSearchButton.setOnClickListener(v -> {
            String resolvedUserId = userId;
            if (resolvedUserId == null || resolvedUserId.trim().isEmpty()) {
                resolvedUserId = SessionManager.getInstance(this).getUserId();
            }

            if (resolvedUserId == null || resolvedUserId.trim().isEmpty()) {
                showToast(getString(R.string.saved_search_missing_user));
                return;
            }

            saveSearchButton.setEnabled(false);
            firestoreManager.saveSearchCriteria(resolvedUserId, filterState, new FirestoreManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        saveSearchButton.setEnabled(true);
                        showToast(getString(R.string.saved_search_success));
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        saveSearchButton.setEnabled(true);
                        showToast(getString(R.string.saved_search_error));
                    });
                }
            });
        });
    }

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
        boolean tipsShown = prefs.getBoolean(KEY_TIPS_SHOWN, false);
        if (tipsShown) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.quick_tips_title)
                .setMessage(getString(R.string.quick_tips_message))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    prefs.edit().putBoolean(KEY_TIPS_SHOWN, true).apply();
                })
                .setOnCancelListener(dialog -> prefs.edit().putBoolean(KEY_TIPS_SHOWN, true).apply())
                .show();
    }

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
                    showToast("Could not load profile filters");
                    applyProfileAndLoadListings();
                });
    }

    private void applyProfileSnapshot(DataSnapshot snapshot) {
        if (snapshot.exists()) {
            String profileName = snapshot.child("name").getValue(String.class);
            String profileNeighborhood = snapshot.child("preferredNeighborhood").getValue(String.class);
            Long monthlyBudget = snapshot.child("monthlyBudget").getValue(Long.class);

            if (profileName != null && !profileName.trim().isEmpty()) {
                userName = profileName.trim();
            }

            if (profileNeighborhood != null && !profileNeighborhood.trim().isEmpty()) {
                preferredNeighborhood = profileNeighborhood.trim();
            }

            if (monthlyBudget != null) {
                int clampedMax = (int) Math.max(10000, Math.min(50000, monthlyBudget));
                userBudgetMin = 10000;
                userBudgetMax = clampedMax;
            }
        }

        applyProfileAndLoadListings();
    }

    private void applyProfileAndLoadListings() {
        welcomeName.setText(" " + (userName.isEmpty() ? getLocalizedGuestName() : userName));

        filterState.setMinPrice(userBudgetMin);
        filterState.setMaxPrice(userBudgetMax);

        if (!preferredNeighborhood.isEmpty()) {
            filterState.setNeighborhood(preferredNeighborhood);
        }

        setupBudgetSeekBar();
        loadListings();
    }

    private String getPhoneKey() {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "";
        }
        return phoneNumber.replace("+", "").trim();
    }

    // ================= RECYCLER =================
    private void setupRecyclerView() {

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ListingsAdapter(this, listing -> {
            openListingDetails(listing);
        });

        recyclerView.setAdapter(adapter);
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
                    this,
                    visible ? R.color.green : R.color.yellow
            ));
            filterButton.setColorFilter(ContextCompat.getColor(
                    this,
                    visible ? R.color.white : R.color.black
            ));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_FILTER_PANEL_VISIBLE,
                filterDetailsLayout != null && filterDetailsLayout.getVisibility() == View.VISIBLE);
    }

    // ================= BUDGET =================
    private void setupBudgetSeekBar() {

        budgetSeekBar.setMax(40);
        budgetSeekBar.setProgress((userBudgetMax - 10000) / 1000);

        updateBudgetText(userBudgetMin, userBudgetMax);

        budgetSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar, int progress, boolean fromUser) {

                        int max = 10000 + (progress * 1000);

                        filterState.setMinPrice(10000);
                        filterState.setMaxPrice(max);

                        updateBudgetText(10000, max);
                    }

                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        loadListings();
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

        setupChip(chipWifi, "wifi");
        setupChip(chipParking, "parking");
        setupChip(chipSecurity, "security lights");
        setupChip(chipBorehole, "borehole water");

        setupChip(chipStudio, "studio");
        setupChip(chipBedsitter, "bedsitter");
        setupChip(chipOneBed, "1 bedroom");
    }

    private void setupChip(Chip chip, String value) {

        if (chip == null) return;

        chip.setOnClickListener(v -> {

            if (value.equals("studio") ||
                    value.equals("bedsitter") ||
                    value.equals("1 bedroom")) {

                filterState.toggleHouseType(value);

            } else {
                filterState.toggleAmenity(value);
            }

            loadListings();
        });
    }

    // ================= SEARCH =================
    private void setupSearchBar() {

        searchBar.setOnQueryTextListener(
                new android.widget.SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        filterState.setSearchQuery(query != null ? query : "");
                        loadListings();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterState.setSearchQuery(newText != null ? newText : "");
                        loadListings();
                        return true;
                    }
                });
    }

    // ================= VIEW TOGGLE =================
    private void setupViewToggle() {

        listButton.setOnClickListener(v -> {
            recyclerView.setVisibility(View.VISIBLE);

            findViewById(R.id.fragment_container)
                    .setVisibility(View.GONE);

            listButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green));

            mapButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.lightgrey));
        });

        mapButton.setOnClickListener(v -> {
            recyclerView.setVisibility(View.GONE);

            findViewById(R.id.fragment_container)
                    .setVisibility(View.VISIBLE);

            ensureMapFragmentAttached();
            if (mapFragment != null) {
                mapFragment.submitListings(latestListings);
            }

            mapButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green));

            listButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.lightgrey));

        });
    }

    private void ensureMapFragmentAttached() {
        Fragment existing = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (existing instanceof MapFragment) {
            mapFragment = (MapFragment) existing;
            return;
        }

        mapFragment = new MapFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, mapFragment)
                .commit();
    }

    // ================= LANGUAGE =================
    private void setupLanguageSwitch() {
        setupLanguageSwitch(languageSwitch);
    }

    @Override
    protected void onLanguageChanged(boolean isSwahili) {
        welcomeName.setText(" " + (userName.isEmpty() ? getLocalizedGuestName() : userName));
        updateBudgetText(filterState.getMinPrice(), filterState.getMaxPrice());
        updateResultsCount(adapter != null ? adapter.getItemCount() : 0);
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
        registerForTranslation(findViewById(R.id.btnSaveSearch), R.string.save_this_search);
    }

    // ================= LOAD LISTINGS =================
    private void loadListings() {

        firestoreManager.getFilteredListings(filterState,
                new FirestoreManager.ListingsCallback() {

                    @Override
                    public void onSuccess(List<Listing> listings) {

                        runOnUiThread(() -> {

                            latestListings = listings != null
                                    ? new ArrayList<>(listings)
                                    : new ArrayList<>();

                            adapter.setListings(
                                    latestListings);

                            updateResultsCount(
                                    latestListings.size());

                            if (mapFragment != null) {
                                mapFragment.submitListings(latestListings);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() ->
                                showToast("Error loading listings"));
                    }
                });
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

    private void openListingDetails(Listing listing) {
        if (listing == null || listing.getId().trim().isEmpty()) {
            showToast("Could not open listing details");
            return;
        }

        String resolvedUserId = userId;
        if (resolvedUserId == null || resolvedUserId.trim().isEmpty()) {
            resolvedUserId = SessionManager.getInstance(this).getUserId();
        }

        Intent detailsIntent = new Intent(this, ListingDetailsActivity.class);
        detailsIntent.putExtra(ListingDetailsActivity.EXTRA_LISTING_ID, listing.getId());
        detailsIntent.putExtra(ListingDetailsActivity.EXTRA_USER_ID, resolvedUserId != null ? resolvedUserId : "");
        startActivity(detailsIntent);
    }

    private String getLocalizedGuestName() {
        return isSwahiliModeEnabled()
                ? "Mgeni"
                : "Guest";
    }
}