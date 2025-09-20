package configuration.reporter;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.microsoft.playwright.Tracing;
import configuration.Config;
import configuration.PlaywrightContainer;
import org.junit.jupiter.api.extension.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * JUnit 5 extension integrating ExtentReports with Playwright artifacts.
 * <p>
 * For each test class it:
 * <ul>
 *   <li>Initialises an HTML report under {@code build/reports}.</li>
 *   <li>Validates configuration and logs a clear error early if misconfigured.</li>
 *   <li>Starts Playwright tracing and resets the page per test.</li>
 *   <li>On failure, attaches screenshot and pretty stack; always exports a trace ZIP; logs video path if available.</li>
 * </ul>
 */
public class ExtendReport implements BeforeAllCallback, BeforeTestExecutionCallback, AfterAllCallback, AfterTestExecutionCallback {

    private static final String KEY_REPORTER = "reporter";
    private static final String KEY_MISCONFIG = "misconfig";

    private static ExtensionContext.Store store(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(ExtendReport.class, context.getRequiredTestClass()));
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Config.ensureReportsSubdirs();
        String filename = Config.REPORTS_DIR().resolve(context.getDisplayName() + "_Results.html").toString();
        HtmlReporter reporter = new HtmlReporter(filename);
        store(context).put(KEY_REPORTER, reporter);
        try { Config.reload(); } catch (RuntimeException e) { store(context).put(KEY_MISCONFIG, e.getMessage()); }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        HtmlReporter reporter = store(context).get(KEY_REPORTER, HtmlReporter.class);
        ExtentTest test = reporter.createTest(context.getDisplayName());

        String misconfigurationError = store(context).get(KEY_MISCONFIG, String.class);
        if (misconfigurationError != null) {
            test.fail("Misconfiguration: " + misconfigurationError);
            throw new ExtensionConfigurationException("Misconfiguration: " + misconfigurationError);
        }

        List<String> missing = Config.validateCriticalKeys();
        if (!missing.isEmpty()) {
            HtmlReporter.reporter.get().log(Status.FAIL, "Misconfiguration: " + String.join(", ", missing));
            throw new IllegalStateException("Misconfiguration: " + missing);
        }

        StepContext.reset();
        test.log(Status.INFO, context.getDisplayName() + " - started");

        PlaywrightContainer.resetPageForNewTest();
        try {
            PlaywrightContainer.browser().browserContext().tracing().start(
                    new Tracing.StartOptions()
                            .setScreenshots(true)
                            .setSnapshots(true)
                            .setSources(true)
            );
        } catch (Throwable t) {
            test.log(Status.WARNING, "Could not start tracing: " + t.getMessage());
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        HtmlReporter reporter = store(context).get(KEY_REPORTER, HtmlReporter.class);
        Path tracesDir = Config.TRACES_DIR();
        Path screenshotsDir = Config.SCREENSHOTS_DIR();

        try {
            ExtentTest test = reporter.getTest();
            test.log(Status.INFO, context.getDisplayName() + " - finished");

            boolean isFailed = context.getExecutionException().isPresent();
            if (!isFailed) {
                test.pass(context.getDisplayName() + " - passed");
            } else {
                int n = StepContext.getIndex();
                String name = StepContext.getName();
                HtmlReporter.reporter.get().log(Status.FAIL, "Failed at step #" + n + ": " + HtmlReporter.escapeHtml(String.valueOf(name)));

                Throwable t = context.getExecutionException().orElse(null);
                if (t != null) {
                    String shortMsg = HtmlReporter.toShortError(t);
                    String detailsHtml = HtmlReporter.toCollapsibleStackHtml(t);
                    test.fail(shortMsg);
                    test.fail(detailsHtml);
                }

                if (Config.isScreenshotOnTestFailure()) {
                    try {
                        var browserObject = PlaywrightContainer.browserStorage.get();
                        if (browserObject != null && browserObject.isValid()) {
                            byte[] screenshot = browserObject.page().screenshot();
                            String encode = Base64.getEncoder().encodeToString(screenshot);
                            test.fail("Test Screenshot", MediaEntityBuilder.createScreenCaptureFromBase64String(encode).build());
                            String className = context.getRequiredTestClass().getSimpleName();
                            String methodName = context.getRequiredTestMethod().getName();
                            String fileName = Config.buildArtifactFileName(className, methodName, ".png");
                            Path screenshotPath = screenshotsDir.resolve(fileName);
                            Files.write(screenshotPath, screenshot);
                            test.info("Screenshot file: " + screenshotPath.toAbsolutePath());
                        }
                    } catch (Exception e) {
                        test.log(Status.WARNING, "Could not capture test screenshot: " + e.getMessage());
                    }
                }
            }

            try {
                String className = context.getRequiredTestClass().getSimpleName();
                String methodName = context.getRequiredTestMethod().getName();
                String fileName = Config.buildArtifactFileName(className, methodName, ".zip");
                Path traceZip = tracesDir.resolve(fileName);
                PlaywrightContainer.browser().browserContext().tracing().stop(new com.microsoft.playwright.Tracing.StopOptions().setPath(traceZip));
                String traceInfo = "Trace saved: " + traceZip.toAbsolutePath();
                if (isFailed) test.fail(traceInfo); else test.info(traceInfo);
            } catch (Throwable t) {
                reporter.getTest().log(Status.WARNING, "Could not export trace: " + t.getMessage());
            }

            try {
                Path video = PlaywrightContainer.closeCurrentPageAndGetVideo();
                if (video != null) {
                    String msg = "Video saved: " + video.toAbsolutePath();
                    if (isFailed) reporter.getTest().fail(msg); else reporter.getTest().info(msg);
                }
            } catch (Throwable t) {
                reporter.getTest().log(Status.WARNING, "Could not get video: " + t.getMessage());
            }

            try { PlaywrightContainer.closeBrowser(); } catch (Exception ignored) {}
        } finally {
            try { StepContext.clear(); } catch (Throwable ignored) {}
            try { HtmlReporter.reporter.remove(); } catch (Throwable ignored) {}
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try { PlaywrightContainer.closeBrowser(); } catch (Exception ignored) {}
        try { StepContext.clear(); } catch (Throwable ignored) {}
        try { HtmlReporter.reporter.remove(); } catch (Throwable ignored) {}
        HtmlReporter reporter = store(context).remove(KEY_REPORTER, HtmlReporter.class);
        if (reporter != null) reporter.flush();
        store(context).remove(KEY_MISCONFIG);
    }
}
