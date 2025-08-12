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
        
        // Initialize fragments for each tab
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
}