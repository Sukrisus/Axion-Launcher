package com.axion.launcher;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WebViewFragment extends Fragment {

    private static final String TAG = "WebViewFragment";
    
    private WebView webView;
    private EditText urlBar;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ImageButton refreshButton;
    private ImageButton goButton;
    
    private String initialUrl;
    private String title;
    private String allowedSection; // "mods", "textures", or "maps"

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
        urlBar = view.findViewById(R.id.url_bar);
        backButton = view.findViewById(R.id.back_button);
        forwardButton = view.findViewById(R.id.forward_button);
        refreshButton = view.findViewById(R.id.refresh_button);
        goButton = view.findViewById(R.id.go_button);
        
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
                urlBar.setText(url);
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
        
        // Allow non-modbay.org URLs (external links)
        if (!url.contains("modbay.org")) {
            return true;
        }
        
        // Check if URL matches the allowed section
        switch (allowedSection) {
            case "mods":
                return url.contains("/mods/") || url.equals("https://modbay.org/mods/") || url.equals("https://modbay.org/mods");
            case "textures":
                return url.contains("/textures/") || url.equals("https://modbay.org/textures/") || url.equals("https://modbay.org/textures");
            case "maps":
                return url.contains("/maps/") || url.equals("https://modbay.org/maps/") || url.equals("https://modbay.org/maps");
            default:
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
        
        goButton.setOnClickListener(v -> {
            String url = urlBar.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                
                // Check if the manually entered URL is allowed
                if (!isUrlAllowed(url)) {
                    Toast.makeText(requireContext(), 
                        "This URL is not allowed in the " + allowedSection + " section. Please use the appropriate tab.", 
                        Toast.LENGTH_LONG).show();
                    return;
                }
                
                webView.loadUrl(url);
            }
        });
        
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            String url = urlBar.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                
                // Check if the manually entered URL is allowed
                if (!isUrlAllowed(url)) {
                    Toast.makeText(requireContext(), 
                        "This URL is not allowed in the " + allowedSection + " section. Please use the appropriate tab.", 
                        Toast.LENGTH_LONG).show();
                    return true;
                }
                
                webView.loadUrl(url);
            }
            return true;
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
                    
                    // Set destination
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    
                    // Set notification
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setTitle("Downloading " + fileName);
                    request.setDescription("Downloading file...");
                    
                    // Start download
                    DownloadManager dm = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    
                    Toast.makeText(requireContext(), "Download started: " + fileName, Toast.LENGTH_LONG).show();
                    
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadInitialUrl() {
        if (initialUrl != null && !initialUrl.isEmpty()) {
            webView.loadUrl(initialUrl);
            urlBar.setText(initialUrl);
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
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroyView();
    }
}