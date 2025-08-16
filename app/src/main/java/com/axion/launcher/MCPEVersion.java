package com.axion.launcher;

public class MCPEVersion {
    private String versionNumber;
    private String type;
    private String description;
    private String filterType;
    private boolean isInstalled;

    public MCPEVersion(String versionNumber, String type, String description, String filterType, boolean isInstalled) {
        this.versionNumber = versionNumber;
        this.type = type;
        this.description = description;
        this.filterType = filterType;
        this.isInstalled = isInstalled;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    public void setInstalled(boolean installed) {
        isInstalled = installed;
    }
}