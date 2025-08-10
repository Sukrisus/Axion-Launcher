package com.axion.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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
        
        // Create ripple animation
        Animation rippleAnimation = AnimationUtils.loadAnimation(activity, R.anim.ripple_theme_change);
        
        rippleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Animation started
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                // Apply theme change after animation
                setThemeMode(newMode);
                activity.recreate();
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                // Not used
            }
        });
        
        // Start ripple animation
        rootView.startAnimation(rippleAnimation);
    }
}