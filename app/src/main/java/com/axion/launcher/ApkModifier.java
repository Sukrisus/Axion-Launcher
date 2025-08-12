package com.axion.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.*;

public class ApkModifier {
    private static final String TAG = "ApkModifier";
    private static final String MCPE_PACKAGE = "com.mojang.minecraftpe";
    
    private Context context;
    private ProgressCallback progressCallback;
    private ExecutorService executorService;
    private Future<?> currentTask;
    
    public interface ProgressCallback {
        void onProgress(String status, int progress);
        void onSuccess(String apkPath);
        void onError(String error);
    }
    
    public ApkModifier(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    public void modifyApk(String newAppName, String newPackageName, Bitmap newIcon) {
        // Cancel any existing task
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        
        currentTask = executorService.submit(() -> {
            try {
                // Step 1: Check if MCPE is installed
                updateProgress("Checking Minecraft PE installation...", 5);
                if (!isPackageInstalled(MCPE_PACKAGE)) {
                    onError("Minecraft PE is not installed on this device");
                    return;
                }
                
                // Step 2: Check if we have necessary permissions
                updateProgress("Checking permissions...", 10);
                if (!checkInstallPermissions()) {
                    onError("This app needs permission to install APKs. Please enable 'Install unknown apps' in settings.");
                    return;
                }
                
                // Step 3: Extract APK
                updateProgress("Extracting Minecraft PE APK...", 20);
                String originalApkPath = extractApk(MCPE_PACKAGE);
                if (originalApkPath == null) {
                    onError("Failed to extract Minecraft PE APK");
                    return;
                }
                
                // Step 4: Install APK using system installer
                updateProgress("Installing Minecraft PE...", 80);
                installApk(originalApkPath);
                
                updateProgress("Complete!", 100);
                onSuccess(originalApkPath);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in APK installation process", e);
                onError("Error: " + e.getMessage());
            }
        });
    }
    
    public void cancelModification() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
            Log.d(TAG, "APK installation cancelled by user");
            
            if (progressCallback != null) {
                progressCallback.onError("APK installation cancelled");
            }
        }
    }
    
    public boolean isModifying() {
        return currentTask != null && !currentTask.isDone();
    }
    
    public void shutdown() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    private boolean isPackageInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    private String extractApk(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            String sourceDir = appInfo.sourceDir;
            
            // Validate source APK exists
            File sourceFile = new File(sourceDir);
            if (!sourceFile.exists()) {
                Log.e(TAG, "Source APK file does not exist: " + sourceDir);
                return null;
            }
            
            File outputDir = new File(context.getExternalFilesDir(null), "extracted");
            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    Log.e(TAG, "Failed to create output directory: " + outputDir.getAbsolutePath());
                    return null;
                }
            }
            
            File outputFile = new File(outputDir, "minecraft_pe.apk");
            
            // Copy APK file with progress updates
            long totalBytes = sourceFile.length();
            long copiedBytes = 0;
            
            try (FileInputStream in = new FileInputStream(sourceFile);
                 FileOutputStream out = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    copiedBytes += bytesRead;
                    
                    // Update progress every 5%
                    int progress = (int) ((copiedBytes * 50) / totalBytes); // 50% of total progress
                    if (progress > 0 && progress <= 50) {
                        updateProgress("Extracting APK... " + (progress * 100 / 50) + "%", 20 + progress);
                    }
                }
            }
            
            // Verify the copied file
            if (!outputFile.exists() || outputFile.length() != totalBytes) {
                Log.e(TAG, "APK extraction failed - file size mismatch");
                return null;
            }
            
            Log.d(TAG, "APK extracted successfully to: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting APK", e);
            return null;
        }
    }
    
    private void installApk(String apkPath) {
        try {
            File apkFile = new File(apkPath);
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file does not exist for installation: " + apkPath);
                onError("APK file not found for installation");
                return;
            }
            
            // Check if file is readable
            if (!apkFile.canRead()) {
                Log.e(TAG, "APK file is not readable: " + apkPath);
                onError("APK file is not readable");
                return;
            }
            
            Uri apkUri = FileProvider.getUriForFile(context, 
                context.getPackageName() + ".fileprovider", apkFile);
            
            if (apkUri == null) {
                Log.e(TAG, "Failed to get URI for APK file: " + apkPath);
                onError("Failed to prepare APK for installation");
                return;
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Verify that there's an app to handle this intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Log.d(TAG, "APK installation intent started successfully");
            } else {
                Log.e(TAG, "No app found to handle APK installation");
                onError("No app found to install APK. Please enable 'Install unknown apps' in settings.");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error installing APK", e);
            onError("Failed to install APK: " + e.getMessage());
        }
    }
    
    private void updateProgress(String status, int progress) {
        if (progressCallback != null) {
            progressCallback.onProgress(status, progress);
        }
    }
    
    private void onSuccess(String apkPath) {
        if (progressCallback != null) {
            progressCallback.onSuccess(apkPath);
        }
    }
    
    private void onError(String error) {
        if (progressCallback != null) {
            progressCallback.onError(error);
        }
    }
    
    private boolean checkInstallPermissions() {
        try {
            // Check if we can install APKs
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://test.apk"), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            return intent.resolveActivity(context.getPackageManager()) != null;
        } catch (Exception e) {
            Log.w(TAG, "Error checking install permissions", e);
            return false;
        }
    }
}