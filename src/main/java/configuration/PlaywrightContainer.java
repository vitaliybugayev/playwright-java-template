package configuration;

import com.microsoft.playwright.*;

import java.nio.file.Path;

/**
 * Thread-local lifecycle manager for Playwright resources.
 * <p>
 * Creates a single {@link BrowserObject} per test thread on-demand and reuses its
 * {@link BrowserContext} across tests while creating a fresh {@link Page} for each test.
 * Provides helper methods to reset/close the page and obtain video artifacts.
 */
public class PlaywrightContainer {

    /** Holds {@link Browser}, {@link BrowserContext} and the current {@link Page} per thread. */
    public static ThreadLocal<BrowserObject> browserStorage = new ThreadLocal<>();

    /**
     * Gets the current thread's {@link BrowserObject}, creating it if necessary.
     * <ul>
     *   <li>Launch options are resolved from {@link Config#isHeadless()} and {@link Config#getSlowMo()}.</li>
     *   <li>Context is configured with base URL and video recording into {@code build/reports/videos}.</li>
     * </ul>
     * @return alive {@link BrowserObject} bound to the current thread.
     */
    public static BrowserObject browser() {
        BrowserObject browserObject = browserStorage.get();
        if (browserObject != null && !browserObject.isValid()) {
            closeBrowser();
            browserObject = null;
        }

        if (browserObject != null) {
            return browserObject;
        }

        try {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
            launchOptions.setHeadless(Config.isHeadless()).setSlowMo(Config.getSlowMo());

            Config.ensureReportsSubdirs();
            Path videosDir = Config.VIDEOS_DIR();

            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setBaseURL(Config.getBaseUrl())
                    .setRecordVideoDir(videosDir);

            Playwright playwright = Playwright.create();
            Browser browser = BrowserFactory.createBrowser(playwright, BrowserFactory.getBrowserType(), launchOptions);
            BrowserContext browserContext = browser.newContext(contextOptions);

            Page page = browserContext.newPage();

            browserObject = new BrowserObject(playwright, browser, browserContext, page);
            browserStorage.set(browserObject);

            return browserObject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Playwright browser: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the current page (if any), returns the path to the previous video (if recorded),
     * and creates a new page within the same browser context.
     * @return path to the previous page video or {@code null}.
     */
    public static Path resetPageForNewTest() {
        BrowserObject current = browser();
        Path previousVideo = null;
        try {
            Page page = current.page();
            if (page != null) {
                Video v = null;
                try { v = page.video(); } catch (Throwable ignored) {}
                try { page.close(); } catch (Throwable ignored) {}
                if (v != null) {
                    try { previousVideo = v.path(); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        Page newPage = current.browserContext().newPage();
        browserStorage.set(new BrowserObject(current.playwright(), current.browser(), current.browserContext(), newPage));
        return previousVideo;
    }

    /**
     * Closes the current page and returns its video file path if available.
     */
    public static Path closeCurrentPageAndGetVideo() {
        BrowserObject current = browserStorage.get();
        if (current == null) return null;
        Path video = null;
        try {
            Page page = current.page();
            if (page != null && !page.isClosed()) {
                Video v = null;
                try { v = page.video(); } catch (Throwable ignored) {}
                try { page.close(); } catch (Throwable ignored) {}
                if (v != null) {
                    try { video = v.path(); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return video;
    }

    /**
     * Fully disposes of the thread's {@link BrowserObject} and removes it from storage.
     * Safe to call multiple times.
     */
    public static void closeBrowser() {
        BrowserObject browserObject = browserStorage.get();
        if (browserObject != null) {
            try (browserObject) {
                // AutoCloseable.close() will handle resource cleanup
            } catch (Exception ignored) {}
            browserStorage.remove();
        }
    }
}
