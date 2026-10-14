package com.example.undersoil;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.undersoil.data.Plant;
import com.example.undersoil.data.PlantVillageData;
import com.example.undersoil.data.ScanLog;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ScanLogAdapter extends RecyclerView.Adapter<ScanLogAdapter.ViewHolder> {

    private static final int THUMBNAIL_SIZE_PX = 256;

    private List<ScanLog> logList;
    private final LruCache<String, Bitmap> thumbnailCache = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    public ScanLogAdapter(List<ScanLog> logList) {
        this.logList = logList;
    }

    public void updateData(List<ScanLog> newList) {
        this.logList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanLog log = logList.get(position);
        holder.tvLogDiagnosis.setText(log.getDiagnosis());
        holder.tvLogConfidence.setText(String.format(Locale.US, "%.1f%% Confidence", log.getConfidence()));
        holder.tvLogDate.setText(log.getDateStr());
        bindThumbnail(holder.ivLogIcon, log);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), LogResultActivity.class);
            intent.putExtra(LogResultActivity.EXTRA_LOG_ID, log.getId());
            v.getContext().startActivity(intent);
        });
    }

    /** Shows the photo saved with the scan, falling back to the crop's catalog picture, then a leaf icon. */
    private void bindThumbnail(ImageView imageView, ScanLog log) {
        imageView.setImageTintList(null);
        imageView.setPadding(0, 0, 0, 0);

        Bitmap photo = loadThumbnail(log.getImagePath());
        if (photo != null) {
            imageView.setImageBitmap(photo);
            return;
        }

        Plant plant = findPlantForCrop(log.getCropName());
        if (plant != null) {
            imageView.setImageResource(plant.getImageResId());
            return;
        }

        int padding = Math.round(14 * imageView.getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setImageResource(R.drawable.ic_leaf_scan);
        imageView.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(imageView.getContext(), R.color.us_bio_green)));
    }

    private Bitmap loadThumbnail(String imagePath) {
        if (imagePath == null || !new File(imagePath).exists()) return null;

        Bitmap cached = thumbnailCache.get(imagePath);
        if (cached != null) return cached;

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int sampleSize = 1;
        while (Math.min(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= THUMBNAIL_SIZE_PX) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        if (bitmap != null) thumbnailCache.put(imagePath, bitmap);
        return bitmap;
    }

    private Plant findPlantForCrop(String cropName) {
        if (cropName == null) return null;
        String crop = cropName.toLowerCase(Locale.US);
        for (Plant plant : PlantVillageData.INSTANCE.getCatalog()) {
            if (crop.contains(plant.getName().toLowerCase(Locale.US))) return plant;
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogDiagnosis;
        TextView tvLogConfidence;
        TextView tvLogDate;
        ImageView ivLogIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogDiagnosis = itemView.findViewById(R.id.tvLogDiagnosis);
            tvLogConfidence = itemView.findViewById(R.id.tvLogConfidence);
            tvLogDate = itemView.findViewById(R.id.tvLogDate);
            ivLogIcon = itemView.findViewById(R.id.ivLogIcon);
        }
    }
}
