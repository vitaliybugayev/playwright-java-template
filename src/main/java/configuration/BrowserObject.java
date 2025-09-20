package configuration;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Immutable container for Playwright resources bound to a single thread.
 * <p>
 * The record implements {@link AutoCloseable} and closes resources in
 * reverse-dependency order: {@link Page} → {@link BrowserContext} → {@link Browser} → {@link Playwright}.
 */
public record BrowserObject(Playwright playwright, Browser browser, BrowserContext browserContext, Page page) implements AutoCloseable {

    /**
     * Closes all underlying Playwright resources, suppressing any exceptions.
     */
    @Override
    public void close() {
        try { page.close(); } catch (Exception ignored) {}
        try { browserContext.close(); } catch (Exception ignored) {}
        try { browser.close(); } catch (Exception ignored) {}
        try { playwright.close(); } catch (Exception ignored) {}
    }

    /**
     * Lightweight check whether the browser is connected and the page is still open.
     * Intended for defensive use in test lifecycle hooks.
     */
    public boolean isValid() {
        try {
            return browser.isConnected() && !page.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
