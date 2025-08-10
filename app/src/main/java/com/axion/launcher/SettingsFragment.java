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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        MaterialButton deleteButton = view.findViewById(R.id.delete_mcpe_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMinecraftPE();
            }
        });
        
        SwitchMaterial themeSwitch = view.findViewById(R.id.theme_switch);
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        themeSwitch.setChecked(themeManager.isDarkMode());
        
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.toggleTheme();
            }
        });
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
}