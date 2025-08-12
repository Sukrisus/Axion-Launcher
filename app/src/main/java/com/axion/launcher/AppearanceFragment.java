package com.axion.launcher;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.view.animation.AccelerateDecelerateInterpolator;
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

public class AppearanceFragment extends Fragment implements ApkModifier.ProgressCallback {

    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final String DEFAULT_PACKAGE_NAME = "com.mojang.minecraftpe";
    
    private ImageView appIcon;
    private TextInputEditText appNameInput;
    private TextInputEditText packageNameInput;
    private MaterialButton changeAppearanceButton;
    private Bitmap selectedIcon;
    private String selectedIconPath;
    
    private ApkModifier apkModifier;
    private AlertDialog progressDialog;
    private TextView progressStatus;
    private TextView progressPercentage;
    private LinearProgressIndicator progressBar;
    
    // Bottom progress bar components
    private MaterialCardView bottomProgressCard;
    private TextView bottomProgressStatus;
    private TextView bottomProgressPercentage;
    private LinearProgressIndicator bottomProgressBar;
    private MaterialButton minimizeButton;
    private MaterialButton cancelButton;
    
    private boolean isProcessing = false;
    private boolean isMinimized = false;
    
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
        
        // Initialize bottom progress bar components
        View bottomProgressContainer = view.findViewById(R.id.bottom_progress_container);
        bottomProgressCard = bottomProgressContainer.findViewById(R.id.progress_card);
        bottomProgressStatus = bottomProgressContainer.findViewById(R.id.progress_status_text);
        bottomProgressPercentage = bottomProgressContainer.findViewById(R.id.progress_percentage_text);
        bottomProgressBar = bottomProgressContainer.findViewById(R.id.bottom_progress_bar);
        minimizeButton = bottomProgressContainer.findViewById(R.id.minimize_button);
        cancelButton = bottomProgressContainer.findViewById(R.id.cancel_button);
        
        // Initialize APK modifier
        apkModifier = new ApkModifier(requireContext());
        apkModifier.setProgressCallback(this);
        
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
        changeAppearanceButton.setOnClickListener(v -> startApkModification());
        
        // Set up bottom progress bar buttons
        minimizeButton.setOnClickListener(v -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                isMinimized = true;
            }
        });
        
        cancelButton.setOnClickListener(v -> {
            showCancelConfirmation();
        });
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
    
    private void startApkModification() {
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
        
        // Check if Minecraft PE is installed
        try {
            requireContext().getPackageManager().getPackageInfo("com.mojang.minecraftpe", 0);
        } catch (Exception e) {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Minecraft PE Not Found")
                .setMessage("Minecraft PE (com.mojang.minecraftpe) is not installed on this device. Please install it first.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_info)
                .show();
            return;
        }
        
        // Show confirmation dialog
        showConfirmationDialog(newAppName, newPackageName);
    }
    
    private void showConfirmationDialog(String appName, String packageName) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm APK Modification")
            .setMessage("This will:\n\n" +
                       "• Extract Minecraft PE APK\n" +
                       "• Change app name to: " + appName + "\n" +
                       "• Change package to: " + packageName + "\n" +
                       "• Replace the app icon\n" +
                       "• Sign and install the modified APK\n\n" +
                       "This process may take several minutes. Continue?")
            .setPositiveButton("Start Modification", (dialog, which) -> {
                showBottomProgressBar();
                showProgressDialog();
                // Start the real APK modification process
                apkModifier.modifyApk(appName, packageName, selectedIcon);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .setIcon(R.drawable.ic_build)
            .show();
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
    
    private void showBottomProgressBar() {
        isProcessing = true;
        isMinimized = false;
        
        // Show bottom progress bar with animation
        bottomProgressCard.setVisibility(View.VISIBLE);
        bottomProgressCard.setTranslationY(bottomProgressCard.getHeight());
        bottomProgressCard.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        
        // Reset progress
        bottomProgressBar.setProgress(0);
        bottomProgressPercentage.setText("0%");
        bottomProgressStatus.setText("Preparing...");
    }
    
    private void hideBottomProgressBar() {
        if (!isProcessing) return;
        
        isProcessing = false;
        
        // Hide bottom progress bar with animation
        bottomProgressCard.animate()
                .translationY(bottomProgressCard.getHeight())
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> bottomProgressCard.setVisibility(View.GONE))
                .start();
    }
    
    private void showProgressDialog() {
        if (isMinimized) return;
        
        View progressView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null);
        progressStatus = progressView.findViewById(R.id.progress_status);
        progressPercentage = progressView.findViewById(R.id.progress_percentage);
        progressBar = progressView.findViewById(R.id.progress_bar);
        
        progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(progressView)
                .setTitle("Modifying APK")
                .setCancelable(false)
                .create();
        
        progressDialog.show();
    }
    
    private void showCancelConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel APK Modification?")
            .setMessage("Are you sure you want to cancel the APK modification process? This will stop the current operation.")
            .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                // TODO: Implement actual cancellation logic in ApkModifier
                hideBottomProgressBar();
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(requireContext(), "APK modification cancelled", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Continue", (dialog, which) -> dialog.dismiss())
            .setIcon(R.drawable.ic_error)
            .show();
    }
    
    private void animateProgressBar(int targetProgress) {
        if (bottomProgressBar != null) {
            ValueAnimator animator = ValueAnimator.ofInt(bottomProgressBar.getProgress(), targetProgress);
            animator.setDuration(200);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                int progress = (int) animation.getAnimatedValue();
                bottomProgressBar.setProgress(progress);
            });
            animator.start();
        }
        
        if (progressBar != null) {
            ValueAnimator animator = ValueAnimator.ofInt(progressBar.getProgress(), targetProgress);
            animator.setDuration(200);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                int progress = (int) animation.getAnimatedValue();
                progressBar.setProgress(progress);
            });
            animator.start();
        }
    }
    
    // ApkModifier.ProgressCallback implementation
    @Override
    public void onProgress(String status, int progress) {
        requireActivity().runOnUiThread(() -> {
            // Update dialog progress if visible
            if (progressStatus != null) progressStatus.setText(status);
            if (progressPercentage != null) progressPercentage.setText(progress + "%");
            if (progressBar != null) animateProgressBar(progress);
            
            // Update bottom progress bar
            if (bottomProgressStatus != null) bottomProgressStatus.setText(status);
            if (bottomProgressPercentage != null) bottomProgressPercentage.setText(progress + "%");
            if (bottomProgressBar != null) animateProgressBar(progress);
        });
    }
    
    @Override
    public void onSuccess(String apkPath) {
        requireActivity().runOnUiThread(() -> {
            hideBottomProgressBar();
            
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            
            String appName = appNameInput.getText().toString().trim();
            String packageName = packageNameInput.getText().toString().trim();
            
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Success!")
                .setMessage("Your customized Minecraft PE has been created and the installation dialog should appear.\n\n" +
                           "App Name: " + appName + "\n" +
                           "Package: " + packageName + "\n\n" +
                           "The modified APK has been saved and is ready to install!")
                .setPositiveButton("Great!", (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_check_circle)
                .show();
        });
    }
    
    @Override
    public void onError(String error) {
        requireActivity().runOnUiThread(() -> {
            hideBottomProgressBar();
            
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage("Failed to modify APK:\n\n" + error)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_error)
                .show();
        });
    }
}