package com.axion.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<ResourceFile> files;
    private OnFileActionListener actionListener;

    public interface OnFileActionListener {
        void onFileAction(ResourceFile resourceFile);
    }

    public FileAdapter(List<ResourceFile> files, OnFileActionListener actionListener) {
        this.files = files;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file_card, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        ResourceFile resourceFile = files.get(position);
        holder.bind(resourceFile);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void updateFiles(List<ResourceFile> newFiles) {
        this.files = newFiles;
        notifyDataSetChanged();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private TextView fileName;
        private TextView fileDetails;
        private MaterialButton shareButton;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            fileDetails = itemView.findViewById(R.id.file_details);
            shareButton = itemView.findViewById(R.id.share_button);
        }

        public void bind(ResourceFile resourceFile) {
            fileName.setText(resourceFile.getName());
            
            String details = "Downloaded on " + resourceFile.getFormattedDate() + 
                           " • " + resourceFile.getFormattedSize();
            fileDetails.setText(details);

            // Set button enabled/disabled based on file type
            shareButton.setEnabled(resourceFile.isMinecraftFile());

            shareButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onFileAction(resourceFile);
                }
            });
        }
    }
}