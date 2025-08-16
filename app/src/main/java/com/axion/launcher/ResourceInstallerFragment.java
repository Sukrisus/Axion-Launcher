package com.axion.launcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;

public class ResourceInstallerFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private WebViewPagerAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resource_installer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        
        setupViewPager();
        setupTabLayout();
    }

    private void setupViewPager() {
        adapter = new WebViewPagerAdapter(requireActivity());
        viewPager.setAdapter(adapter);
        
        // Disable swipe between tabs to prevent accidental navigation
        viewPager.setUserInputEnabled(false);
    }

    private void setupTabLayout() {
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Addons");
                    break;
                case 1:
                    tab.setText("Textures");
                    break;
                case 2:
                    tab.setText("Maps");
                    break;
            }
        }).attach();
        
        // Set initial tab
        tabLayout.selectTab(tabLayout.getTabAt(0));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the current WebView when returning to the fragment
        if (adapter != null && viewPager != null) {
            WebViewFragment currentFragment = adapter.getFragment(viewPager.getCurrentItem());
            if (currentFragment != null) {
                // You can add refresh logic here if needed
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause WebView when leaving the fragment
        if (adapter != null) {
            for (WebViewFragment fragment : adapter.getAllFragments()) {
                if (fragment != null && fragment.getView() != null) {
                    // WebView will automatically pause when fragment is paused
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up WebView resources
        if (adapter != null) {
            for (WebViewFragment fragment : adapter.getAllFragments()) {
                if (fragment != null) {
                    // WebView will be cleaned up automatically when fragment is destroyed
                }
            }
        }
    }
}