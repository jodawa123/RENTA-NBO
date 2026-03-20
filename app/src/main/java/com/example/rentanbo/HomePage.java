package com.example.rentanbo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends BaseActivity {

    private static final String TAG = "HomePage";

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

    // ================= STATE =================
    private ListingsAdapter adapter;
    private FirestoreManager firestoreManager;
    private FilterState filterState;
    private boolean isFilterVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        receiveUserData();
        initCore();
        initViews();
        setupRecyclerView();
        setupFilterToggle();
        setupBudgetSeekBar();
        setupChips();
        setupSearchBar();
        setupViewToggle();
        setupLanguageSwitch();
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

        filterDetailsLayout.setVisibility(View.GONE);
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
        welcomeName.setText(" " + (userName.isEmpty() ? "Guest" : userName));

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
            showToast("Viewing: " + listing.getTitle());
        });

        recyclerView.setAdapter(adapter);
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
        budgetRangeText.setText("KSh " + min + " - " + max);
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

            mapButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green));

            listButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.lightgrey));

            showToast("Map view coming soon");
        });
    }

    // ================= LANGUAGE =================
    private void setupLanguageSwitch() {

        if (languageSwitch == null) return;

        languageSwitch.setChecked("sw".equals(userLanguage));

        languageSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    userLanguage = isChecked ? "sw" : "en";

                    showToast("Language: " +
                            (isChecked ? "Swahili" : "English"));
                });
    }

    // ================= LOAD LISTINGS =================
    private void loadListings() {

        firestoreManager.getFilteredListings(filterState,
                new FirestoreManager.ListingsCallback() {

                    @Override
                    public void onSuccess(List<Listing> listings) {

                        runOnUiThread(() -> {

                            adapter.setListings(
                                    listings != null ? listings : new ArrayList<>());

                            updateResultsCount(
                                    listings != null ? listings.size() : 0);
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
        resultsCount.setText(
                count + (count == 1 ?
                        " result found" : " results found"));
    }
}