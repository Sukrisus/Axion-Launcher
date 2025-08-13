package com.axion.launcher;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WebViewFragment extends Fragment {

    private static final String TAG = "WebViewFragment";
    
    private WebView webView;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ImageButton refreshButton;
    
    private String initialUrl;
    private String title;
    private String allowedSection; // "mods", "textures", or "maps"
    
    // Track broadcast receivers to prevent memory leaks
    private List<BroadcastReceiver> registeredReceivers = new ArrayList<>();

    public static WebViewFragment newInstance(String url, String title) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        args.putString("title", title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialUrl = getArguments().getString("url", "");
            title = getArguments().getString("title", "");
            
            // Determine the allowed section based on the initial URL
            if (initialUrl.contains("/mods/")) {
                allowedSection = "mods";
            } else if (initialUrl.contains("/textures/")) {
                allowedSection = "textures";
            } else if (initialUrl.contains("/maps/")) {
                allowedSection = "maps";
            } else {
                allowedSection = "mods"; // Default fallback
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        webView = view.findViewById(R.id.webview);
        backButton = view.findViewById(R.id.back_button);
        forwardButton = view.findViewById(R.id.forward_button);
        refreshButton = view.findViewById(R.id.refresh_button);
        
        setupWebView();
        setupNavigationControls();
        setupDownloadListener();
        loadInitialUrl();
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Check if the URL is allowed for this section
                if (!isUrlAllowed(url)) {
                    Log.w(TAG, "Blocked navigation to: " + url + " (not allowed in " + allowedSection + " section)");
                    Toast.makeText(requireContext(), 
                        "Navigation blocked: This content is not available in the " + allowedSection + " section", 
                        Toast.LENGTH_LONG).show();
                    return true; // Block the navigation
                }
                
                // Handle different URL schemes
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                    // Let the system handle these URLs
                    return false;
                } else if (url.startsWith("market://") || url.startsWith("intent://")) {
                    // Handle app store links
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Cannot open this link", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                } else {
                    // Load in WebView
                    view.loadUrl(url);
                    return true;
                }
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateNavigationButtons();
            }
            
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    Toast.makeText(requireContext(), "Error loading page: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // You can add a progress bar here if needed
            }
            
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // Update the title if needed
            }
        });
    }

    private boolean isUrlAllowed(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        try {
            // Allow all non-modbay.org URLs (external links)
            if (!url.contains("modbay.org")) {
                return true;
            }
            
            // For modbay.org URLs, only block specific cross-section navigation
            switch (allowedSection) {
                case "mods":
                    // Block URLs that start with textures or maps sections
                    return !url.startsWith("https://modbay.org/textures/") && 
                           !url.startsWith("https://modbay.org/maps/");
                case "textures":
                    // Block URLs that start with mods or maps sections
                    return !url.startsWith("https://modbay.org/mods/") && 
                           !url.startsWith("https://modbay.org/maps/");
                case "maps":
                    // Block URLs that start with mods or textures sections
                    return !url.startsWith("https://modbay.org/mods/") && 
                           !url.startsWith("https://modbay.org/textures/");
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking URL allowance", e);
            return false;
        }
    }

    private void setupNavigationControls() {
        backButton.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        
        forwardButton.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });
        
        refreshButton.setOnClickListener(v -> {
            webView.reload();
        });
    }

    private void setupDownloadListener() {
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    
                    // Set headers
                    request.addRequestHeader("User-Agent", userAgent);
                    
                    // Get filename
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    
                    // First download to Downloads directory (this works reliably)
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    
                    // Set notification
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setTitle("Downloading " + fileName);
                    request.setDescription("Downloading to Downloads folder...");
                    
                    // Start download
                    DownloadManager dm = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    long downloadId = dm.enqueue(request);
                    
                    Toast.makeText(requireContext(), "Download started: " + fileName, Toast.LENGTH_SHORT).show();
                    
                    // Set up a broadcast receiver to move the file after download completes
                    setupDownloadCompleteReceiver(dm, downloadId, fileName);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error starting download", e);
                    Toast.makeText(requireContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupDownloadCompleteReceiver(DownloadManager dm, long downloadId, String fileName) {
        // Create a broadcast receiver to handle download completion
        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    try {
                        // Get the downloaded file from Downloads directory
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File downloadedFile = new File(downloadsDir, fileName);
                        
                        if (downloadedFile.exists()) {
                            // Create target directory
                            File targetDir = new File(Environment.getExternalStorageDirectory(), "axion/files/resources/" + allowedSection);
                            if (!targetDir.exists()) {
                                targetDir.mkdirs();
                            }
                            
                            // Move file to target directory
                            File targetFile = new File(targetDir, fileName);
                            if (downloadedFile.renameTo(targetFile)) {
                                Toast.makeText(requireContext(), "File moved to " + allowedSection + " folder", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "File moved successfully: " + targetFile.getAbsolutePath());
                            } else {
                                // If rename fails, try copy
                                java.io.FileInputStream fis = new java.io.FileInputStream(downloadedFile);
                                java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile);
                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                while ((bytesRead = fis.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                                fis.close();
                                fos.close();
                                downloadedFile.delete(); // Delete original
                                Toast.makeText(requireContext(), "File copied to " + allowedSection + " folder", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "File copied successfully: " + targetFile.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error moving downloaded file", e);
                        Toast.makeText(requireContext(), "Error moving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    } finally {
                        // Unregister this receiver
                        try {
                            if (requireContext() != null) {
                                requireContext().unregisterReceiver(this);
                                registeredReceivers.remove(this);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error unregistering receiver", e);
                        }
                    }
                }
            }
        };
        
        // Register the receiver and track it
        try {
            requireContext().registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            registeredReceivers.add(downloadReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error registering download receiver", e);
        }
    }

    private void loadInitialUrl() {
        if (initialUrl != null && !initialUrl.isEmpty()) {
            webView.loadUrl(initialUrl);
        }
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(webView.canGoBack());
        forwardButton.setEnabled(webView.canGoForward());
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        }
    }

    public void loadUrl(String url) {
        if (webView != null && isUrlAllowed(url)) {
            webView.loadUrl(url);
        } else if (webView != null) {
            Toast.makeText(requireContext(), 
                "This URL is not allowed in the " + allowedSection + " section.", 
                Toast.LENGTH_SHORT).show();
        }
    }

    public String getCurrentUrl() {
        return webView != null ? webView.getUrl() : "";
    }

    public String getTitle() {
        return title;
    }

    public String getAllowedSection() {
        return allowedSection;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up WebView
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        
        // Clean up all registered broadcast receivers to prevent memory leaks
        if (registeredReceivers != null) {
            for (BroadcastReceiver receiver : registeredReceivers) {
                try {
                    if (requireContext() != null) {
                        requireContext().unregisterReceiver(receiver);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error unregistering receiver in onDestroyView", e);
                }
            }
            registeredReceivers.clear();
        }
    }
}