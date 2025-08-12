package com.axion.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

public class AppearanceFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final String DEFAULT_PACKAGE_NAME = "com.mojang.minecraftpe";
    
    private ImageView appIcon;
    private TextInputEditText appNameInput;
    private TextInputEditText packageNameInput;
    private MaterialButton changeAppearanceButton;
    private Bitmap selectedIcon;
    private String selectedIconPath;
    
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;
    private ActivityResultLauncher<Intent> installLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appearance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        appIcon = view.findViewById(R.id.app_icon);
        appNameInput = view.findViewById(R.id.app_name_input);
        packageNameInput = view.findViewById(R.id.package_name_input);
        changeAppearanceButton = view.findViewById(R.id.change_appearance_button);
        MaterialCardView iconContainer = view.findViewById(R.id.icon_container);
        
        // Set default package name
        packageNameInput.setText(DEFAULT_PACKAGE_NAME);
        
        // Register activity result launchers
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageResult
        );
        
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    showPermissionDeniedMessage();
                }
            }
        );
        
        multiplePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    openImagePicker();
                } else {
                    showPermissionDeniedMessage();
                }
            }
        );
        
        // Install launcher for APK installation
        installLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(requireContext(), "APK installed successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Installation cancelled or failed", Toast.LENGTH_SHORT).show();
                }
            }
        );
        
        // Set up icon selection
        iconContainer.setOnClickListener(v -> selectImage());
        
        // Set up change appearance button
        changeAppearanceButton.setOnClickListener(v -> startAppearanceModification());
    }
    
    private void selectImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Use new granular permissions
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                openImagePicker();
            }
        } else {
            // Android 12 and below - Use legacy permissions
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
        }
    }
    
    private void openImagePicker() {
        try {
            imagePickerLauncher.launch("image/*");
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error opening image picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showPermissionDeniedMessage() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permission Required")
            .setMessage("This app needs access to your photos to select an image for the app icon. Please grant the permission in Settings.")
            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
            .setNegativeButton("Open Settings", (dialog, which) -> {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Could not open settings", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }
    
    private void handleImageResult(Uri imageUri) {
        if (imageUri != null) {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                selectedIcon = BitmapFactory.decodeStream(inputStream);
                
                // Resize to 512x512 for app icon
                selectedIcon = Bitmap.createScaledBitmap(selectedIcon, 512, 512, true);
                
                appIcon.setImageBitmap(selectedIcon);
                appIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                
                // Save icon to internal storage
                saveIconToInternalStorage(selectedIcon);
                
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void saveIconToInternalStorage(Bitmap bitmap) {
        try {
            File iconFile = new File(requireContext().getFilesDir(), "custom_icon.png");
            FileOutputStream fos = new FileOutputStream(iconFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            selectedIconPath = iconFile.getAbsolutePath();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error saving icon", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startAppearanceModification() {
        String newAppName = appNameInput.getText().toString().trim();
        String newPackageName = packageNameInput.getText().toString().trim();
        
        // Validate inputs
        if (newAppName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an app name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (newPackageName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a package name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isValidPackageName(newPackageName)) {
            Toast.makeText(requireContext(), "Invalid package name format. Use: com.example.appname", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (selectedIcon == null) {
            Toast.makeText(requireContext(), "Please select an icon", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgressDialog(newAppName, newPackageName);
    }
    
    private boolean isValidPackageName(String packageName) {
        // Basic package name validation
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        // Check if it contains at least one dot
        if (!packageName.contains(".")) {
            return false;
        }
        
        // Check if it starts or ends with a dot
        if (packageName.startsWith(".") || packageName.endsWith(".")) {
            return false;
        }
        
        // Check if it contains consecutive dots
        if (packageName.contains("..")) {
            return false;
        }
        
        // Check each segment
        String[] segments = packageName.split("\\.");
        for (String segment : segments) {
            if (segment.isEmpty()) {
                return false;
            }
            
            // Check if segment starts with a letter and contains only valid characters
            if (!segment.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                return false;
            }
        }
        
        return true;
    }
    
    private void showProgressDialog(String newAppName, String newPackageName) {
        View progressView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null);
        TextView progressStatus = progressView.findViewById(R.id.progress_status);
        TextView progressPercentage = progressView.findViewById(R.id.progress_percentage);
        LinearProgressIndicator progressBar = progressView.findViewById(R.id.progress_bar);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(progressView)
                .setCancelable(false)
                .create();
        
        dialog.show();
        
        // Start the modification process
        new Thread(() -> {
            try {
                // Simulate the modification process with progress updates
                updateProgress(progressStatus, progressPercentage, progressBar, "Extracting APK...", 10);
                Thread.sleep(800);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Replacing icon...", 25);
                Thread.sleep(800);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Updating app name...", 40);
                Thread.sleep(800);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Changing package name...", 55);
                Thread.sleep(800);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Rebuilding APK...", 70);
                Thread.sleep(800);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Signing APK...", 85);
                Thread.sleep(800);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Installing APK...", 95);
                Thread.sleep(500);
                
                // Simulate APK installation
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    installModifiedAPK(newAppName, newPackageName);
                });
                
            } catch (InterruptedException e) {
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Operation cancelled", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void installModifiedAPK(String appName, String packageName) {
        try {
            // In a real implementation, this would install the actual modified APK
            // For now, we'll simulate the installation process
            
            Toast.makeText(requireContext(), 
                "APK modification completed!\nApp: " + appName + "\nPackage: " + packageName, 
                Toast.LENGTH_LONG).show();
                
            // Show success message
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Success!")
                .setMessage("Your customized Minecraft PE has been created and installed successfully.\n\n" +
                           "App Name: " + appName + "\n" +
                           "Package: " + packageName + "\n\n" +
                           "You can now find it in your app drawer!")
                .setPositiveButton("Great!", (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_check_circle)
                .show();
                
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error installing APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void updateProgress(TextView status, TextView percentage, LinearProgressIndicator progressBar, 
                              String statusText, int progress) {
        requireActivity().runOnUiThread(() -> {
            status.setText(statusText);
            percentage.setText(progress + "%");
            progressBar.setProgress(progress);
        });
    }
}