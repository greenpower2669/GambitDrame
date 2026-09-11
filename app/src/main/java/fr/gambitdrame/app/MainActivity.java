package fr.gambitdrame.app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int bottomInset;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                } else {
                    bottomInset = insets.getSystemWindowInsetBottom();
                }
                v.setPadding(0, 0, 0, bottomInset);
                return insets;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String js = "(function(){"
                        + "if(document.getElementById('android-board-fix'))return;"
                        + "var s=document.createElement('style');"
                        + "s.id='android-board-fix';"
                        + "s.textContent='"
                        + ".board{grid-template-columns:repeat(8,minmax(0,1fr))!important;grid-template-rows:repeat(8,minmax(0,1fr))!important;aspect-ratio:1/1!important;}"
                        + ".square{width:100%!important;height:100%!important;min-width:0!important;min-height:0!important;align-self:stretch!important;justify-self:stretch!important;}"
                        + "main{padding-bottom:72px!important;}"
                        + "';"
                        + "document.head.appendChild(s);"
                        + "})();";
                view.evaluateJavascript(js, null);
            }
        });

        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        webView.requestApplyInsets();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
