package com.example.rentanbo;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ListingsAdapter extends RecyclerView.Adapter<ListingsAdapter.ListingViewHolder> {

    private List<Listing> listings = new ArrayList<>();
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onViewDetailsClick(Listing listing);
    }

    public ListingsAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listing, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listings.get(position);
        holder.bind(listing);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    public void setListings(List<Listing> listings) {
        this.listings = listings;
        notifyDataSetChanged();
    }

    class ListingViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgProperty, imgFavorite;
        private TextView txtPrice, txtTitle, txtLocation;
        private MaterialButton btnViewDetails;
        private ImageView wifiIcon, waterIcon, securityIcon;

        ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProperty = itemView.findViewById(R.id.imgProperty);
            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            wifiIcon = itemView.findViewById(R.id.wifiIcon);
            waterIcon = itemView.findViewById(R.id.waterIcon);
            securityIcon = itemView.findViewById(R.id.securityIcon);
        }

        void bind(Listing listing) {
            txtPrice.setText(listing.getFormattedPrice());
            txtTitle.setText(listing.getTitle());
            txtLocation.setText(listing.getNeighborhood());

            // Load image if available
            if (listing.getImages() != null && !listing.getImages().isEmpty()) {
                Glide.with(context)
                        .load(listing.getImages().get(0))
                        .placeholder(R.drawable.bedsitter3)
                        .error(R.drawable.bedsitter3)
                        .into(imgProperty);
            } else {
                imgProperty.setImageResource(R.drawable.bedsitter3);
            }

            // Handle amenities visibility
            if (listing.getAmenities() != null) {
                wifiIcon.setVisibility(listing.getAmenities().contains("WiFi") ? View.VISIBLE : View.GONE);
                waterIcon.setVisibility(listing.getAmenities().contains("Borehole Water") ||
                        listing.getAmenities().contains("Municipal Water") ? View.VISIBLE : View.GONE);
                securityIcon.setVisibility(listing.getAmenities().contains("Security Lights") ||
                        listing.getAmenities().contains("Electric Fence") ? View.VISIBLE : View.GONE);
            }

            // Favorite click
            imgFavorite.setOnClickListener(v -> {
                // Toggle favorite state
                imgFavorite.setSelected(!imgFavorite.isSelected());
                // TODO: Save to user's favorites
            });

            // View details click
            btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetailsClick(listing);
                }
            });
        }
    }
}