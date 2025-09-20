package tests;

import configuration.PlaywrightContainer;
import configuration.reporter.ExtendReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base test class that wires reporting via {@link ExtendReport} and ensures the browser is closed
 * after the test class completes.
 */
@ExtendWith(ExtendReport.class)
public class BaseTest {

    @AfterAll
    static void tearDown() {
        PlaywrightContainer.closeBrowser();
    }
}
