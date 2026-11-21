package jp.tangoquiz.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String ttsStatus = "初期化中";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) { ttsStatus = "読み上げエンジンを起動できません"; return; }
            int r = tts.setLanguage(Locale.US);
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) r = tts.setLanguage(Locale.ENGLISH);
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) { ttsStatus = "英語の音声データがありません"; return; }
            ttsReady = true; ttsStatus = "OK";
        });
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        // アプリ内のファイルを https://appassets.androidplatform.net/assets/ として配信(localStorage が安定して使える)
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidTTS");
        setContentView(web);
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    class Bridge {
        @JavascriptInterface
        public boolean available() { return ttsReady; }
        @JavascriptInterface
        public String status() { return ttsStatus; }
        @JavascriptInterface
        public boolean speak(String text, float rate) {
            if (!ttsReady) return false;
            tts.setSpeechRate(rate);
            return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tango") == TextToSpeech.SUCCESS;
        }
    }

    @Override
    public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }

    @Override
    protected void onDestroy() { if (tts != null) { tts.stop(); tts.shutdown(); } super.onDestroy(); }
}
