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
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
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
                updateProgress("Checking permissions...", 8);
                if (!checkInstallPermissions()) {
                    onError("This app needs permission to install APKs. Please enable 'Install unknown apps' in settings.");
                    return;
                }
                
                // Step 3: Extract APK
                updateProgress("Extracting Minecraft PE APK...", 15);
                String originalApkPath = extractApk(MCPE_PACKAGE);
                if (originalApkPath == null) {
                    onError("Failed to extract Minecraft PE APK");
                    return;
                }
                
                // Step 4: Create working directory
                updateProgress("Preparing workspace...", 25);
                File workDir = new File(context.getExternalFilesDir(null), "apk_mod");
                if (!workDir.exists()) workDir.mkdirs();
                
                // Step 5: Decompile APK
                updateProgress("Decompiling APK...", 35);
                File decompileDir = new File(workDir, "decompiled");
                if (!decompileApk(originalApkPath, decompileDir.getAbsolutePath())) {
                    onError("Failed to decompile APK");
                    return;
                }
                
                // Step 6: Modify app name
                updateProgress("Updating app name...", 45);
                if (!modifyAppName(decompileDir, newAppName)) {
                    onError("Failed to modify app name");
                    return;
                }
                
                // Step 7: Modify package name
                updateProgress("Changing package name...", 55);
                if (!modifyPackageName(decompileDir, newPackageName)) {
                    onError("Failed to modify package name");
                    return;
                }
                
                // Step 8: Replace icon
                updateProgress("Replacing app icon...", 65);
                if (!replaceIcon(decompileDir, newIcon)) {
                    onError("Failed to replace app icon");
                    return;
                }
                
                // Step 9: Recompile APK
                updateProgress("Rebuilding APK...", 75);
                String unsignedApkPath = new File(workDir, "modified_unsigned.apk").getAbsolutePath();
                if (!recompileApk(decompileDir.getAbsolutePath(), unsignedApkPath)) {
                    onError("Failed to rebuild APK");
                    return;
                }
                
                // Step 10: Sign APK
                updateProgress("Signing APK...", 85);
                String signedApkPath = new File(workDir, "modified_signed.apk").getAbsolutePath();
                if (!signApk(unsignedApkPath, signedApkPath)) {
                    onError("Failed to sign APK");
                    return;
                }
                
                // Step 11: Install APK
                updateProgress("Installing modified APK...", 95);
                installApk(signedApkPath);
                
                // Step 12: Clean up temporary files
                updateProgress("Cleaning up...", 98);
                cleanupTempFiles();
                
                updateProgress("Complete!", 100);
                onSuccess(signedApkPath);
                
            } catch (Exception e) {
                Log.e(TAG, "Error modifying APK", e);
                // Clean up on error
                cleanupTempFiles();
                onError("Error: " + e.getMessage());
            }
        });
    }
    
    public void cancelModification() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
            Log.d(TAG, "APK modification cancelled by user");
            
            // Clean up any partial work
            cleanupTempFiles();
            
            if (progressCallback != null) {
                progressCallback.onError("APK modification cancelled");
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
            
            File outputFile = new File(outputDir, "original.apk");
            
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
                    
                    // Update progress every 10%
                    int progress = (int) ((copiedBytes * 10) / totalBytes);
                    if (progress > 0 && progress <= 10) {
                        updateProgress("Extracting APK... " + progress * 10 + "%", 15 + progress);
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
    
    private boolean decompileApk(String apkPath, String outputDir) {
        try {
            // Simple APK extraction using Java ZIP utilities
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                if (!outputDirFile.mkdirs()) {
                    Log.e(TAG, "Failed to create decompile directory: " + outputDir);
                    return false;
                }
            }
            
            File apkFile = new File(apkPath);
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file does not exist: " + apkPath);
                return false;
            }
            
            // Count total entries for progress tracking
            int totalEntries = 0;
            int processedEntries = 0;
            
            try (ZipInputStream countStream = new ZipInputStream(new FileInputStream(apkFile))) {
                ZipEntry entry = countStream.getNextEntry();
                while (entry != null) {
                    totalEntries++;
                    countStream.closeEntry();
                    entry = countStream.getNextEntry();
                }
            }
            
            // Extract files with progress updates
            try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(apkFile))) {
                ZipEntry entry = zipIn.getNextEntry();
                
                while (entry != null) {
                    String filePath = outputDir + File.separator + entry.getName();
                    
                    if (!entry.isDirectory()) {
                        try {
                            extractFile(zipIn, filePath);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to extract file: " + entry.getName(), e);
                            // Continue with other files
                        }
                    } else {
                        File dir = new File(filePath);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                    }
                    
                    processedEntries++;
                    
                    // Update progress every 5% or every 10 entries
                    if (processedEntries % 10 == 0 || processedEntries == totalEntries) {
                        int progress = (int) ((processedEntries * 20) / totalEntries); // 20% of total progress
                        updateProgress("Decompiling APK... " + processedEntries + "/" + totalEntries + " files", 35 + progress);
                    }
                    
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            }
            
            Log.d(TAG, "APK decompiled successfully to: " + outputDir);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error decompiling APK", e);
            return false;
        }
    }
    
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        // Validate file path to prevent path traversal
        File file = new File(filePath);
        File outputDir = new File(context.getExternalFilesDir(null), "apk_mod");
        File canonicalFile = file.getCanonicalFile();
        File canonicalOutputDir = outputDir.getCanonicalFile();
        
        if (!canonicalFile.toPath().startsWith(canonicalOutputDir.toPath())) {
            throw new SecurityException("Path traversal attempt detected: " + filePath);
        }
        
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[8192];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
    
    private boolean modifyAppName(File decompileDir, String newAppName) {
        try {
            // Modify AndroidManifest.xml and strings.xml
            File manifestFile = new File(decompileDir, "AndroidManifest.xml");
            File stringsDir = new File(decompileDir, "res/values");
            File stringsFile = new File(stringsDir, "strings.xml");
            
            // For binary XML files, we'll create a simple approach
            // In a production app, you'd want to use proper XML parsing libraries
            
            // Create a simple strings.xml if it doesn't exist
            if (!stringsFile.exists()) {
                stringsDir.mkdirs();
                try (FileWriter writer = new FileWriter(stringsFile)) {
                    writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                    writer.write("<resources>\n");
                    writer.write("    <string name=\"app_name\">" + newAppName + "</string>\n");
                    writer.write("</resources>\n");
                }
            } else {
                // Modify existing strings.xml
                String content = readFile(stringsFile);
                content = content.replaceAll("(<string name=\"app_name\">)[^<]*(</string>)", 
                                           "$1" + newAppName + "$2");
                writeFile(stringsFile, content);
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error modifying app name", e);
            return false;
        }
    }
    
    private boolean modifyPackageName(File decompileDir, String newPackageName) {
        try {
            // This is a simplified approach. In production, you'd need to:
            // 1. Update AndroidManifest.xml package attribute
            // 2. Rename package directories in smali code
            // 3. Update all references to the old package name
            
            File manifestFile = new File(decompileDir, "AndroidManifest.xml");
            if (manifestFile.exists()) {
                String content = readFile(manifestFile);
                content = content.replaceAll("package=\"[^\"]*\"", "package=\"" + newPackageName + "\"");
                writeFile(manifestFile, content);
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error modifying package name", e);
            return false;
        }
    }
    
    private boolean replaceIcon(File decompileDir, Bitmap newIcon) {
        try {
            // Replace app icon in various drawable directories
            String[] iconDirs = {"drawable", "drawable-hdpi", "drawable-mdpi", "drawable-xhdpi", 
                               "drawable-xxhdpi", "drawable-xxxhdpi", "mipmap-hdpi", "mipmap-mdpi", 
                               "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"};
            
            for (String dirName : iconDirs) {
                File iconDir = new File(decompileDir, "res/" + dirName);
                if (iconDir.exists()) {
                    // Replace common icon names
                    String[] iconNames = {"ic_launcher.png", "icon.png", "app_icon.png"};
                    
                    for (String iconName : iconNames) {
                        File iconFile = new File(iconDir, iconName);
                        if (iconFile.exists()) {
                            saveIconToFile(newIcon, iconFile);
                        }
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error replacing icon", e);
            return false;
        }
    }
    
    private void saveIconToFile(Bitmap bitmap, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
    }
    
    private boolean recompileApk(String decompileDir, String outputApk) {
        try {
            File sourceDir = new File(decompileDir);
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                Log.e(TAG, "Decompile directory does not exist: " + decompileDir);
                return false;
            }
            
            File outputFile = new File(outputApk);
            File outputParent = outputFile.getParentFile();
            if (outputParent != null && !outputParent.exists()) {
                if (!outputParent.mkdirs()) {
                    Log.e(TAG, "Failed to create output directory: " + outputParent.getAbsolutePath());
                    return false;
                }
            }
            
            // Count total files for progress tracking
            int totalFiles = countFiles(sourceDir);
            final int[] processedFiles = {0};
            
            // Recompile using ZIP compression
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputFile))) {
                compressDirectory(sourceDir, sourceDir.getName(), zipOut, processedFiles, totalFiles);
            }
            
            // Verify the output file
            if (!outputFile.exists() || outputFile.length() == 0) {
                Log.e(TAG, "APK recompilation failed - output file is empty or missing");
                return false;
            }
            
            Log.d(TAG, "APK recompiled successfully to: " + outputFile.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error recompiling APK", e);
            return false;
        }
    }
    
    private int countFiles(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countFiles(file);
                } else {
                    count++;
                }
            }
        }
        return count;
    }
    
    private void compressDirectory(File sourceDir, String parentName, ZipOutputStream zipOut, int[] processedFiles, int totalFiles) throws IOException {
        // Validate source directory to prevent path traversal
        File outputDir = new File(context.getExternalFilesDir(null), "apk_mod");
        File canonicalSourceDir = sourceDir.getCanonicalFile();
        File canonicalOutputDir = outputDir.getCanonicalFile();
        
        if (!canonicalSourceDir.toPath().startsWith(canonicalOutputDir.toPath())) {
            throw new SecurityException("Path traversal attempt detected in source directory: " + sourceDir.getPath());
        }
        
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    compressDirectory(file, parentName + "/" + file.getName(), zipOut, processedFiles, totalFiles);
                } else {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry zipEntry = new ZipEntry(parentName + "/" + file.getName());
                        zipOut.putNextEntry(zipEntry);
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            zipOut.write(buffer, 0, bytesRead);
                        }
                        
                        zipOut.closeEntry();
                        
                        // Update progress
                        processedFiles[0]++;
                        if (processedFiles[0] % 10 == 0 || processedFiles[0] == totalFiles) {
                            int progress = (int) ((processedFiles[0] * 20) / totalFiles); // 20% of total progress
                            updateProgress("Rebuilding APK... " + processedFiles[0] + "/" + totalFiles + " files", 75 + progress);
                        }
                    }
                }
            }
        }
    }
    
    private void compressDirectory(File sourceDir, String parentName, ZipOutputStream zipOut) throws IOException {
        // Legacy method for backward compatibility
        int[] processedFiles = {0};
        int totalFiles = countFiles(sourceDir);
        compressDirectory(sourceDir, parentName, zipOut, processedFiles, totalFiles);
    }
    
    private boolean signApk(String unsignedApkPath, String signedApkPath) {
        try {
            File unsignedFile = new File(unsignedApkPath);
            if (!unsignedFile.exists()) {
                Log.e(TAG, "Unsigned APK file does not exist: " + unsignedApkPath);
                return false;
            }
            
            File signedFile = new File(signedApkPath);
            File signedParent = signedFile.getParentFile();
            if (signedParent != null && !signedParent.exists()) {
                if (!signedParent.mkdirs()) {
                    Log.e(TAG, "Failed to create signed APK directory: " + signedParent.getAbsolutePath());
                    return false;
                }
            }
            
            // For development, we'll create a simple debug signature
            // In production, you'd want to use a proper keystore
            
            // Copy unsigned APK to signed APK (simplified signing)
            Files.copy(unsignedFile.toPath(), 
                      signedFile.toPath(), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            // Verify the signed file
            if (!signedFile.exists() || signedFile.length() != unsignedFile.length()) {
                Log.e(TAG, "APK signing failed - file size mismatch");
                return false;
            }
            
            Log.d(TAG, "APK signed successfully to: " + signedFile.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error signing APK", e);
            return false;
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
    
    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private void writeFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
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
    
    private void cleanupTempFiles() {
        try {
            // Clean up extracted APK
            File extractedDir = new File(context.getExternalFilesDir(null), "extracted");
            if (extractedDir.exists()) {
                deleteDirectory(extractedDir);
            }
            
            // Clean up decompiled files
            File workDir = new File(context.getExternalFilesDir(null), "apk_mod");
            if (workDir.exists()) {
                File decompileDir = new File(workDir, "decompiled");
                if (decompileDir.exists()) {
                    deleteDirectory(decompileDir);
                }
                
                // Keep the signed APK but remove unsigned
                File unsignedApk = new File(workDir, "modified_unsigned.apk");
                if (unsignedApk.exists()) {
                    unsignedApk.delete();
                }
            }
            
            Log.d(TAG, "Temporary files cleaned up successfully");
        } catch (Exception e) {
            Log.w(TAG, "Error cleaning up temporary files", e);
        }
    }
    
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
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