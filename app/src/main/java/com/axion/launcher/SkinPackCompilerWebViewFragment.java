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
import android.webkit.WebChromeClient;
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

public class SkinPackCompilerWebViewFragment extends Fragment {

    private static final String TAG = "SkinPackCompilerWebView";
    
    private WebView webView;
    private ImageButton backButton;
    
    // Track broadcast receivers to prevent memory leaks
    private List<BroadcastReceiver> registeredReceivers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chunkbase_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        webView = view.findViewById(R.id.webview);
        backButton = view.findViewById(R.id.back_button);
        
        setupWebView();
        setupBackButton();
        setupDownloadListener();
        loadSkinPackCompiler();
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
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // Progress updates if needed
            }
        });
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                // Go back to Tools fragment
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupDownloadListener() {
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    // Get filename from URL or content disposition
                    String fileName = getFileNameFromUrl(url, contentDisposition);
                    
                    // Create download request
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    request.setTitle(fileName);
                    request.setDescription("Downloading skin pack file");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    
                    // First download to Downloads directory (this works reliably)
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    
                    // Start download
                    DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    if (downloadManager != null) {
                        long downloadId = downloadManager.enqueue(request);
                        Toast.makeText(requireContext(), "Download started: " + fileName, Toast.LENGTH_SHORT).show();
                        
                        // Set up a broadcast receiver to move the file after download completes
                        setupDownloadCompleteReceiver(downloadManager, downloadId, fileName);
                    }
                    
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
                            File targetDir = new File(Environment.getExternalStorageDirectory(), "axion/files/resources/textures");
                            if (!targetDir.exists()) {
                                targetDir.mkdirs();
                            }
                            
                            // Move file to target directory
                            File targetFile = new File(targetDir, fileName);
                            if (downloadedFile.renameTo(targetFile)) {
                                Toast.makeText(requireContext(), "File moved to textures folder", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(requireContext(), "File copied to textures folder", Toast.LENGTH_SHORT).show();
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

    private String getFileNameFromUrl(String url, String contentDisposition) {
        // Try to get filename from content disposition first
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            String filename = contentDisposition.substring(contentDisposition.indexOf("filename=") + 9);
            if (filename.startsWith("\"") && filename.endsWith("\"")) {
                filename = filename.substring(1, filename.length() - 1);
            }
            return filename;
        }
        
        // Fallback to URL
        if (url != null && url.contains("/")) {
            String filename = url.substring(url.lastIndexOf("/") + 1);
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.indexOf("?"));
            }
            return filename;
        }
        
        // Default filename
        return "skin_pack_" + System.currentTimeMillis() + ".mcpack";
    }

    private void loadSkinPackCompiler() {
        webView.loadUrl("https://cdsmythe.com/minecraftskins/");
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