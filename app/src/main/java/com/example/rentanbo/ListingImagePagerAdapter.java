package com.example.rentanbo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ListingImagePagerAdapter extends RecyclerView.Adapter<ListingImagePagerAdapter.ImageViewHolder> {

    private final List<String> imageUrls = new ArrayList<>();

    public void setImageUrls(List<String> urls) {
        imageUrls.clear();
        if (urls != null) {
            imageUrls.addAll(urls);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.isEmpty() ? "" : imageUrls.get(position);

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            holder.imageView.setImageResource(R.drawable.bedsitter3);
            return;
        }

        Glide.with(holder.imageView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.bedsitter3)
                .error(R.drawable.bedsitter3)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return Math.max(1, imageUrls.size());
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageListing);
        }
    }
}

