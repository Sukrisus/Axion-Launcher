package com.axion.launcher;

import android.app.DownloadManager;
import android.content.Context;
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

public class SlimSkinPackCompilerWebViewFragment extends Fragment {

    private static final String TAG = "SlimSkinPackCompilerWebView";
    
    private WebView webView;
    private ImageButton backButton;

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
        loadSlimSkinPackCompiler();
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
                    // Create download directory
                    String downloadPath = createCustomDownloadDirectory();
                    
                    // Get filename from URL or content disposition
                    String fileName = getFileNameFromUrl(url, contentDisposition);
                    
                    // Create download request
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    request.setTitle(fileName);
                    request.setDescription("Downloading slim skin pack file");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir("axion/files/resources/textures", fileName);
                    
                    // Start download
                    DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    if (downloadManager != null) {
                        downloadManager.enqueue(request);
                        Toast.makeText(requireContext(), "Download started: " + fileName, Toast.LENGTH_SHORT).show();
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error starting download", e);
                    Toast.makeText(requireContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private String createCustomDownloadDirectory() {
        File baseDir = new File(Environment.getExternalStorageDirectory(), "axion/files/resources");
        File sectionDir = new File(baseDir, "textures");
        
        if (!sectionDir.exists()) {
            if (!sectionDir.mkdirs()) {
                // Fallback to app's external files directory if public storage fails
                File fallbackDir = new File(requireContext().getExternalFilesDir(null), "resources");
                File fallbackSectionDir = new File(fallbackDir, "textures");
                if (!fallbackSectionDir.exists()) {
                    fallbackSectionDir.mkdirs();
                }
                return fallbackSectionDir.getAbsolutePath();
            }
        }
        
        return sectionDir.getAbsolutePath();
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
        return "slim_skin_pack_" + System.currentTimeMillis() + ".mcpack";
    }

    private void loadSlimSkinPackCompiler() {
        webView.loadUrl("https://cdsmythe.com/minecraftskins/slimskins/index.html");
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
        if (webView != null) {
            webView.destroy();
        }
    }
}