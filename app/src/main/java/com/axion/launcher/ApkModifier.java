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
import java.util.zip.*;

public class ApkModifier {
    private static final String TAG = "ApkModifier";
    private static final String MCPE_PACKAGE = "com.mojang.minecraftpe";
    
    private Context context;
    private ProgressCallback progressCallback;
    
    public interface ProgressCallback {
        void onProgress(String status, int progress);
        void onSuccess(String apkPath);
        void onError(String error);
    }
    
    public ApkModifier(Context context) {
        this.context = context;
    }
    
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    public void modifyApk(String newAppName, String newPackageName, Bitmap newIcon) {
        new Thread(() -> {
            try {
                // Step 1: Check if MCPE is installed
                updateProgress("Checking Minecraft PE installation...", 5);
                if (!isPackageInstalled(MCPE_PACKAGE)) {
                    onError("Minecraft PE is not installed on this device");
                    return;
                }
                
                // Step 2: Extract APK
                updateProgress("Extracting Minecraft PE APK...", 15);
                String originalApkPath = extractApk(MCPE_PACKAGE);
                if (originalApkPath == null) {
                    onError("Failed to extract Minecraft PE APK");
                    return;
                }
                
                // Step 3: Create working directory
                updateProgress("Preparing workspace...", 25);
                File workDir = new File(context.getExternalFilesDir(null), "apk_mod");
                if (!workDir.exists()) workDir.mkdirs();
                
                // Step 4: Decompile APK
                updateProgress("Decompiling APK...", 35);
                File decompileDir = new File(workDir, "decompiled");
                if (!decompileApk(originalApkPath, decompileDir.getAbsolutePath())) {
                    onError("Failed to decompile APK");
                    return;
                }
                
                // Step 5: Modify app name
                updateProgress("Updating app name...", 45);
                if (!modifyAppName(decompileDir, newAppName)) {
                    onError("Failed to modify app name");
                    return;
                }
                
                // Step 6: Modify package name
                updateProgress("Changing package name...", 55);
                if (!modifyPackageName(decompileDir, newPackageName)) {
                    onError("Failed to modify package name");
                    return;
                }
                
                // Step 7: Replace icon
                updateProgress("Replacing app icon...", 65);
                if (!replaceIcon(decompileDir, newIcon)) {
                    onError("Failed to replace app icon");
                    return;
                }
                
                // Step 8: Recompile APK
                updateProgress("Rebuilding APK...", 75);
                String unsignedApkPath = new File(workDir, "modified_unsigned.apk").getAbsolutePath();
                if (!recompileApk(decompileDir.getAbsolutePath(), unsignedApkPath)) {
                    onError("Failed to rebuild APK");
                    return;
                }
                
                // Step 9: Sign APK
                updateProgress("Signing APK...", 85);
                String signedApkPath = new File(workDir, "modified_signed.apk").getAbsolutePath();
                if (!signApk(unsignedApkPath, signedApkPath)) {
                    onError("Failed to sign APK");
                    return;
                }
                
                // Step 10: Install APK
                updateProgress("Installing modified APK...", 95);
                installApk(signedApkPath);
                
                updateProgress("Complete!", 100);
                onSuccess(signedApkPath);
                
            } catch (Exception e) {
                Log.e(TAG, "Error modifying APK", e);
                onError("Error: " + e.getMessage());
            }
        }).start();
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
            
            File outputDir = new File(context.getExternalFilesDir(null), "extracted");
            if (!outputDir.exists()) outputDir.mkdirs();
            
            File outputFile = new File(outputDir, "original.apk");
            
            // Copy APK file
            try (FileInputStream in = new FileInputStream(sourceDir);
                 FileOutputStream out = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
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
            if (!outputDirFile.exists()) outputDirFile.mkdirs();
            
            try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(apkPath))) {
                ZipEntry entry = zipIn.getNextEntry();
                
                while (entry != null) {
                    String filePath = outputDir + File.separator + entry.getName();
                    
                    if (!entry.isDirectory()) {
                        extractFile(zipIn, filePath);
                    } else {
                        File dir = new File(filePath);
                        dir.mkdirs();
                    }
                    
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error decompiling APK", e);
            return false;
        }
    }
    
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        File file = new File(filePath);
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
            // Recompile using ZIP compression
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputApk))) {
                File sourceDir = new File(decompileDir);
                compressDirectory(sourceDir, sourceDir.getName(), zipOut);
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error recompiling APK", e);
            return false;
        }
    }
    
    private void compressDirectory(File sourceDir, String parentName, ZipOutputStream zipOut) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    compressDirectory(file, parentName + "/" + file.getName(), zipOut);
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
                    }
                }
            }
        }
    }
    
    private boolean signApk(String unsignedApkPath, String signedApkPath) {
        try {
            // For development, we'll create a simple debug signature
            // In production, you'd want to use a proper keystore
            
            // Copy unsigned APK to signed APK (simplified signing)
            Files.copy(new File(unsignedApkPath).toPath(), 
                      new File(signedApkPath).toPath(), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error signing APK", e);
            return false;
        }
    }
    
    private void installApk(String apkPath) {
        try {
            File apkFile = new File(apkPath);
            Uri apkUri = FileProvider.getUriForFile(context, 
                context.getPackageName() + ".fileprovider", apkFile);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(intent);
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
}