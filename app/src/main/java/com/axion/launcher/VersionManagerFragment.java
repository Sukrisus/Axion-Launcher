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
        
        // Set initial tab
        tabLayout.selectTab(tabLayout.getTabAt(0));
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        versionAdapter = new VersionAdapter(new ArrayList<>(), this::onVersionAction);
        recyclerView.setAdapter(versionAdapter);
    }
    
    private void loadVersions() {
        allVersions = new ArrayList<>();
        
        // Release versions
        allVersions.add(new MCPEVersion("1.20.1", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.10", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.12", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.13", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.14", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.15", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.30", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.31", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.32", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.40", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.50", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.60", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.70", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.80", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.20.81", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.22", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.23", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.30", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.31", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.40", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.41", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.43", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.44", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.50", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.51", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.60", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.61", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.62", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.70", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.72", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.80", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.81", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.82", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.90", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.92", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.93", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.94", "Release", "Stable version", "release", false));
        allVersions.add(new MCPEVersion("1.21.100", "Release", "Stable version", "release", false));
        
        // Beta versions (using some of the versions as beta)
        allVersions.add(new MCPEVersion("1.21.110.20", "Beta", "Beta version", "beta", false));
        allVersions.add(new MCPEVersion("1.21.110.23", "Beta", "Beta version", "beta", false));
        allVersions.add(new MCPEVersion("1.21.110.25", "Beta", "Beta version", "beta", false));
        
        // Preview versions (using some newer versions as preview)
        allVersions.add(new MCPEVersion("1.22.0.50", "Preview", "Preview version", "preview", false));
        allVersions.add(new MCPEVersion("1.22.0.40", "Preview", "Preview version", "preview", false));
        allVersions.add(new MCPEVersion("1.22.0.60", "Preview", "Preview version", "preview", false));
        allVersions.add(new MCPEVersion("1.22.0.55", "Preview", "Preview version", "preview", false));
        
        // Check which version is installed
        checkInstalledVersion();
        
        // Debug: Print loaded versions
        System.out.println("Loaded " + allVersions.size() + " versions");
        for (MCPEVersion version : allVersions) {
            System.out.println("Version: " + version.getVersionNumber() + " - " + version.getFilterType());
        }
        
        filterVersions();
    }
    
    private void checkInstalledVersion() {
        try {
            PackageManager pm = requireActivity().getPackageManager();
            String packageName = "com.mojang.minecraftpe";
            
            // Check if Minecraft PE is installed
            pm.getPackageInfo(packageName, 0);
            
            // For demo purposes, let's assume version 1.21.100 is installed
            // In a real app, you'd get this from the package info
            String installedVersion = "1.21.100";
            
            for (MCPEVersion version : allVersions) {
                if (version.getVersionNumber().equals(installedVersion)) {
                    version.setInstalled(true);
                    System.out.println("Marked version " + installedVersion + " as installed");
                    break;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Minecraft PE is not installed
            System.out.println("Minecraft PE is not installed");
        }
    }
    
    private void filterVersions() {
        List<MCPEVersion> filteredVersions = new ArrayList<>();
        for (MCPEVersion version : allVersions) {
            if (version.getFilterType().equals(currentFilter)) {
                filteredVersions.add(version);
            }
        }
        
        // Sort versions in ascending order (latest to oldest)
        filteredVersions.sort((v1, v2) -> {
            String[] parts1 = v1.getVersionNumber().split("\\.");
            String[] parts2 = v2.getVersionNumber().split("\\.");
            
            int maxLength = Math.max(parts1.length, parts2.length);
            
            for (int i = 0; i < maxLength; i++) {
                int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
                int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
                
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }
            return 0;
        });
        
        // Debug: Print filtered and sorted versions
        System.out.println("Filter: " + currentFilter + " - Found " + filteredVersions.size() + " versions");
        for (MCPEVersion version : filteredVersions) {
            System.out.println("Filtered & Sorted: " + version.getVersionNumber() + " (Installed: " + version.isInstalled() + ")");
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
        } else if ("select".equals(action)) {
            // Update the selected version in the dashboard
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.setSelectedVersion(version);
                Toast.makeText(requireContext(), "Selected " + version.getVersionNumber(), Toast.LENGTH_SHORT).show();
                
                // Navigate back to dashboard
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DashboardFragment())
                        .commit();
                activity.updateNavigationSelection(R.id.nav_dashboard);
            }
        }
    }
}