package com.bbrowser.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String SEARCH_DUCKDUCKGO = "https://duckduckgo.com/?q=";
    private static final String SEARCH_GOOGLE = "https://www.google.com/search?q=";
    private static final String SEARCH_BING = "https://www.bing.com/search?q=";

    private RelativeLayout mainRootLayout;
    private LinearLayout topBar, bottomBar, urlContainer;
    private WebView webView;
    private EditText urlInput;
    private ImageButton btnSettings, btnNavBack, btnNavForward, btnNavReload;
    private ProgressBar progressBar;
    private ScrollView homeLayout, panelSettings;
    private FrameLayout fullScreenContainer;
    private LinearLayout panelHistory;

    private RadioGroup rgTheme, rgSearchEngine;
    private RadioButton rbDarkTheme, rbLightTheme, rbDuck, rbGoogle, rbBing;
    private Button btnShowHistory, btnClearCookies, btnCloseSettings, btnClearHistoryList, btnCloseHistory;
    private TextView txtWebViewVersion, txtHistoryContent, txtHomeTitle, txtHomeSub;
    private TextView lblSettingsTitle, lblTheme, lblSearch, lblData, lblAboutHeader;

    private SharedPreferences preferences;
    private boolean isDarkMode = true;
    private String currentSearchEngine = SEARCH_DUCKDUCKGO;
    private String defaultUserAgent = "";

    // Developer Shield Toggles
    private boolean isAdBlockEnabled = true;
    private boolean isTrackerBlockEnabled = true;
    private boolean isAntiFingerprintEnabled = true;
    private Switch swAdBlock, swTrackerBlock, swAntiFingerprint;

    private final List<String> historyList = new ArrayList<>();
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;

    // Blocklists for Developer Shield
    private final Set<String> adServers = new HashSet<>();
    private final Set<String> trackerServers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forceEnglishLocale();
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("BbrowserPrefs", MODE_PRIVATE);

        initViews();
        hideLegacyLanguageOptions();
        initShieldBlocklists();
        setupDeveloperShieldUI();
        loadPreferences();
        setupWebView();
        detectWebViewVersion();
        setupListeners();
        applyThemeUI();
    }

    private void forceEnglishLocale() {
        Locale locale = new Locale("en", "US");
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void initViews() {
        mainRootLayout = findViewById(R.id.mainRootLayout);
        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);
        urlContainer = findViewById(R.id.urlContainer);

        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        btnSettings = findViewById(R.id.btnSettings);
        btnNavBack = findViewById(R.id.btnNavBack);
        btnNavForward = findViewById(R.id.btnNavForward);
        btnNavReload = findViewById(R.id.btnNavReload);
        progressBar = findViewById(R.id.progressBar);
        homeLayout = findViewById(R.id.homeLayout);
        fullScreenContainer = findViewById(R.id.fullScreenContainer);

        panelSettings = findViewById(R.id.panelSettings);
        panelHistory = findViewById(R.id.panelHistory);

        rgTheme = findViewById(R.id.rgTheme);
        rbDarkTheme = findViewById(R.id.rbDarkTheme);
        rbLightTheme = findViewById(R.id.rbLightTheme);

        rgSearchEngine = findViewById(R.id.rgSearchEngine);
        rbDuck = findViewById(R.id.rbDuck);
        rbGoogle = findViewById(R.id.rbGoogle);
        rbBing = findViewById(R.id.rbBing);

        btnShowHistory = findViewById(R.id.btnShowHistory);
        btnClearCookies = findViewById(R.id.btnClearCookies);
        btnCloseSettings = findViewById(R.id.btnCloseSettings);
        btnClearHistoryList = findViewById(R.id.btnClearHistoryList);
        btnCloseHistory = findViewById(R.id.btnCloseHistory);

        txtWebViewVersion = findViewById(R.id.txtWebViewVersion);
        txtHistoryContent = findViewById(R.id.txtHistoryContent);
        txtHomeTitle = findViewById(R.id.txtHomeTitle);
        txtHomeSub = findViewById(R.id.txtHomeSub);

        lblSettingsTitle = findViewById(R.id.lblSettingsTitle);
        lblTheme = findViewById(R.id.lblTheme);
        lblSearch = findViewById(R.id.lblSearch);
        lblData = findViewById(R.id.lblData);
        lblAboutHeader = findViewById(R.id.lblAboutHeader);
    }

    private void hideLegacyLanguageOptions() {
        Spinner spinnerLanguage = findViewById(R.id.spinnerLanguage);
        TextView lblLanguage = findViewById(R.id.lblLanguage);
        if (spinnerLanguage != null) spinnerLanguage.setVisibility(View.GONE);
        if (lblLanguage != null) lblLanguage.setVisibility(View.GONE);
    }

    @SuppressLint("SetTextI18n")
    private void setupDeveloperShieldUI() {
        try {
            LinearLayout settingsContentLayout = (LinearLayout) panelSettings.getChildAt(0);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(Color.parseColor("#475569"));
            LinearLayout.LayoutParams dividerParams = (LinearLayout.LayoutParams) divider.getLayoutParams();
            dividerParams.setMargins(0, 40, 0, 40);

            TextView lblShield = new TextView(this);
            lblShield.setText("Developer Shield (Privacy & Security)");
            lblShield.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            lblShield.setTypeface(null, Typeface.BOLD);
            lblShield.setTextColor(Color.parseColor("#10B981"));
            lblShield.setPadding(0, 0, 0, 20);

            LinearLayout shieldContainer = new LinearLayout(this);
            shieldContainer.setOrientation(LinearLayout.VERTICAL);
            shieldContainer.setBackgroundColor(Color.parseColor("#1E293B"));
            shieldContainer.setPadding(30, 30, 30, 30);

            swAdBlock = new Switch(this);
            swAdBlock.setText("Strict Ad Blocker");
            swAdBlock.setTextColor(Color.parseColor("#94A3B8"));
            swAdBlock.setTextSize(15);
            swAdBlock.setPadding(0, 10, 0, 10);

            swTrackerBlock = new Switch(this);
            swTrackerBlock.setText("Anti-Tracking Protection");
            swTrackerBlock.setTextColor(Color.parseColor("#94A3B8"));
            swTrackerBlock.setTextSize(15);
            swTrackerBlock.setPadding(0, 10, 0, 10);

            swAntiFingerprint = new Switch(this);
            swAntiFingerprint.setText("Anti-Fingerprint (Canvas/WebGL Spoofing)");
            swAntiFingerprint.setTextColor(Color.parseColor("#94A3B8"));
            swAntiFingerprint.setTextSize(15);
            swAntiFingerprint.setPadding(0, 10, 0, 10);

            shieldContainer.addView(lblShield);
            shieldContainer.addView(swAdBlock);
            shieldContainer.addView(swTrackerBlock);
            shieldContainer.addView(swAntiFingerprint);

            int dataIndex = settingsContentLayout.indexOfChild(lblData);
            if(dataIndex != -1) {
                settingsContentLayout.addView(divider, dataIndex);
                settingsContentLayout.addView(shieldContainer, dataIndex + 1);
            } else {
                settingsContentLayout.addView(divider);
                settingsContentLayout.addView(shieldContainer);
            }

            swAdBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isAdBlockEnabled = isChecked;
                savePreferences();
            });
            swTrackerBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isTrackerBlockEnabled = isChecked;
                savePreferences();
            });
            swAntiFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isAntiFingerprintEnabled = isChecked;
                savePreferences();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPreferences() {
        isDarkMode = preferences.getBoolean("dark_mode", true);
        currentSearchEngine = preferences.getString("search_engine", SEARCH_DUCKDUCKGO);

        isAdBlockEnabled = preferences.getBoolean("shield_adblock", true);
        isTrackerBlockEnabled = preferences.getBoolean("shield_tracker", true);
        isAntiFingerprintEnabled = preferences.getBoolean("shield_fingerprint", true);

        rbDarkTheme.setChecked(isDarkMode);
        rbLightTheme.setChecked(!isDarkMode);

        if (currentSearchEngine.equals(SEARCH_GOOGLE)) rbGoogle.setChecked(true);
        else if (currentSearchEngine.equals(SEARCH_BING)) rbBing.setChecked(true);
        else rbDuck.setChecked(true);

        if(swAdBlock != null) swAdBlock.setChecked(isAdBlockEnabled);
        if(swTrackerBlock != null) swTrackerBlock.setChecked(isTrackerBlockEnabled);
        if(swAntiFingerprint != null) swAntiFingerprint.setChecked(isAntiFingerprintEnabled);
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("dark_mode", isDarkMode);
        editor.putString("search_engine", currentSearchEngine);
        editor.putBoolean("shield_adblock", isAdBlockEnabled);
        editor.putBoolean("shield_tracker", isTrackerBlockEnabled);
        editor.putBoolean("shield_fingerprint", isAntiFingerprintEnabled);
        editor.apply();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setGeolocationEnabled(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        defaultUserAgent = settings.getUserAgentString();
        String cleanUA = defaultUserAgent.replaceAll("; wv", "").replaceAll("tr-TR", "en-US");
        settings.setUserAgentString(cleanUA + " Bbrowser/1.0 (DeveloperShield/Active)");

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, !isTrackerBlockEnabled);

        webView.setWebViewClient(new BbrowserWebViewClient());
        webView.setWebChromeClient(new BbrowserWebChromeClient());
    }

    private void detectWebViewVersion() {
        try {
            PackageInfo info = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                info = WebView.getCurrentWebViewPackage();
            } else {
                PackageManager pm = getPackageManager();
                info = pm.getPackageInfo("com.google.android.webview", 0);
            }
            if (info != null) txtWebViewVersion.setText("WebView Core Version: " + info.versionName);
            else txtWebViewVersion.setText("WebView Core Version: Not Detected");
        } catch (Exception e) {
            txtWebViewVersion.setText("WebView Core Version: Unknown");
        }
    }

    private void setupListeners() {
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleUrlOrSearch();
                return true;
            }
            return false;
        });

        btnSettings.setOnClickListener(v -> {
            if (panelSettings.getVisibility() == View.VISIBLE) {
                panelSettings.setVisibility(View.GONE);
            } else {
                panelHistory.setVisibility(View.GONE);
                panelSettings.setVisibility(View.VISIBLE);
            }
        });

        btnNavBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
            else showHomeScreen();
        });

        btnNavForward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        btnNavReload.setOnClickListener(v -> {
            if (webView.getVisibility() == View.VISIBLE) webView.reload();
        });

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            isDarkMode = (checkedId == R.id.rbDarkTheme);
            applyThemeUI();
            savePreferences();
        });

        rgSearchEngine.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbGoogle) currentSearchEngine = SEARCH_GOOGLE;
            else if (checkedId == R.id.rbBing) currentSearchEngine = SEARCH_BING;
            else currentSearchEngine = SEARCH_DUCKDUCKGO;
            savePreferences();
        });

        btnShowHistory.setOnClickListener(v -> {
            panelSettings.setVisibility(View.GONE);
            updateHistoryUI();
            panelHistory.setVisibility(View.VISIBLE);
        });

        btnClearCookies.setOnClickListener(v -> {
            webView.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
            Toast.makeText(this, "Cookies and cache cleared!", Toast.LENGTH_SHORT).show();
        });

        btnClearHistoryList.setOnClickListener(v -> {
            historyList.clear();
            webView.clearHistory();
            updateHistoryUI();
            Toast.makeText(this, "Browsing history cleared!", Toast.LENGTH_SHORT).show();
        });

        btnCloseHistory.setOnClickListener(v -> panelHistory.setVisibility(View.GONE));
        btnCloseSettings.setOnClickListener(v -> {
            panelSettings.setVisibility(View.GONE);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !isTrackerBlockEnabled);
            if(webView.getVisibility() == View.VISIBLE) webView.reload();
        });
    }

    private void updateHistoryUI() {
        StringBuilder sb = new StringBuilder();
        if (historyList.isEmpty()) {
            sb.append("No browsing history.");
        } else {
            for (int i = historyList.size() - 1; i >= 0; i--) {
                sb.append("• ").append(historyList.get(i)).append("\n\n");
            }
        }
        txtHistoryContent.setText(sb.toString());
    }

    private void applyThemeUI() {
        if (isDarkMode) {
            mainRootLayout.setBackgroundColor(Color.parseColor("#0F172A"));
            topBar.setBackgroundColor(Color.parseColor("#1E293B"));
            bottomBar.setBackgroundColor(Color.parseColor("#1E293B"));
            urlContainer.setBackgroundColor(Color.parseColor("#334155"));
            panelSettings.setBackgroundColor(Color.parseColor("#1E293B"));

            urlInput.setTextColor(Color.parseColor("#FFFFFF"));
            urlInput.setHintTextColor(Color.parseColor("#94A3B8"));
            txtHomeTitle.setTextColor(Color.parseColor("#38BDF8"));
            txtHomeSub.setTextColor(Color.parseColor("#94A3B8"));

            lblSettingsTitle.setTextColor(Color.parseColor("#38BDF8"));
            if(lblTheme != null) lblTheme.setTextColor(Color.parseColor("#94A3B8"));
            if(lblSearch != null) lblSearch.setTextColor(Color.parseColor("#94A3B8"));
            if(lblData != null) lblData.setTextColor(Color.parseColor("#94A3B8"));
            if(lblAboutHeader != null) lblAboutHeader.setTextColor(Color.parseColor("#94A3B8"));

            rbDarkTheme.setTextColor(Color.parseColor("#FFFFFF"));
            rbLightTheme.setTextColor(Color.parseColor("#FFFFFF"));
            rbDuck.setTextColor(Color.parseColor("#FFFFFF"));
            rbGoogle.setTextColor(Color.parseColor("#FFFFFF"));
            rbBing.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            mainRootLayout.setBackgroundColor(Color.parseColor("#E0F2FE"));
            topBar.setBackgroundColor(Color.parseColor("#BAE6FD"));
            bottomBar.setBackgroundColor(Color.parseColor("#BAE6FD"));
            urlContainer.setBackgroundColor(Color.parseColor("#FFFFFF"));
            panelSettings.setBackgroundColor(Color.parseColor("#F0F9FF"));

            urlInput.setTextColor(Color.parseColor("#0F172A"));
            urlInput.setHintTextColor(Color.parseColor("#64748B"));
            txtHomeTitle.setTextColor(Color.parseColor("#0284C7"));
            txtHomeSub.setTextColor(Color.parseColor("#475569"));

            lblSettingsTitle.setTextColor(Color.parseColor("#0284C7"));
            if(lblTheme != null) lblTheme.setTextColor(Color.parseColor("#475569"));
            if(lblSearch != null) lblSearch.setTextColor(Color.parseColor("#475569"));
            if(lblData != null) lblData.setTextColor(Color.parseColor("#475569"));
            if(lblAboutHeader != null) lblAboutHeader.setTextColor(Color.parseColor("#475569"));

            rbDarkTheme.setTextColor(Color.parseColor("#0F172A"));
            rbLightTheme.setTextColor(Color.parseColor("#0F172A"));
            rbDuck.setTextColor(Color.parseColor("#0F172A"));
            rbGoogle.setTextColor(Color.parseColor("#0F172A"));
            rbBing.setTextColor(Color.parseColor("#0F172A"));
        }
    }

    private void handleUrlOrSearch() {
        String input = urlInput.getText().toString().trim();
        if (input.isEmpty()) return;

        hideKeyboard();
        if (input.startsWith("http://") || input.startsWith("https://")) {
            loadUrl(input);
        } else if (input.contains(".") && !input.contains(" ")) {
            loadUrl("https://" + input);
        } else {
            loadUrl(currentSearchEngine + Uri.encode(input));
        }
    }

    private void loadUrl(String url) {
        homeLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        panelSettings.setVisibility(View.GONE);
        panelHistory.setVisibility(View.GONE);

        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("Accept-Language", "en-US,en;q=0.9");
        extraHeaders.put("DNT", "1");
        webView.loadUrl(url, extraHeaders);
    }

    private void showHomeScreen() {
        hideKeyboard();
        webView.setVisibility(View.GONE);
        homeLayout.setVisibility(View.VISIBLE);
        panelSettings.setVisibility(View.GONE);
        panelHistory.setVisibility(View.GONE);
        urlInput.setText("");
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            WebChromeClient chromeClient = new BbrowserWebChromeClient();
            chromeClient.onHideCustomView();
        } else if (panelHistory.getVisibility() == View.VISIBLE) {
            panelHistory.setVisibility(View.GONE);
        } else if (panelSettings.getVisibility() == View.VISIBLE) {
            panelSettings.setVisibility(View.GONE);
        } else if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else if (webView.getVisibility() == View.VISIBLE) {
            showHomeScreen();
        } else {
            super.onBackPressed();
        }
    }

    /* -----------------------------------------------------------------------------------------
     * DEVELOPER SHIELD: BLOCKLIST INITIALIZATION
     * -----------------------------------------------------------------------------------------*/
    private void initShieldBlocklists() {
        String[] ads = {
            "doubleclick.net", "adservice.google.com", "googlesyndication.com", "adsystem.com",
            "adtech.de", "advertising.com", "amazon-adsystem.com", "adnxs.com", "criteo.com",
            "outbrain.com", "taboola.com", "adroll.com", "pubmatic.com", "rubiconproject.com",
            "openx.net", "ads.yahoo.com", "yieldmanager.com", "moatads.com", "adform.net",
            "smartadserver.com", "casalemedia.com", "zedo.com", "exponential.com", "advertising.apple.com",
            "ads.twitter.com", "adsafeprotected.com", "bidswitch.net", "indexexchange.com",
            "sovrn.com", "media.net", "epom.com", "propellerads.com", "infolinks.com"
        };
        for(String ad : ads) adServers.add(ad);

        String[] trackers = {
            "google-analytics.com", "analytics.yahoo.com", "facebook.net", "facebook.com/tr",
            "pixel.facebook.com", "hotjar.com", "scorecardresearch.com", "quantserve.com",
            "mixpanel.com", "segment.com", "optimizely.com", "crazyegg.com", "statcounter.com",
            "chartbeat.com", "histats.com", "yandex.ru/metrika", "mc.yandex.ru", "clarity.ms",
            "mouseflow.com", "fullstory.com", "appsflyer.com", "adjust.com", "branch.io",
            "newrelic.com", "datadoghq-browser-agent.com", "bugsnag.com", "sentry.io",
            "amplitude.com", "kissmetrics.io", "matomo.org", "piwik.pro"
        };
        for(String trk : trackers) trackerServers.add(trk);
    }

    /* -----------------------------------------------------------------------------------------
     * DEVELOPER SHIELD: ANTI-FINGERPRINT JAVASCRIPT INJECTION
     * -----------------------------------------------------------------------------------------*/
    private String getAntiFingerprintScript() {
        return "javascript:(function() {" +
            "  try {" +
            "    const nav = window.navigator;" +
            "    const fakeHardwareConcurrency = 4;" +
            "    const fakeDeviceMemory = 8;" +
            "    Object.defineProperty(nav, 'hardwareConcurrency', { get: () => fakeHardwareConcurrency });" +
            "    Object.defineProperty(nav, 'deviceMemory', { get: () => fakeDeviceMemory });" +
            "    Object.defineProperty(nav, 'doNotTrack', { get: () => '1' });" +
            "    Object.defineProperty(nav, 'language', { get: () => 'en-US' });" +
            "    Object.defineProperty(nav, 'languages', { get: () => ['en-US', 'en'] });" +
            "    Object.defineProperty(nav, 'plugins', { get: () => [1, 2, 3] });" +
            "    " +
            "    const originalGetContext = HTMLCanvasElement.prototype.getContext;" +
            "    HTMLCanvasElement.prototype.getContext = function(type, attributes) {" +
            "      const context = originalGetContext.call(this, type, attributes);" +
            "      if (type === '2d' && context) {" +
            "        const originalGetImageData = context.getImageData;" +
            "        context.getImageData = function(x, y, w, h) {" +
            "          const imageData = originalGetImageData.call(this, x, y, w, h);" +
            "          for (let i = 0; i < imageData.data.length; i += 4) {" +
            "            imageData.data[i] = imageData.data[i] ^ 1;" +
            "          }" +
            "          return imageData;" +
            "        };" +
            "      }" +
            "      return context;" +
            "    };" +
            "    const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;" +
            "    HTMLCanvasElement.prototype.toDataURL = function() {" +
            "       const fakeContext = originalGetContext.call(this, '2d');" +
            "       if(fakeContext) { fakeContext.fillStyle = 'rgba(255,255,255,0.01)'; fakeContext.fillRect(0,0,1,1); }" +
            "       return originalToDataURL.apply(this, arguments);" +
            "    };" +
            "    " +
            "    if (window.WebGLRenderingContext) {" +
            "      const getParameter = WebGLRenderingContext.prototype.getParameter;" +
            "      WebGLRenderingContext.prototype.getParameter = function(parameter) {" +
            "        if (parameter === 37445) return 'Google Inc.'; " +
            "        if (parameter === 37446) return 'ANGLE (Intel(R) HD Graphics Direct3D11)'; " +
            "        return getParameter.call(this, parameter);" +
            "      };" +
            "    }" +
            "  } catch(e) {} " +
            "})();";
    }

    /* -----------------------------------------------------------------------------------------
     * WEBVIEW CLIENTS
     * -----------------------------------------------------------------------------------------*/
    private class BbrowserWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress < 100) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                onHideCustomView();
                return;
            }
            customView = view;
            customViewCallback = callback;
            fullScreenContainer.addView(customView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            fullScreenContainer.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) return;
            fullScreenContainer.removeView(customView);
            fullScreenContainer.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            customView = null;
            if (customViewCallback != null) customViewCallback.onCustomViewHidden();
        }
    }

    private class BbrowserWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            urlInput.setText(url);
            if (!historyList.contains(url)) {
                historyList.add(url);
            }
            if (isAntiFingerprintEnabled) {
                view.evaluateJavascript(getAntiFingerprintScript(), null);
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString().toLowerCase();

            if (isAdBlockEnabled) {
                for (String adHost : adServers) {
                    if (url.contains(adHost)) {
                        return getEmptyResponse();
                    }
                }
            }

            if (isTrackerBlockEnabled) {
                for (String trkHost : trackerServers) {
                    if (url.contains(trkHost)) {
                        return getEmptyResponse();
                    }
                }
            }

            return super.shouldInterceptRequest(view, request);
        }

        private WebResourceResponse getEmptyResponse() {
            ByteArrayInputStream emptyData = new ByteArrayInputStream("".getBytes());
            return new WebResourceResponse("text/plain", "UTF-8", emptyData);
        }
    }
}
