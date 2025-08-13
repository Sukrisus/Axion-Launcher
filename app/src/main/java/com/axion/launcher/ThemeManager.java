package com.axion.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

public class ThemeManager {
    private static final String PREF_NAME = "theme_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int THEME_AMOLED = 3; // Custom AMOLED mode
    
    private static ThemeManager instance;
    private SharedPreferences preferences;
    
    private ThemeManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public void setThemeMode(int themeMode) {
        if (themeMode == THEME_AMOLED) {
            // For AMOLED mode, we use dark mode but with custom colors
            AppCompatDelegate.setDefaultNightMode(THEME_DARK);
            // Set a custom theme attribute for AMOLED
            preferences.edit().putBoolean("amoled_mode", true).apply();
        } else {
            AppCompatDelegate.setDefaultNightMode(themeMode);
            preferences.edit().putBoolean("amoled_mode", false).apply();
        }
        preferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();
    }
    
    public int getCurrentThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, THEME_DARK);
    }
    
    public boolean isDarkMode() {
        int mode = getCurrentThemeMode();
        return mode == THEME_DARK || mode == THEME_AMOLED;
    }
    
    public boolean isAmoledMode() {
        return getCurrentThemeMode() == THEME_AMOLED && preferences.getBoolean("amoled_mode", false);
    }
    
    public void setThemeMode(int themeMode, Activity activity, View rootView) {
        int currentMode = getCurrentThemeMode();
        if (currentMode != themeMode) {
            createSmoothThemeTransition(activity, rootView, themeMode);
        }
    }
    
    private void createSmoothThemeTransition(Activity activity, View rootView, int newMode) {
        // Create overlay view for smooth transition
        View overlay = new View(activity);
        
        // Set initial color based on current theme
        int startColor = getCurrentThemeColor();
        int endColor = getThemeColor(newMode);
        
        overlay.setBackgroundColor(startColor);
        
        // Add overlay to root view
        if (rootView.getParent() instanceof android.view.ViewGroup) {
            android.view.ViewGroup parent = (android.view.ViewGroup) rootView.getParent();
            android.view.ViewGroup.LayoutParams params = new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            parent.addView(overlay, params);
        }
        
        // Create color transition animation
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(startColor, endColor);
        colorAnimator.setDuration(250);
        colorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        colorAnimator.addUpdateListener(animation -> {
            int animatedColor = (int) animation.getAnimatedValue();
            overlay.setBackgroundColor(animatedColor);
        });
        
        // Create fade animation
        ValueAnimator fadeAnimator = ValueAnimator.ofFloat(0f, 1f, 0f);
        fadeAnimator.setDuration(300);
        fadeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        fadeAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            rootView.setAlpha(1f - alpha * 0.3f); // Subtle fade effect
        });
        
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Apply theme change after animation
                setThemeMode(newMode);
                
                // Remove overlay and restore alpha
                if (overlay.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) overlay.getParent()).removeView(overlay);
                }
                rootView.setAlpha(1f);
                
                // Recreate activity for theme change
                activity.recreate();
            }
        });
        
        // Start animations
        colorAnimator.start();
        fadeAnimator.start();
    }
    
    private int getCurrentThemeColor() {
        return getThemeColor(getCurrentThemeMode());
    }
    
    private int getThemeColor(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                return Color.parseColor("#FFFFFF");
            case THEME_DARK:
                return Color.parseColor("#121212");
            case THEME_AMOLED:
                return Color.parseColor("#000000");
            default:
                return Color.parseColor("#121212");
        }
    }
    
    // Helper method to get AMOLED-aware colors
    public int getAmoledAwareColor(Context context, int lightColorRes, int darkColorRes) {
        if (isAmoledMode()) {
            // Return true black for backgrounds in AMOLED mode
            if (lightColorRes == R.color.background_color || 
                lightColorRes == R.color.surface_color || 
                lightColorRes == R.color.card_background) {
                return Color.BLACK;
            }
            // Return slightly lighter black for surface variants
            if (lightColorRes == R.color.surface_variant || 
                lightColorRes == R.color.card_background_variant) {
                return Color.parseColor("#0A0A0A");
            }
        }
        
        // Use normal theme colors
        return ContextCompat.getColor(context, isDarkMode() ? darkColorRes : lightColorRes);
    }
    
    // Legacy method for backward compatibility
    public void toggleTheme(Activity activity, View rootView) {
        int currentMode = getCurrentThemeMode();
        int newMode;
        
        if (currentMode == THEME_LIGHT) {
            newMode = THEME_DARK;
        } else if (currentMode == THEME_DARK) {
            newMode = THEME_AMOLED;
        } else {
            newMode = THEME_LIGHT;
        }
        
        createSmoothThemeTransition(activity, rootView, newMode);
    }
}