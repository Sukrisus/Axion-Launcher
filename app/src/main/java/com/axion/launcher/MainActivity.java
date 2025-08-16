package com.axion.launcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MCPEVersion selectedVersion;
    private ThemeManager themeManager;
    private ApkModifier apkModifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize theme manager and apply saved theme
        themeManager = ThemeManager.getInstance(this);
        themeManager.setThemeMode(themeManager.getCurrentThemeMode());
        
        // Initialize ApkModifier
        apkModifier = new ApkModifier(this);
        
        setContentView(R.layout.activity_main);

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Setup toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Axion Launcher");
        toolbar.setTitleTextColor(getResources().getColor(R.color.black));

        // Setup navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Handle incoming intent
        handleIncomingIntent(getIntent());

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_dashboard);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up ApkModifier to prevent memory leaks
        if (apkModifier != null) {
            apkModifier.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_dashboard) {
            selectedFragment = new DashboardFragment();
        } else if (itemId == R.id.nav_version_manager) {
            selectedFragment = new VersionManagerFragment();
        } else if (itemId == R.id.nav_resource_installer) {
            selectedFragment = new ResourceInstallerFragment();
        } else if (itemId == R.id.nav_resource_manager) {
            selectedFragment = new ResourceManagerFragment();
        } else if (itemId == R.id.nav_tools) {
            selectedFragment = new ToolsFragment();
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
        } else if (itemId == R.id.nav_info) {
            selectedFragment = new InfoFragment();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    public void updateNavigationSelection(int menuItemId) {
        navigationView.setCheckedItem(menuItemId);
    }
    
    public void setSelectedVersion(MCPEVersion version) {
        this.selectedVersion = version;
    }
    
    public MCPEVersion getSelectedVersion() {
        return selectedVersion;
    }
    
    public void toggleTheme() {
        View rootView = findViewById(android.R.id.content);
        themeManager.toggleTheme(this, rootView);
    }
    
    public ApkModifier getApkModifier() {
        return apkModifier;
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String mimeType = intent.getType();
                if (mimeType != null) {
                    saveMinecraftFile(data, mimeType);
                }
            }
        }
    }

    private void saveMinecraftFile(Uri fileUri, String mimeType) {
        try {
            // Determine the target folder based on MIME type
            String targetFolder = getTargetFolderForMimeType(mimeType);
            if (targetFolder == null) {
                Log.w(TAG, "Unsupported MIME type: " + mimeType);
                return;
            }

            // Get the file name from the URI
            String fileName = getFileNameFromUri(fileUri);
            if (fileName == null) {
                fileName = "minecraft_file_" + System.currentTimeMillis();
            }

            // Create target directory
            File baseDir = new File(getExternalFilesDir(null), "resources");
            File targetDir = new File(baseDir, targetFolder);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // Create target file
            File targetFile = new File(targetDir, fileName);

            // Copy the file
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                 OutputStream outputStream = new FileOutputStream(targetFile)) {
                
                if (inputStream == null) {
                    Log.e(TAG, "Failed to open input stream for URI: " + fileUri);
                    return;
                }

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // Show success message
            String message = "File saved to " + targetFolder + " folder";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "File saved successfully: " + targetFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Error saving Minecraft file", e);
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getTargetFolderForMimeType(String mimeType) {
        switch (mimeType) {
            case "application/minecraft-pack":
                return "textures";
            case "application/minecraft-addon":
                return "mods";
            case "application/minecraft-template":
                return "maps";
            default:
                return null;
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            return new File(path).getName();
        }
        return null;
    }
}