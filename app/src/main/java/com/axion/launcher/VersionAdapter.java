package com.axion.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.VersionViewHolder> {

    private List<MCPEVersion> versions;
    private OnVersionActionListener actionListener;

    public interface OnVersionActionListener {
        void onVersionAction(MCPEVersion version, String action);
    }

    public VersionAdapter(List<MCPEVersion> versions, OnVersionActionListener actionListener) {
        this.versions = versions;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public VersionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_version, parent, false);
        return new VersionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VersionViewHolder holder, int position) {
        MCPEVersion version = versions.get(position);
        holder.bind(version);
        
        // Add fast staggered slide-in-left animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationX(-50f);
        
        holder.itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(150)
                .setStartDelay(position * 15) // Much faster staggered delay
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .start();
    }

    @Override
    public int getItemCount() {
        return versions.size();
    }

    public void updateVersions(List<MCPEVersion> newVersions) {
        this.versions = newVersions;
        System.out.println("Adapter: Updating with " + newVersions.size() + " versions");
        notifyDataSetChanged();
    }

    class VersionViewHolder extends RecyclerView.ViewHolder {
        private TextView versionNumber;
        private TextView versionType;
        private TextView versionDescription;
        private MaterialButton actionButton;

        public VersionViewHolder(@NonNull View itemView) {
            super(itemView);
            versionNumber = itemView.findViewById(R.id.version_number);
            versionType = itemView.findViewById(R.id.version_type);
            versionDescription = itemView.findViewById(R.id.version_description);
            actionButton = itemView.findViewById(R.id.action_button);
        }

        public void bind(MCPEVersion version) {
            versionNumber.setText(version.getVersionNumber());
            versionType.setText(version.getType());
            versionDescription.setText(version.getDescription());

            if (version.isInstalled()) {
                actionButton.setText("Delete");
                actionButton.setBackgroundTintList(itemView.getContext().getColorStateList(R.color.error_color));
                actionButton.setOnClickListener(v -> actionListener.onVersionAction(version, "delete"));
            } else {
                actionButton.setText("Download");
                actionButton.setBackgroundTintList(itemView.getContext().getColorStateList(R.color.primary_color));
                actionButton.setOnClickListener(v -> actionListener.onVersionAction(version, "download"));
            }
            
            // Make the entire card clickable for version selection
            itemView.setOnClickListener(v -> actionListener.onVersionAction(version, "select"));
        }
    }
}