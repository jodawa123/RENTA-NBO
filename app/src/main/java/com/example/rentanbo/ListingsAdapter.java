package com.example.rentanbo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ListingsAdapter extends RecyclerView.Adapter<ListingsAdapter.ListingViewHolder> {

    private List<Listing> listings = new ArrayList<>();
    private Context context;
    private OnItemClickListener listener;

    // Optimized Glide options for faster loading
    private static final RequestOptions GLIDE_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .format(DecodeFormat.PREFER_RGB_565)
            .placeholder(R.drawable.bedsitter3)
            .error(R.drawable.bedsitter3)
            .centerCrop();

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
    public List<Listing> getListings() {
        return listings;
    }

    public void setListings(List<Listing> listings) {
        this.listings = listings;
        notifyDataSetChanged();
    }

    class ListingViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgProperty, imgFavorite;
        private TextView txtPrice, txtTitle, txtLocation;
        private MaterialButton btnViewDetails;

        ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProperty = itemView.findViewById(R.id.imgProperty);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }

        void bind(Listing listing) {
            // Set text immediately
            txtPrice.setText(listing.getFormattedPrice());
            txtTitle.setText(listing.getTitle());
            txtLocation.setText(listing.getNeighborhood());

            // Load image directly from HTTPS URL
            if (listing.getImages() != null && !listing.getImages().isEmpty()) {
                String imageUrl = listing.getImages().get(0);

                Glide.with(context)
                        .load(imageUrl)
                        .apply(GLIDE_OPTIONS)
                        .into(imgProperty);
            } else {
                imgProperty.setImageResource(R.drawable.bedsitter3);
            }


            // View details click
            btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetailsClick(listing);
                }
            });
        }
    }
}