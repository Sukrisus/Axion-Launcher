package com.axion.launcher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class VersionManagerFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private VersionAdapter versionAdapter;
    private List<MCPEVersion> allVersions;
    private String currentFilter = "release";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_version_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tabLayout = view.findViewById(R.id.tab_layout);
        recyclerView = view.findViewById(R.id.versions_recycler_view);
        
        setupTabLayout();
        setupRecyclerView();
        loadVersions();
    }
    
    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Release"));
        tabLayout.addTab(tabLayout.newTab().setText("Beta"));
        tabLayout.addTab(tabLayout.newTab().setText("Preview"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "release";
                        break;
                    case 1:
                        currentFilter = "beta";
                        break;
                    case 2:
                        currentFilter = "preview";
                        break;
                }
                filterVersions();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        versionAdapter = new VersionAdapter(new ArrayList<>(), this::onVersionAction);
        recyclerView.setAdapter(versionAdapter);
    }
    
    private void loadVersions() {
        allVersions = new ArrayList<>();
        
        // Release versions
        allVersions.add(new MCPEVersion("1.21.72.01", "Release", "Latest stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.50.01", "Release", "Previous stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.40.02", "Release", "Older stable version", "release", false));
        
        // Beta versions
        allVersions.add(new MCPEVersion("1.22.0.50", "Beta", "Latest beta version", "beta", false));
        allVersions.add(new MCPEVersion("1.22.0.40", "Beta", "Previous beta version", "beta", false));
        
        // Preview versions
        allVersions.add(new MCPEVersion("1.22.0.60", "Preview", "Latest preview version", "preview", false));
        allVersions.add(new MCPEVersion("1.22.0.55", "Preview", "Previous preview version", "preview", false));
        
        // Check which version is installed
        checkInstalledVersion();
        
        filterVersions();
    }
    
    private void checkInstalledVersion() {
        try {
            PackageManager pm = requireActivity().getPackageManager();
            String packageName = "com.mojang.minecraftpe";
            
            // Check if Minecraft PE is installed
            pm.getPackageInfo(packageName, 0);
            
            // For demo purposes, let's assume version 1.21.72.01 is installed
            // In a real app, you'd get this from the package info
            String installedVersion = "1.21.72.01";
            
            for (MCPEVersion version : allVersions) {
                if (version.getVersionNumber().equals(installedVersion)) {
                    version.setInstalled(true);
                    break;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Minecraft PE is not installed
        }
    }
    
    private void filterVersions() {
        List<MCPEVersion> filteredVersions = new ArrayList<>();
        for (MCPEVersion version : allVersions) {
            if (version.getType().equals(currentFilter)) {
                filteredVersions.add(version);
            }
        }
        versionAdapter.updateVersions(filteredVersions);
    }
    
    private void onVersionAction(MCPEVersion version, String action) {
        if ("download".equals(action)) {
            Toast.makeText(requireContext(), "Downloading " + version.getVersionNumber(), Toast.LENGTH_SHORT).show();
            // Here you would implement actual download logic
        } else if ("delete".equals(action)) {
            Toast.makeText(requireContext(), "Deleting " + version.getVersionNumber(), Toast.LENGTH_SHORT).show();
            // Here you would implement actual deletion logic
            version.setInstalled(false);
            filterVersions();
        }
    }
}