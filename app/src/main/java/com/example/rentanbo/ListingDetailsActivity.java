package com.example.rentanbo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListingDetailsActivity extends BaseActivity implements OnMapReadyCallback {

    public static final String EXTRA_LISTING_ID = "listingId";
    public static final String EXTRA_USER_ID = "userId";

    private FirestoreManager firestoreManager;
    private ListingImagePagerAdapter imagePagerAdapter;
    private AmenitiesAdapter amenitiesAdapter;
    private SimilarListingsAdapter similarListingsAdapter;

    private ViewPager2 imagesPager;
    private TextView imageIndex;
    private TextView title;
    private TextView price;
    private TextView neighborhood;
    private TextView description;
    private TextView address;
    private TextView landlordName;
    private TextView landlordPhone;
    private TextView landlordEmail;
    private TextView similarEmpty;
    private ImageButton backButton;
    private ImageButton favoriteButton;
    private ImageButton shareButton;
    private MaterialButton messageButton;
    private MaterialButton callButton;
    private MaterialButton scheduleTourButton;
    private SwitchMaterial languageSwitch;

    private String listingId;
    private String userId;
    private Listing currentListing;
    private boolean isFavorite;
    private GoogleMap googleMap;
    private SimilarListingsUiState similarListingsUiState = SimilarListingsUiState.NONE;

    private enum SimilarListingsUiState {
        NONE,
        EMPTY,
        ERROR
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_details);

        firestoreManager = FirestoreManager.getInstance();

        listingId = getIntent().getStringExtra(EXTRA_LISTING_ID);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (TextUtils.isEmpty(userId)) {
            userId = SessionManager.getInstance(this).getUserId();
        }

        if (TextUtils.isEmpty(listingId)) {
            showToast(getLocalizedText("Missing listing information", "Taarifa za nyumba hazijapatikana"));
            finish();
            return;
        }

        initViews();
        setupAdapters();
        setupMap();
        setupLanguageSwitch();
        registerViewsForTranslation();
        setupActions();
        loadListingDetails();
    }

    private void initViews() {
        imagesPager = findViewById(R.id.vpListingImages);
        imageIndex = findViewById(R.id.txtImageIndex);
        title = findViewById(R.id.txtDetailsTitle);
        price = findViewById(R.id.txtDetailsPrice);
        neighborhood = findViewById(R.id.txtDetailsNeighborhood);
        description = findViewById(R.id.txtDetailsDescription);
        address = findViewById(R.id.txtDetailsAddress);
        landlordName = findViewById(R.id.txtLandlordName);
        landlordPhone = findViewById(R.id.txtLandlordPhone);
        landlordEmail = findViewById(R.id.txtLandlordEmail);
        similarEmpty = findViewById(R.id.txtSimilarEmpty);

        backButton = findViewById(R.id.btnBack);
        favoriteButton = findViewById(R.id.btnFavorite);
        shareButton = findViewById(R.id.btnShare);
        messageButton = findViewById(R.id.btnMessageLandlord);
        callButton = findViewById(R.id.btnCallLandlord);
        scheduleTourButton = findViewById(R.id.btnScheduleTour);
        languageSwitch = findViewById(R.id.switchlanguage);
    }

    private void setupLanguageSwitch() {
        setupLanguageSwitch(languageSwitch);
    }

    private void registerViewsForTranslation() {
        registerForTranslation(findViewById(R.id.switchlanguage), R.string.switch_language_en);
        registerForTranslation(findViewById(R.id.txtDescriptionLabel), R.string.details_description);
        registerForTranslation(findViewById(R.id.txtAddressLabel), R.string.details_address);
        registerForTranslation(findViewById(R.id.txtAmenitiesLabel), R.string.details_amenities);
        registerForTranslation(findViewById(R.id.txtMapLabel), R.string.details_location);
        registerForTranslation(findViewById(R.id.btnMessageLandlord), R.string.details_message_landlord);
        registerForTranslation(findViewById(R.id.btnCallLandlord), R.string.details_call_landlord);
        registerForTranslation(findViewById(R.id.btnScheduleTour), R.string.details_schedule_tour);
        registerForTranslation(findViewById(R.id.txtSimilarLabel), R.string.details_similar_listings);
    }

    private void setupAdapters() {
        imagePagerAdapter = new ListingImagePagerAdapter();
        imagesPager.setAdapter(imagePagerAdapter);
        imagesPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateImageCounter(position, imagePagerAdapter.getItemCount());
            }
        });

        RecyclerView amenitiesRecycler = findViewById(R.id.recyclerAmenities);
        amenitiesRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        amenitiesAdapter = new AmenitiesAdapter();
        amenitiesRecycler.setAdapter(amenitiesAdapter);

        RecyclerView similarRecycler = findViewById(R.id.recyclerSimilarListings);
        similarRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        similarListingsAdapter = new SimilarListingsAdapter(listing -> {
            Intent intent = new Intent(this, ListingDetailsActivity.class);
            intent.putExtra(EXTRA_LISTING_ID, listing.getId());
            intent.putExtra(EXTRA_USER_ID, userId);
            startActivity(intent);
        });
        similarRecycler.setAdapter(similarListingsAdapter);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.listingMiniMap);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.listingMiniMap, mapFragment)
                    .commitNow();
        }

        mapFragment.getMapAsync(this);
    }

    private void setupActions() {
        backButton.setOnClickListener(v -> finish());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        shareButton.setOnClickListener(v -> shareListing());
        messageButton.setOnClickListener(v -> messageLandlord());
        callButton.setOnClickListener(v -> callLandlord());
        scheduleTourButton.setOnClickListener(v -> showVirtualTourDialog());
    }

    private void loadListingDetails() {
        firestoreManager.getListingById(listingId,
                listing -> runOnUiThread(() -> {
                    currentListing = listing;
                    bindListing();
                    loadFavoriteState();
                    loadSimilarListings();
                    renderMap();
                }),
                e -> runOnUiThread(() -> {
                    showToast(getLocalizedText("Could not load listing details", "Imeshindikana kupakia maelezo ya nyumba"));
                    finish();
                }));
    }

    private void bindListing() {
        if (currentListing == null) {
            return;
        }

        title.setText(currentListing.getTitle());
        neighborhood.setText(currentListing.getNeighborhood());
        price.setText(getString(R.string.ksh_price_month_format, currentListing.getPrice()));

        String descriptionText = currentListing.getDescription();
        if (TextUtils.isEmpty(descriptionText)) {
            descriptionText = getLocalizedText(
                    getString(R.string.details_default_description),
                    "Nyumba hii iko eneo zuri kwa wanafunzi, ina huduma muhimu, na usafiri ni rahisi kupatikana."
            );
        }
        description.setText(descriptionText);

        String addressText = currentListing.getPhysicalAddress();
        if (TextUtils.isEmpty(addressText)) {
            addressText = isSwahiliModeEnabled()
                    ? "Karibu na " + currentListing.getNeighborhood() + ", Nairobi"
                    : getString(R.string.details_default_address, currentListing.getNeighborhood());
        }
        address.setText(addressText);

        Listing.Landlord landlord = currentListing.getLandlord();
        landlordName.setText(landlord != null && !TextUtils.isEmpty(landlord.getName())
                ? landlord.getName()
                : getLocalizedText("Landlord details unavailable", "Taarifa za mwenye nyumba hazipatikani"));
        landlordPhone.setText(landlord != null && !TextUtils.isEmpty(landlord.getPhone())
                ? landlord.getPhone()
                : getLocalizedText("Phone number unavailable", "Nambari ya simu haipatikani"));
        landlordEmail.setText(landlord != null && !TextUtils.isEmpty(landlord.getEmail())
                ? landlord.getEmail()
                : getLocalizedText("Email unavailable", "Barua pepe haipatikani"));

        List<String> images = currentListing.getImages();
        imagePagerAdapter.setImageUrls(images != null ? images : new ArrayList<>());
        updateImageCounter(0, imagePagerAdapter.getItemCount());

        amenitiesAdapter.setAmenities(currentListing.getAmenities());
    }

    private void loadFavoriteState() {
        if (TextUtils.isEmpty(userId) || currentListing == null) {
            setFavoriteUiState(false);
            return;
        }

        firestoreManager.isListingFavorite(userId, currentListing.getId(), new FirestoreManager.FavoriteStatusCallback() {
            @Override
            public void onResult(boolean favorite) {
                runOnUiThread(() -> setFavoriteUiState(favorite));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> setFavoriteUiState(false));
            }
        });
    }

    private void toggleFavorite() {
        if (currentListing == null || TextUtils.isEmpty(userId)) {
            showToast(getLocalizedText("Missing user profile for favorites", "Wasifu wa mtumiaji haujapatikana kwa favorites"));
            return;
        }

        favoriteButton.setEnabled(false);
        boolean nextState = !isFavorite;

        firestoreManager.setFavoriteListing(userId, currentListing.getId(), nextState, new FirestoreManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    favoriteButton.setEnabled(true);
                    setFavoriteUiState(nextState);
                    FavoritesCacheRepository cacheRepository = FavoritesCacheRepository.getInstance(ListingDetailsActivity.this);
                    if (nextState) {
                        cacheRepository.upsertFavorite(currentListing);
                    } else {
                        cacheRepository.removeFavorite(currentListing.getId());
                    }
                    showToast(nextState
                            ? getString(R.string.details_saved_favorite)
                            : getString(R.string.details_removed_favorite));
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    favoriteButton.setEnabled(true);
                    showToast(getLocalizedText("Could not update favorites", "Imeshindikana kusasisha favorites"));
                });
            }
        });
    }

    private void setFavoriteUiState(boolean favorite) {
        isFavorite = favorite;
        int tintColor = favorite ? R.color.green : R.color.grey;
        favoriteButton.setColorFilter(ContextCompat.getColor(this, tintColor));
    }

    private void loadSimilarListings() {
        if (currentListing == null) {
            return;
        }

        firestoreManager.getSimilarListings(currentListing, 10, new FirestoreManager.ListingsCallback() {
            @Override
            public void onSuccess(List<Listing> listings) {
                runOnUiThread(() -> {
                    similarListingsAdapter.setListings(listings);
                    similarListingsUiState = (listings == null || listings.isEmpty())
                            ? SimilarListingsUiState.EMPTY
                            : SimilarListingsUiState.NONE;
                    updateSimilarListingsEmptyState();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    similarListingsUiState = SimilarListingsUiState.ERROR;
                    updateSimilarListingsEmptyState();
                });
            }
        });
    }

    private void shareListing() {
        if (currentListing == null) {
            return;
        }

        String shareLink = "https://renta-nbo.app/listings/" + currentListing.getId();
        String message = getString(
                R.string.details_share_message,
                currentListing.getTitle(),
                getString(R.string.ksh_price_month_format, currentListing.getPrice()),
                currentListing.getNeighborhood(),
                shareLink
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentListing.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.details_share_chooser_title)));
    }

    private void messageLandlord() {
        String phone = getLandlordPhone();
        if (TextUtils.isEmpty(phone)) {
            showToast(getLocalizedText("Phone number unavailable", "Nambari ya simu haipatikani"));
            return;
        }

        String message = getString(R.string.details_sms_template, currentListing != null ? currentListing.getTitle() : "this listing");
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + phone));
        smsIntent.putExtra("sms_body", message);
        startActivity(smsIntent);
    }

    private void callLandlord() {
        String phone = getLandlordPhone();
        if (TextUtils.isEmpty(phone)) {
            showToast(getLocalizedText("Phone number unavailable", "Nambari ya simu haipatikani"));
            return;
        }

        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phone));
        startActivity(dialIntent);
    }

    private void showVirtualTourDialog() {
        if (currentListing == null) {
            return;
        }

        final String[] slots = getResources().getStringArray(R.array.virtual_tour_slots);
        final int[] selectedIndex = {0};

        new AlertDialog.Builder(this)
                .setTitle(R.string.details_schedule_tour)
                .setSingleChoiceItems(slots, selectedIndex[0], (dialog, which) -> selectedIndex[0] = which)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.details_schedule_now, (dialog, which) -> {
                    String chosenSlot = slots[Math.max(0, Math.min(selectedIndex[0], slots.length - 1))];
                    firestoreManager.saveVirtualTourRequest(userId, currentListing, chosenSlot, new FirestoreManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> showToast(getString(R.string.details_tour_requested, chosenSlot)));
                        }

                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> showToast(getLocalizedText("Could not request tour", "Imeshindikana kuomba ziara")));
                        }
                    });
                })
                .show();
    }

    private void updateSimilarListingsEmptyState() {
        switch (similarListingsUiState) {
            case EMPTY:
                similarEmpty.setText(getLocalizedText(
                        getString(R.string.details_no_similar_listings),
                        "Hakuna nyumba zinazofanana zilizopatikana."
                ));
                break;
            case ERROR:
                similarEmpty.setText(getLocalizedText(
                        getString(R.string.details_similar_load_error),
                        "Imeshindikana kupakia nyumba zinazofanana."
                ));
                break;
            case NONE:
            default:
                similarEmpty.setText("");
                break;
        }
    }

    private String getLocalizedText(String englishText, String swahiliText) {
        return isSwahiliModeEnabled() ? swahiliText : englishText;
    }

    private boolean isSwahiliModeEnabled() {
        return translationManager != null && translationManager.isSwahiliMode();
    }

    @Override
    protected void onLanguageChanged(boolean isSwahili) {
        if (currentListing != null) {
            bindListing();
        }
        updateSimilarListingsEmptyState();
    }

    private String getLandlordPhone() {
        if (currentListing == null || currentListing.getLandlord() == null) {
            return "";
        }
        return currentListing.getLandlord().getPhone() != null
                ? currentListing.getLandlord().getPhone().trim()
                : "";
    }

    private void updateImageCounter(int index, int total) {
        imageIndex.setText(String.format(Locale.US, "%d/%d", Math.max(1, index + 1), Math.max(1, total)));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        renderMap();
    }

    private void renderMap() {
        if (googleMap == null || currentListing == null) {
            return;
        }

        LatLng location = getListingLatLng(currentListing);
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(currentListing.getTitle())
                .snippet(getString(
                        R.string.ksh_price_month_format,
                        currentListing.getPrice()) + " | " + currentListing.getNeighborhood()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 14f));
    }

    private LatLng getListingLatLng(Listing listing) {
        if (listing.getLocation() != null) {
            return new LatLng(listing.getLocation().getLatitude(), listing.getLocation().getLongitude());
        }

        String area = listing.getNeighborhood() == null ? "" : listing.getNeighborhood().toLowerCase(Locale.US);
        switch (area) {
            case "rongai":
                return new LatLng(-1.3521, 36.8090);
            case "kahawa":
                return new LatLng(-1.2400, 36.9100);
            case "langata":
                return new LatLng(-1.3667, 36.7667);
            default:
                return new LatLng(-1.2866, 36.8172);
        }
    }
}

