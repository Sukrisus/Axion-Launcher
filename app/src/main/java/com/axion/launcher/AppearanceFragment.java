package com.axion.launcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

public class AppearanceFragment extends Fragment {

    private MaterialCardView lightModeCard;
    private MaterialCardView darkModeCard;
    private MaterialCardView amoledModeCard;
    private RadioButton lightModeRadio;
    private RadioButton darkModeRadio;
    private RadioButton amoledModeRadio;
    private ThemeManager themeManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appearance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ThemeManager
        themeManager = ThemeManager.getInstance(requireContext());
        
        // Initialize views
        lightModeCard = view.findViewById(R.id.light_mode_card);
        darkModeCard = view.findViewById(R.id.dark_mode_card);
        amoledModeCard = view.findViewById(R.id.amoled_mode_card);
        lightModeRadio = view.findViewById(R.id.light_mode_radio);
        darkModeRadio = view.findViewById(R.id.dark_mode_radio);
        amoledModeRadio = view.findViewById(R.id.amoled_mode_radio);
        
        // Set up click listeners
        setupClickListeners();
        
        // Set current theme selection
        updateThemeSelection();
    }

    private void setupClickListeners() {
        lightModeCard.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                themeManager.setThemeMode(ThemeManager.THEME_LIGHT, activity, activity.findViewById(android.R.id.content));
            }
        });

        darkModeCard.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                themeManager.setThemeMode(ThemeManager.THEME_DARK, activity, activity.findViewById(android.R.id.content));
            }
        });

        amoledModeCard.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                themeManager.setThemeMode(ThemeManager.THEME_AMOLED, activity, activity.findViewById(android.R.id.content));
            }
        });

        // Radio button listeners for visual feedback
        lightModeRadio.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                themeManager.setThemeMode(ThemeManager.THEME_LIGHT, activity, activity.findViewById(android.R.id.content));
            }
        });

        darkModeRadio.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                themeManager.setThemeMode(ThemeManager.THEME_DARK, activity, activity.findViewById(android.R.id.content));
            }
        });

        amoledModeRadio.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                themeManager.setThemeMode(ThemeManager.THEME_AMOLED, activity, activity.findViewById(android.R.id.content));
            }
        });
    }

    private void updateThemeSelection() {
        int currentTheme = themeManager.getCurrentThemeMode();
        
        // Uncheck all radio buttons first
        lightModeRadio.setChecked(false);
        darkModeRadio.setChecked(false);
        amoledModeRadio.setChecked(false);
        
        // Check the appropriate radio button
        switch (currentTheme) {
            case ThemeManager.THEME_LIGHT:
                lightModeRadio.setChecked(true);
                break;
            case ThemeManager.THEME_DARK:
                darkModeRadio.setChecked(true);
                break;
            case ThemeManager.THEME_AMOLED:
                amoledModeRadio.setChecked(true);
                break;
            default:
                darkModeRadio.setChecked(true);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update selection when returning to the fragment
        updateThemeSelection();
    }
}