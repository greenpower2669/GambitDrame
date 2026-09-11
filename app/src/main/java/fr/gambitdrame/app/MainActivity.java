package fr.gambitdrame.app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.FRANCE);
                tts.setSpeechRate(0.95f);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

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

        webView.addJavascriptInterface(new NativeBridge(), "GambitNative");

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
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                return host == null || !"appassets.androidplatform.net".equals(host);
            }
        });

        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        webView.requestApplyInsets();
    }

    private class NativeBridge {
        @JavascriptInterface
        public void speak(String text) {
            if (!ttsReady || text == null || text.trim().isEmpty()) return;
            runOnUiThread(() -> tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gambitdrame"));
        }

        @JavascriptInterface
        public void haptic(String kind) {
            runOnUiThread(() -> {
                if (webView == null) return;
                int feedback = "wrong".equals(kind)
                        ? HapticFeedbackConstants.LONG_PRESS
                        : HapticFeedbackConstants.KEYBOARD_TAP;
                webView.performHapticFeedback(feedback);
            });
        }
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
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (webView != null) {
            webView.removeJavascriptInterface("GambitNative");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
