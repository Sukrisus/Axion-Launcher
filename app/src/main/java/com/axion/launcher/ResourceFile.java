package com.axion.launcher;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResourceFile {
    private File file;
    private String name;
    private String extension;
    private long size;
    private long lastModified;
    private String section; // "mods", "textures", or "maps"

    public ResourceFile(File file, String section) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        
        this.file = file;
        this.section = section != null ? section : "unknown";
        this.name = file.getName();
        this.extension = getFileExtension(file.getName());
        this.size = file.length();
        this.lastModified = file.lastModified();
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getSection() {
        return section;
    }

    public String getFormattedSize() {
        if (size < 0) {
            return "Unknown";
        }
        
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    public String getFormattedDate() {
        if (lastModified <= 0) {
            return "Unknown";
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(lastModified));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public boolean isMinecraftFile() {
        return extension.equals(".mcaddon") || 
               extension.equals(".mcpack") || 
               extension.equals(".mctemplate") ||
               extension.equals(".mcaddon.zip") || 
               extension.equals(".mcpack.zip") || 
               extension.equals(".mctemplate.zip");
    }

    public boolean needsZipRemoval() {
        return extension.equals(".mcaddon.zip") || 
               extension.equals(".mcpack.zip") || 
               extension.equals(".mctemplate.zip");
    }

    public String getMinecraftExtension() {
        if (needsZipRemoval()) {
            return extension.replace(".zip", "");
        }
        return extension;
    }
}