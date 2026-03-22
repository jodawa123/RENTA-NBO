package com.example.rentanbo;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends BaseActivity {

    private FirestoreManager firestoreManager;
    private FavoritesCacheRepository cacheRepository;

    private RecyclerView recyclerFavorites;
    private TextView txtOfflineBanner;
    private View emptyStateContainer;
    private TextView txtEmptyTitle;
    private TextView txtEmptySubtitle;
    private ImageView imgEmpty;
    private ImageButton btnBack;
    private SwitchMaterial languageSwitch;

    private ListingsAdapter adapter;
    private final List<Listing> currentFavorites = new ArrayList<>();

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        firestoreManager = FirestoreManager.getInstance();
        cacheRepository = FavoritesCacheRepository.getInstance(this);

        userId = getIntent().getStringExtra(ListingDetailsActivity.EXTRA_USER_ID);
        if (TextUtils.isEmpty(userId)) {
            userId = SessionManager.getInstance(this).getUserId();
        }

        initViews();
        setupLanguageSwitch(languageSwitch);
        registerViewsForTranslation();
        setupRecycler();
        setupActions();
        applyTooltips();

        loadFavorites();
    }

    private void initViews() {
        recyclerFavorites = findViewById(R.id.recyclerFavorites);
        txtOfflineBanner = findViewById(R.id.txtOfflineBanner);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        txtEmptyTitle = findViewById(R.id.txtFavoritesEmptyTitle);
        txtEmptySubtitle = findViewById(R.id.txtFavoritesEmptySubtitle);
        imgEmpty = findViewById(R.id.imgFavoritesEmpty);
        btnBack = findViewById(R.id.btnFavoritesBack);
        languageSwitch = findViewById(R.id.switchlanguage);
    }

    private void setupRecycler() {
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ListingsAdapter(this, listing -> {
            Intent intent = new Intent(this, ListingDetailsActivity.class);
            intent.putExtra(ListingDetailsActivity.EXTRA_LISTING_ID, listing.getId());
            intent.putExtra(ListingDetailsActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        recyclerFavorites.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeToRemove = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position < 0 || position >= currentFavorites.size()) {
                    return;
                }

                Listing listing = currentFavorites.get(position);
                confirmRemoveFavorite(listing);
            }
        };

        new ItemTouchHelper(swipeToRemove).attachToRecyclerView(recyclerFavorites);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void applyTooltips() {
        ViewCompat.setTooltipText(btnBack, getString(R.string.tip_back));
        ViewCompat.setTooltipText(recyclerFavorites, getString(R.string.tip_swipe_remove_favorite));
    }

    private void registerViewsForTranslation() {
        registerForTranslation(findViewById(R.id.txtFavoritesTitle), R.string.favorites_title);
        registerForTranslation(findViewById(R.id.switchlanguage), R.string.switch_language_en);
        registerForTranslation(findViewById(R.id.txtOfflineBanner), R.string.offline_banner_text);
        registerForTranslation(findViewById(R.id.txtFavoritesEmptyTitle), R.string.favorites_empty_title);
        registerForTranslation(findViewById(R.id.txtFavoritesEmptySubtitle), R.string.favorites_empty_subtitle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        boolean online = isOnline();
        txtOfflineBanner.setVisibility(online ? View.GONE : View.VISIBLE);

        if (!online || TextUtils.isEmpty(userId)) {
            loadCachedFavorites();
            return;
        }

        firestoreManager.getFavoriteListingIds(userId, new FirestoreManager.FavoriteIdsCallback() {
            @Override
            public void onSuccess(List<String> listingIds) {
                if (listingIds == null || listingIds.isEmpty()) {
                    cacheRepository.cacheFavorites(new ArrayList<>());
                    runOnUiThread(() -> renderFavorites(new ArrayList<>()));
                    return;
                }

                firestoreManager.getListingsByIds(listingIds, new FirestoreManager.ListingsCallback() {
                    @Override
                    public void onSuccess(List<Listing> listings) {
                        cacheRepository.cacheFavorites(listings);
                        runOnUiThread(() -> renderFavorites(listings));
                    }

                    @Override
                    public void onFailure(String error) {
                        loadCachedFavorites();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                loadCachedFavorites();
            }
        });
    }

    private void loadCachedFavorites() {
        cacheRepository.getCachedFavorites(listings -> runOnUiThread(() -> renderFavorites(listings)));
    }

    private void renderFavorites(List<Listing> listings) {
        currentFavorites.clear();
        if (listings != null) {
            currentFavorites.addAll(listings);
        }

        adapter.setListings(new ArrayList<>(currentFavorites));
        boolean isEmpty = currentFavorites.isEmpty();
        emptyStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerFavorites.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void confirmRemoveFavorite(Listing listing) {
        if (listing == null) {
            adapter.notifyDataSetChanged();
            return;
        }

        String title = TextUtils.isEmpty(listing.getTitle()) ? getString(R.string.favorites_item_title_fallback) : listing.getTitle();
        new AlertDialog.Builder(this)
                .setTitle(R.string.favorites_remove_title)
                .setMessage(getString(R.string.favorites_remove_message, title))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> adapter.notifyDataSetChanged())
                .setPositiveButton(R.string.favorites_remove_confirm, (dialog, which) -> removeFavorite(listing))
                .setOnCancelListener(dialog -> adapter.notifyDataSetChanged())
                .show();
    }

    private void removeFavorite(Listing listing) {
        if (listing == null || TextUtils.isEmpty(listing.getId())) {
            adapter.notifyDataSetChanged();
            return;
        }

        currentFavorites.remove(listing);
        adapter.setListings(new ArrayList<>(currentFavorites));
        renderFavorites(currentFavorites);

        cacheRepository.removeFavorite(listing.getId());

        if (!TextUtils.isEmpty(userId)) {
            firestoreManager.setFavoriteListing(userId, listing.getId(), false, new FirestoreManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> showToast(getString(R.string.details_removed_favorite)));
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> showToast(getString(R.string.favorites_remove_failed)));
                }
            });
        }
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    @Override
    protected void onLanguageChanged(boolean isSwahili) {
        if (currentFavorites.isEmpty()) {
            txtEmptyTitle.setText(getString(R.string.favorites_empty_title));
            txtEmptySubtitle.setText(getString(R.string.favorites_empty_subtitle));
        }
    }
}

