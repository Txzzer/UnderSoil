package com.example.undersoil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.undersoil.data.ScanLog;

import java.util.List;
import java.util.Locale;

public class ScanLogAdapter extends RecyclerView.Adapter<ScanLogAdapter.ViewHolder> {

    private List<ScanLog> logList;

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
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogDiagnosis;
        TextView tvLogConfidence;
        TextView tvLogDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogDiagnosis = itemView.findViewById(R.id.tvLogDiagnosis);
            tvLogConfidence = itemView.findViewById(R.id.tvLogConfidence);
            tvLogDate = itemView.findViewById(R.id.tvLogDate);
        }
    }
}
