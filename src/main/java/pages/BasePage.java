package pages;

import com.microsoft.playwright.Page;

import static configuration.PlaywrightContainer.browser;

/**
 * Base class for page objects, providing convenient access to the current {@link Page}.
 */
public class BasePage {
    protected Page page = browser().page();
}
