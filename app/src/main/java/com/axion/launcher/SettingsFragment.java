package com.axion.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.ImageView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ImageView deleteButton = view.findViewById(R.id.delete_mcpe_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMinecraftPE();
            }
        });
        
        // Initialize all switches with Material You 3 design
        MaterialSwitch autoLaunchSwitch = view.findViewById(R.id.auto_launch_switch);
        MaterialSwitch notificationsSwitch = view.findViewById(R.id.notifications_switch);
        MaterialSwitch themeSwitch = view.findViewById(R.id.theme_switch);
        
        // Set up theme switch
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        themeSwitch.setChecked(themeManager.isDarkMode());
        
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.toggleTheme();
            }
        });
        
        // Set up auto launch switch
        autoLaunchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save auto launch preference
            requireContext().getSharedPreferences("app_preferences", requireContext().MODE_PRIVATE)
                    .edit()
                    .putBoolean("auto_launch", isChecked)
                    .apply();
            
            Toast.makeText(requireContext(), 
                isChecked ? "Auto launch enabled" : "Auto launch disabled", 
                Toast.LENGTH_SHORT).show();
        });
        
        // Set up notifications switch
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save notifications preference
            requireContext().getSharedPreferences("app_preferences", requireContext().MODE_PRIVATE)
                    .edit()
                    .putBoolean("notifications", isChecked)
                    .apply();
            
            Toast.makeText(requireContext(), 
                isChecked ? "Notifications enabled" : "Notifications disabled", 
                Toast.LENGTH_SHORT).show();
        });
        
        // Load saved preferences
        loadSwitchPreferences(autoLaunchSwitch, notificationsSwitch);
        
        // Set up appearance card click
        View appearanceCard = view.findViewById(R.id.appearance_card);
        appearanceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAppearance();
            }
        });
    }
    
    private void loadSwitchPreferences(MaterialSwitch autoLaunchSwitch, MaterialSwitch notificationsSwitch) {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_preferences", requireContext().MODE_PRIVATE);
        
        autoLaunchSwitch.setChecked(prefs.getBoolean("auto_launch", false));
        notificationsSwitch.setChecked(prefs.getBoolean("notifications", true));
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
    
    private void navigateToAppearance() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AppearanceFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }
}