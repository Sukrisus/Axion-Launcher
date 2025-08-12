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

import com.google.android.material.button.MaterialButton;
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
    private MaterialButton universalOpenButton;
    private FileAdapter adapter;
    private String section; // "mods", "textures", or "maps"
    private List<ResourceFile> allFiles = new ArrayList<>();

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
        universalOpenButton = view.findViewById(R.id.universal_open_button);
        
        setupRecyclerView();
        setupUniversalOpenButton();
        loadFiles();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FileAdapter(new ArrayList<>(), this::onFileAction);
        recyclerView.setAdapter(adapter);
    }

    private void setupUniversalOpenButton() {
        universalOpenButton.setOnClickListener(v -> openAllFilesInMinecraft());
    }

    private void loadFiles() {
        try {
            File baseDir = new File(Environment.getExternalStorageDirectory(), "axion/files/resources");
            File sectionDir = new File(baseDir, section);
            
            allFiles = new ArrayList<>();
            
            if (sectionDir.exists() && sectionDir.isDirectory()) {
                File[] fileArray = sectionDir.listFiles();
                if (fileArray != null) {
                    for (File file : fileArray) {
                        if (file.isFile()) {
                            allFiles.add(new ResourceFile(file, section));
                        }
                    }
                }
            }
            
            // Sort files by last modified date (newest first)
            Collections.sort(allFiles, (f1, f2) -> Long.compare(f2.getLastModified(), f1.getLastModified()));
            
            adapter.updateFiles(allFiles);
            updateEmptyState(allFiles.isEmpty());
            updateUniversalOpenButton();
            
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

    private void updateUniversalOpenButton() {
        boolean hasMinecraftFiles = allFiles.stream().anyMatch(ResourceFile::isMinecraftFile);
        universalOpenButton.setEnabled(hasMinecraftFiles);
    }

    private void openAllFilesInMinecraft() {
        List<ResourceFile> minecraftFiles = new ArrayList<>();
        List<ResourceFile> zipFiles = new ArrayList<>();
        
        for (ResourceFile file : allFiles) {
            if (file.isMinecraftFile()) {
                if (file.needsZipRemoval()) {
                    zipFiles.add(file);
                } else {
                    minecraftFiles.add(file);
                }
            }
        }
        
        if (zipFiles.isEmpty() && minecraftFiles.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("No Minecraft Files")
                .setMessage("No Minecraft files found in this folder.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
            return;
        }
        
        if (!zipFiles.isEmpty()) {
            showZipRemovalDialogForAll(zipFiles, minecraftFiles);
        } else {
            openFilesInMinecraft(minecraftFiles);
        }
    }

    private void showZipRemovalDialogForAll(List<ResourceFile> zipFiles, List<ResourceFile> normalFiles) {
        StringBuilder message = new StringBuilder();
        message.append("Found ").append(zipFiles.size()).append(" file(s) with .zip extension.\n\n");
        message.append("Would you like to remove the .zip extension and open all files in Minecraft PE?");
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove .zip Extensions?")
            .setMessage(message.toString())
            .setPositiveButton("Proceed", (dialog, which) -> {
                removeZipExtensionsForAll(zipFiles, normalFiles);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void removeZipExtensionsForAll(List<ResourceFile> zipFiles, List<ResourceFile> normalFiles) {
        List<ResourceFile> allFilesToOpen = new ArrayList<>(normalFiles);
        
        for (ResourceFile zipFile : zipFiles) {
            try {
                File originalFile = zipFile.getFile();
                String newName = originalFile.getName().replace(".zip", "");
                File newFile = new File(originalFile.getParent(), newName);
                
                if (originalFile.renameTo(newFile)) {
                    allFilesToOpen.add(new ResourceFile(newFile, section));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing zip extension for: " + zipFile.getName(), e);
            }
        }
        
        // Refresh the file list
        loadFiles();
        
        // Open all files
        openFilesInMinecraft(allFilesToOpen);
    }

    private void openFilesInMinecraft(List<ResourceFile> files) {
        if (files.isEmpty()) {
            return;
        }
        
        // Open the first file, which will trigger Minecraft PE
        openInMinecraft(files.get(0));
        
        // Show a message about the other files
        if (files.size() > 1) {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Files Opened")
                .setMessage(files.size() + " files are being opened in Minecraft PE.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
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
            File file = resourceFile.getFile();
            
            // Use FileProvider to get a proper content URI
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file
            );
            
            // Determine MIME type based on file extension
            String mimeType = getMimeTypeForFile(resourceFile.getExtension());
            
            // Try multiple approaches to open the file
            
            // Approach 1: Direct VIEW intent to Minecraft PE
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.setPackage("com.mojang.minecraftpe");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
                return;
            }
            
            // Approach 2: SEND intent to Minecraft PE
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType(mimeType);
            sendIntent.setPackage("com.mojang.minecraftpe");
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (sendIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(sendIntent);
                return;
            }
            
            // Approach 3: Let system choose the app
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW);
            fallbackIntent.setDataAndType(fileUri, mimeType);
            fallbackIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (fallbackIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(fallbackIntent);
                return;
            }
            
            // Approach 4: Show app chooser
            Intent chooserIntent = Intent.createChooser(fallbackIntent, "Open with");
            startActivity(chooserIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening file in Minecraft", e);
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage("Failed to open file: " + e.getMessage())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
        }
    }

    private String getMimeTypeForFile(String extension) {
        switch (extension.toLowerCase()) {
            case ".mcaddon":
                return "application/minecraft-addon";
            case ".mcpack":
                return "application/minecraft-pack";
            case ".mctemplate":
                return "application/minecraft-template";
            case ".mcaddon.zip":
                return "application/zip";
            case ".mcpack.zip":
                return "application/zip";
            case ".mctemplate.zip":
                return "application/zip";
            default:
                return "application/octet-stream";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the file list when returning to the fragment
        loadFiles();
    }
}