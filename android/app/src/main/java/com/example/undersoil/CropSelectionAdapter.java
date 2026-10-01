package com.example.undersoil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CropSelectionAdapter extends RecyclerView.Adapter<CropSelectionAdapter.ViewHolder> {

    private List<String> cropList;
    private Set<String> selectedCrops = new HashSet<>();

    public CropSelectionAdapter(List<String> cropList) {
        this.cropList = cropList;
    }

    public List<String> getSelectedCrops() {
        return new ArrayList<>(selectedCrops);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crop_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String cropName = cropList.get(position);
        holder.cbSelectCrop.setText(cropName);
        holder.cbSelectCrop.setChecked(selectedCrops.contains(cropName));

        holder.cbSelectCrop.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedCrops.add(cropName);
            } else {
                selectedCrops.remove(cropName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cropList != null ? cropList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelectCrop;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectCrop = itemView.findViewById(R.id.cbSelectCrop);
        }
    }
}
