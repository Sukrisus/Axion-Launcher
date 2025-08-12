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
            String extractedApkPath = null;
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
                extractedApkPath = extractApk(MCPE_PACKAGE);
                if (extractedApkPath == null) {
                    onError("Failed to extract Minecraft PE APK");
                    return;
                }
                
                // Step 4: Install APK using system installer
                updateProgress("Installing Minecraft PE...", 80);
                installApk(extractedApkPath);
                
                updateProgress("Complete!", 100);
                onSuccess(extractedApkPath);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in APK installation process", e);
                onError("Error: " + e.getMessage());
            } finally {
                // Clean up temporary files on error or completion
                cleanupTempFiles();
            }
        });
    }
    
    public void cancelModification() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
            Log.d(TAG, "APK installation cancelled by user");
            
            // Clean up temporary files when cancelled
            cleanupTempFiles();
            
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
        // Clean up temporary files on shutdown
        cleanupTempFiles();
    }
    
    private boolean isPackageInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    private boolean validateApkFile(String apkPath) {
        try {
            File apkFile = new File(apkPath);
            if (!apkFile.exists() || !apkFile.canRead()) {
                return false;
            }
            
            // Check if it's a valid ZIP file (APK is a ZIP file)
            try (ZipFile zipFile = new ZipFile(apkFile)) {
                // Check for essential APK files
                boolean hasManifest = zipFile.getEntry("AndroidManifest.xml") != null;
                boolean hasResources = zipFile.getEntry("resources.arsc") != null;
                boolean hasClasses = zipFile.getEntry("classes.dex") != null;
                
                if (!hasManifest) {
                    Log.e(TAG, "APK validation failed: Missing AndroidManifest.xml");
                    return false;
                }
                
                if (!hasResources) {
                    Log.w(TAG, "APK validation warning: Missing resources.arsc");
                }
                
                if (!hasClasses) {
                    Log.w(TAG, "APK validation warning: Missing classes.dex");
                }
                
                return true;
            } catch (Exception e) {
                Log.e(TAG, "APK validation failed: Not a valid ZIP file", e);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error validating APK file", e);
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
            
            if (!sourceFile.canRead()) {
                Log.e(TAG, "Source APK file is not readable: " + sourceDir);
                return null;
            }
            
            // Validate the source APK
            if (!validateApkFile(sourceDir)) {
                Log.e(TAG, "Source APK is not valid: " + sourceDir);
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
            } catch (IOException e) {
                Log.e(TAG, "Error copying APK file", e);
                // Clean up partial file
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                return null;
            }
            
            // Verify the copied file
            if (!outputFile.exists() || outputFile.length() != totalBytes) {
                Log.e(TAG, "APK extraction failed - file size mismatch");
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                return null;
            }
            
            // Validate the copied APK
            if (!validateApkFile(outputFile.getAbsolutePath())) {
                Log.e(TAG, "Copied APK is not valid");
                if (outputFile.exists()) {
                    outputFile.delete();
                }
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
            
            // Check file size
            long fileSize = apkFile.length();
            if (fileSize == 0) {
                Log.e(TAG, "APK file is empty: " + apkPath);
                onError("APK file is empty or corrupted");
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
            
            // For Android 7.0+, add additional flags
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            }
            
            // Verify that there's an app to handle this intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Log.d(TAG, "APK installation intent started successfully");
            } else {
                Log.e(TAG, "No app found to handle APK installation");
                onError("No package installer found. Please enable 'Install unknown apps' in settings.");
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
            
            // Check if there's an app to handle this intent
            boolean hasInstaller = intent.resolveActivity(context.getPackageManager()) != null;
            
            if (!hasInstaller) {
                Log.w(TAG, "No package installer found");
                return false;
            }
            
            // For Android 8.0+, check if we can request install packages
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PackageManager pm = context.getPackageManager();
                if (!pm.canRequestPackageInstalls()) {
                    Log.w(TAG, "Cannot request package installs");
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error checking install permissions", e);
            return false;
        }
    }

    private void cleanupTempFiles() {
        try {
            File extractedDir = new File(context.getExternalFilesDir(null), "extracted");
            if (extractedDir.exists()) {
                File[] files = extractedDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            if (!file.delete()) {
                                Log.w(TAG, "Failed to delete temporary file: " + file.getAbsolutePath());
                            }
                        }
                    }
                }
                if (!extractedDir.delete()) {
                    Log.w(TAG, "Failed to delete temporary directory: " + extractedDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error cleaning up temporary files", e);
        }
    }
}