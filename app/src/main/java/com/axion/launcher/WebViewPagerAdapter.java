package com.axion.launcher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class WebViewPagerAdapter extends FragmentStateAdapter {

    private List<WebViewFragment> fragments;

    public WebViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        fragments = new ArrayList<>();
        
        // Initialize fragments for each tab with explicit section URLs
        fragments.add(WebViewFragment.newInstance("https://modbay.org/mods/", "Addons"));
        fragments.add(WebViewFragment.newInstance("https://modbay.org/textures/", "Textures"));
        fragments.add(WebViewFragment.newInstance("https://modbay.org/maps/", "Maps"));
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public WebViewFragment getFragment(int position) {
        if (position >= 0 && position < fragments.size()) {
            return fragments.get(position);
        }
        return null;
    }

    public List<WebViewFragment> getAllFragments() {
        return fragments;
    }

    public String getSectionForPosition(int position) {
        switch (position) {
            case 0:
                return "mods";
            case 1:
                return "textures";
            case 2:
                return "maps";
            default:
                return "mods";
        }
    }
}