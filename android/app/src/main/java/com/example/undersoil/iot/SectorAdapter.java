package com.example.undersoil.iot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.undersoil.R;
import com.example.undersoil.data.model.Sector;

import java.util.List;

public class SectorAdapter extends RecyclerView.Adapter<SectorAdapter.ViewHolder> {

    public interface OnSectorClickListener {
        void onSectorClick(Sector sector);
    }

    private List<Sector> sectorList;
    private OnSectorClickListener listener;

    public SectorAdapter(List<Sector> sectorList, OnSectorClickListener listener) {
        this.sectorList = sectorList;
        this.listener = listener;
    }

    public void updateData(List<Sector> newList) {
        this.sectorList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sector, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sector sector = sectorList.get(position);
        holder.tvSectorName.setText(sector.getName());
        holder.tvSectorCrops.setText(String.format("Crops: %s", sector.getCropNames()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSectorClick(sector);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sectorList != null ? sectorList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectorName;
        TextView tvSectorCrops;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSectorName = itemView.findViewById(R.id.tvSectorName);
            tvSectorCrops = itemView.findViewById(R.id.tvSectorCrops);
        }
    }
}
