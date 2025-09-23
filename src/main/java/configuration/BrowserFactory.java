package configuration;

import com.microsoft.playwright.*;

/**
 * Factory for creating Playwright {@link Browser} instances according to configuration.
 */
public class BrowserFactory {

    /** Supported browser engines. */
    public enum BrowserType {
        CHROMIUM, FIREFOX, WEBKIT
    }

    /**
     * Launches and returns a {@link Browser} using the provided Playwright instance and options.
     */
    public static Browser createBrowser(Playwright playwright, BrowserType browserType, com.microsoft.playwright.BrowserType.LaunchOptions launchOptions) {
        return switch (browserType) {
            case CHROMIUM -> playwright.chromium().launch(launchOptions);
            case FIREFOX -> playwright.firefox().launch(launchOptions);
            case WEBKIT -> playwright.webkit().launch(launchOptions);
        };
    }

    /** Returns whether the given browser supports video recording. */
    public static boolean isVideoSupported(BrowserType browserType) {
        // Playwright video recording is supported in Chromium and WebKit.
        // Firefox video recording support is limited/unsupported in some environments.
        return browserType == BrowserType.CHROMIUM || browserType == BrowserType.WEBKIT;
    }

    /**
     * Resolves target browser type from configuration key {@code BROWSER_TYPE} (default: CHROMIUM).
     * Falls back to CHROMIUM on unknown values.
     */
    public static BrowserType getBrowserType() {
        String browserName = Config.getString("BROWSER_TYPE", "CHROMIUM").toUpperCase();
        try {
            return BrowserType.valueOf(browserName);
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown browser type: " + browserName + ". Defaulting to CHROMIUM.");
            return BrowserType.CHROMIUM;
        }
    }
}
