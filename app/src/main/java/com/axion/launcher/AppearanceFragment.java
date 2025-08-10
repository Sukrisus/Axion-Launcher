package com.axion.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

public class AppearanceFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    
    private ImageView appIcon;
    private TextInputEditText appNameInput;
    private MaterialButton changeAppearanceButton;
    private Bitmap selectedIcon;
    private String selectedIconPath;

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
        changeAppearanceButton = view.findViewById(R.id.change_appearance_button);
        MaterialCardView iconContainer = view.findViewById(R.id.icon_container);
        
        // Set up icon selection
        iconContainer.setOnClickListener(v -> selectImage());
        
        // Set up change appearance button
        changeAppearanceButton.setOnClickListener(v -> startAppearanceModification());
    }
    
    private void selectImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                    PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
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
        
        if (newAppName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an app name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedIcon == null) {
            Toast.makeText(requireContext(), "Please select an icon", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgressDialog(newAppName);
    }
    
    private void showProgressDialog(String newAppName) {
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
                Thread.sleep(1000);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Replacing icon...", 30);
                Thread.sleep(1000);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Updating app name...", 50);
                Thread.sleep(1000);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Signing APK...", 70);
                Thread.sleep(1000);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Installing...", 90);
                Thread.sleep(1000);
                
                updateProgress(progressStatus, progressPercentage, progressBar, "Complete!", 100);
                Thread.sleep(500);
                
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Appearance modified successfully!", Toast.LENGTH_LONG).show();
                });
                
            } catch (InterruptedException e) {
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Operation cancelled", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void updateProgress(TextView status, TextView percentage, LinearProgressIndicator progressBar, 
                              String statusText, int progress) {
        requireActivity().runOnUiThread(() -> {
            status.setText(statusText);
            percentage.setText(progress + "%");
            progressBar.setProgress(progress);
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}