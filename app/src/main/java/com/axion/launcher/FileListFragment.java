package com.axion.launcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileListFragment extends Fragment {

    private static final String TAG = "FileListFragment";
    
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private FileAdapter adapter;
    private String section; // "mods", "textures", or "maps"

    public static FileListFragment newInstance(String section) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putString("section", section);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            section = getArguments().getString("section", "mods");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.file_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        
        setupRecyclerView();
        loadFiles();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FileAdapter(new ArrayList<>(), this::onFileAction);
        recyclerView.setAdapter(adapter);
    }

    private void loadFiles() {
        try {
            File baseDir = new File(Environment.getExternalStorageDirectory(), "axion/files/resources");
            File sectionDir = new File(baseDir, section);
            
            List<ResourceFile> files = new ArrayList<>();
            
            if (sectionDir.exists() && sectionDir.isDirectory()) {
                File[] fileArray = sectionDir.listFiles();
                if (fileArray != null) {
                    for (File file : fileArray) {
                        if (file.isFile()) {
                            files.add(new ResourceFile(file, section));
                        }
                    }
                }
            }
            
            // Sort files by last modified date (newest first)
            Collections.sort(files, (f1, f2) -> Long.compare(f2.getLastModified(), f1.getLastModified()));
            
            adapter.updateFiles(files);
            updateEmptyState(files.isEmpty());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading files", e);
            updateEmptyState(true);
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void onFileAction(ResourceFile resourceFile) {
        if (resourceFile.isMinecraftFile()) {
            if (resourceFile.needsZipRemoval()) {
                showZipRemovalDialog(resourceFile);
            } else {
                openInMinecraft(resourceFile);
            }
        } else {
            // For non-Minecraft files, just show a message
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("File Type Not Supported")
                .setMessage("This file type cannot be opened in Minecraft PE.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
        }
    }

    private void showZipRemovalDialog(ResourceFile resourceFile) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove .zip Extension?")
            .setMessage("This file has a .zip extension. Would you like to remove it and open in Minecraft PE?")
            .setPositiveButton("Proceed", (dialog, which) -> {
                removeZipExtension(resourceFile);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void removeZipExtension(ResourceFile resourceFile) {
        try {
            File originalFile = resourceFile.getFile();
            String newName = originalFile.getName().replace(".zip", "");
            File newFile = new File(originalFile.getParent(), newName);
            
            if (originalFile.renameTo(newFile)) {
                // Refresh the file list
                loadFiles();
                // Open the renamed file
                openInMinecraft(new ResourceFile(newFile, section));
            } else {
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Error")
                    .setMessage("Failed to remove .zip extension. Please try again.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing zip extension", e);
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage("An error occurred while processing the file.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
        }
    }

    private void openInMinecraft(ResourceFile resourceFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(resourceFile.getFile()), "application/octet-stream");
            intent.setPackage("com.mojang.minecraftpe");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Minecraft PE Not Found")
                    .setMessage("Minecraft PE is not installed on this device.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening file in Minecraft", e);
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage("Failed to open file in Minecraft PE.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the file list when returning to the fragment
        loadFiles();
    }
}