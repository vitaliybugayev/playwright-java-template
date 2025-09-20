package tests;

import configuration.Config;
import org.junit.jupiter.api.Test;
import pages.DemoPage;

import static configuration.PlaywrightContainer.browser;
import static org.assertj.core.api.Assertions.assertThat;

public class DemoTest extends BaseTest {

    @Test
    void openBaseUrlAndVerifyTitle() {
        var page = browser().page();
        var demo = new DemoPage(page);
        var url = Config.getBaseUrl();
        demo.navigateTo(url);
        var title = demo.readTitle();
        assertThat(title).as("page title").isNotBlank();
    }
}
