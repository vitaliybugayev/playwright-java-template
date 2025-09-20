package pages;

import com.microsoft.playwright.Page;
import configuration.reporter.Step;

/**
 * Minimal example page object used by demo tests.
 */
public class DemoPage {
    private final Page page;

    public DemoPage(Page page) { this.page = page; }

    @Step("Navigate to page: {0}")
    public void navigateTo(String url) {
        page.navigate(url);
    }

    @Step("Read page title")
    public String readTitle() { return page.title(); }
}
