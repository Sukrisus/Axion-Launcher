package com.axion.launcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class ToolsFragment extends Fragment {

    private MaterialButton chunkbaseButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tools, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        chunkbaseButton = view.findViewById(R.id.chunkbase_button);
        
        setupToolButtons();
    }

    private void setupToolButtons() {
        chunkbaseButton.setOnClickListener(v -> {
            // Navigate to Chunkbase WebView
            ChunkbaseWebViewFragment chunkbaseFragment = new ChunkbaseWebViewFragment();
            getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, chunkbaseFragment)
                .addToBackStack(null)
                .commit();
        });
    }
}