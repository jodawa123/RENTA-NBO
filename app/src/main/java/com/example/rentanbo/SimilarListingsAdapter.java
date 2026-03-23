package com.example.rentanbo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SimilarListingsAdapter extends RecyclerView.Adapter<SimilarListingsAdapter.SimilarListingViewHolder> {

    public interface OnSimilarListingClickListener {
        void onListingClicked(Listing listing);
    }

    private final List<Listing> listings = new ArrayList<>();
    private final OnSimilarListingClickListener listener;

    public SimilarListingsAdapter(OnSimilarListingClickListener listener) {
        this.listener = listener;
    }

    public void setListings(List<Listing> listingItems) {
        listings.clear();
        if (listingItems != null) {
            listings.addAll(listingItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SimilarListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_similar_listing, parent, false);
        return new SimilarListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SimilarListingViewHolder holder, int position) {
        Listing listing = listings.get(position);
        holder.bind(listing);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    class SimilarListingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView title;
        private final TextView price;
        private final TextView neighborhood;

        SimilarListingViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgSimilarProperty);
            title = itemView.findViewById(R.id.txtSimilarTitle);
            price = itemView.findViewById(R.id.txtSimilarPrice);
            neighborhood = itemView.findViewById(R.id.txtSimilarNeighborhood);
        }

        void bind(Listing listing) {
            title.setText(listing.getTitle());
            price.setText(itemView.getContext().getString(
                    R.string.ksh_price_month_format,
                    listing.getPrice()));
            neighborhood.setText(listing.getNeighborhood());

            if (listing.getImages() != null && !listing.getImages().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(listing.getImages().get(0))
                        .placeholder(R.drawable.bedsitter3)
                        .error(R.drawable.bedsitter3)
                        .into(image);
            } else {
                image.setImageResource(R.drawable.bedsitter3);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onListingClicked(listing);
                }
            });
        }
    }
}

