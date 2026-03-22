package com.example.rentanbo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AmenitiesAdapter extends RecyclerView.Adapter<AmenitiesAdapter.AmenityViewHolder> {

	private final List<String> amenities = new ArrayList<>();

	public void setAmenities(List<String> amenityItems) {
		amenities.clear();
		if (amenityItems != null) {
			amenities.addAll(amenityItems);
		}
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public AmenityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_amenity, parent, false);
		return new AmenityViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull AmenityViewHolder holder, int position) {
		String amenity = amenities.get(position);
		holder.name.setText(amenity);
		holder.icon.setImageResource(getAmenityIcon(amenity));
	}

	@Override
	public int getItemCount() {
		return amenities.size();
	}

	private int getAmenityIcon(String amenity) {
		String value = amenity == null ? "" : amenity.toLowerCase(Locale.US);

		if (value.contains("wifi") || value.contains("wi-fi") || value.contains("internet")) {
			return R.drawable.outline_android_wifi_3_bar_24;
		}
		if (value.contains("water") || value.contains("borehole")) {
			return R.drawable.outline_humidity_low_24;
		}
		if (value.contains("security") || value.contains("fence") || value.contains("guard")) {
			return R.drawable.outline_security_24;
		}
		if (value.contains("parking")) {
			return R.drawable.outline_atr_24;
		}
		return R.drawable.outline_captive_portal_24;
	}

	static class AmenityViewHolder extends RecyclerView.ViewHolder {
		private final ImageView icon;
		private final TextView name;

		AmenityViewHolder(@NonNull View itemView) {
			super(itemView);
			icon = itemView.findViewById(R.id.imgAmenityIcon);
			name = itemView.findViewById(R.id.txtAmenityName);
		}
	}
}

