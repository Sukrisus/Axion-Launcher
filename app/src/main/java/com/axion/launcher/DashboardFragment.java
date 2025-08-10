package com.axion.launcher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        MaterialButton launchButton = view.findViewById(R.id.launch_button);
        launchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMinecraftPE();
            }
        });
        
        MaterialButton deleteButton = view.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMinecraftPE();
            }
        });
        
        // Make version card clickable to navigate to version manager
        View versionCard = view.findViewById(R.id.version_card);
        versionCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToVersionManager();
            }
        });
        
        // Update version display
        updateVersionDisplay();
    }
    
    private void launchMinecraftPE() {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName("com.mojang.minecraftpe", "com.mojang.minecraftpe.MainActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error launching Minecraft PE", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteMinecraftPE() {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(android.net.Uri.parse("package:com.mojang.minecraftpe"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error deleting Minecraft PE", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateToVersionManager() {
        // Update navigation drawer to show version manager
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new VersionManagerFragment())
                    .commit();
            
            // Update navigation drawer selection
            activity.updateNavigationSelection(R.id.nav_version_manager);
        }
    }
    
    private void updateVersionDisplay() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            MCPEVersion selectedVersion = activity.getSelectedVersion();
            
            if (selectedVersion != null) {
                TextView versionInfo = getView().findViewById(R.id.version_info);
                if (versionInfo != null) {
                    versionInfo.setText("Version: " + selectedVersion.getVersionNumber() + " - " + selectedVersion.getType());
                }
            }
        }
    }
}