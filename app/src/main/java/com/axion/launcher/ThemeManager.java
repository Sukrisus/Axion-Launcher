package com.axion.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREF_NAME = "theme_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    
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
        AppCompatDelegate.setDefaultNightMode(themeMode);
        preferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();
    }
    
    public int getCurrentThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, THEME_DARK);
    }
    
    public boolean isDarkMode() {
        return getCurrentThemeMode() == THEME_DARK;
    }
    
    public void toggleTheme(Activity activity, View rootView) {
        int currentMode = getCurrentThemeMode();
        int newMode = (currentMode == THEME_DARK) ? THEME_LIGHT : THEME_DARK;
        
        // Create smooth fade animation
        createSmoothThemeTransition(activity, rootView, newMode);
    }
    
    private void createSmoothThemeTransition(Activity activity, View rootView, int newMode) {
        // Create overlay view for smooth transition
        View overlay = new View(activity);
        
        // Set initial color based on current theme
        int startColor = isDarkMode() ? Color.parseColor("#121212") : Color.parseColor("#FFFFFF");
        int endColor = (newMode == THEME_DARK) ? Color.parseColor("#121212") : Color.parseColor("#FFFFFF");
        
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
}