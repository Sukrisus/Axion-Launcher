package com.axion.launcher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class ResourceManagerPagerAdapter extends FragmentStateAdapter {

    private List<FileListFragment> fragments;

    public ResourceManagerPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        fragments = new ArrayList<>();
        
        // Initialize fragments for each tab
        fragments.add(FileListFragment.newInstance("mods"));
        fragments.add(FileListFragment.newInstance("textures"));
        fragments.add(FileListFragment.newInstance("maps"));
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

    public FileListFragment getFragment(int position) {
        if (position >= 0 && position < fragments.size()) {
            return fragments.get(position);
        }
        return null;
    }

    public List<FileListFragment> getAllFragments() {
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