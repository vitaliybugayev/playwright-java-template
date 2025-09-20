package configuration.reporter;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import configuration.PlaywrightContainer;
import configuration.Config;

import java.util.Base64;
import java.util.function.Supplier;

/**
 * Utilities for executing logical steps with automatic reporting.
 * <p>
 * Provides wrappers that:
 * <ul>
 *   <li>Record step number and name.</li>
 *   <li>Execute the body and on failure attach a screenshot (if enabled) and a formatted stack trace.</li>
 * </ul>
 * Also offers "sneaky" variants that rethrow checked exceptions without declaring them.
 */
public final class StepLifecycle {
  private StepLifecycle(){}

  @FunctionalInterface public interface ThrowingSupplier<T> { T get() throws Throwable; }
  @FunctionalInterface public interface ThrowingRunnable { void run() throws Throwable; }

  /**
   * Executes a step, reporting failures with optional screenshot attachment.
   */
  public static <T> T run(String name, ThrowingSupplier<T> body) throws Throwable {
    StepContext.next(name);
    try {
      T result = body.get();
      return result;
    } catch (Throwable t) {
      attachFailure(name, t);
      throw t;
    }
  }

  /** Executes a void step variant. */
  public static void run(String name, ThrowingRunnable body) throws Throwable { run(name, () -> { body.run(); return null; }); }

  /**
   * Attaches a FAIL log to the HTML report and an inline screenshot (if configured).
   */
  private static void attachFailure(String name, Throwable t) {
    try {
      if (Config.isScreenshotOnStepFailure()) {
        byte[] png = PlaywrightContainer.browser().page().screenshot();
        String b64 = Base64.getEncoder().encodeToString(png);
        HtmlReporter.reporter.get().log(
          Status.FAIL,
          name + "<br/>" + HtmlReporter.escapeHtml(String.valueOf(t.getMessage()))
            + "<br/>" + HtmlReporter.toCollapsibleStackHtml(t),
          MediaEntityBuilder.createScreenCaptureFromBase64String(b64).build()
        );
        return;
      }
    } catch (Throwable sse) {
      HtmlReporter.reporter.get().log(Status.WARNING, "Unable to capture step screenshot: " + sse.getMessage());
    }
    HtmlReporter.reporter.get().log(Status.FAIL,
      name + " (no screenshot)<br/>" + HtmlReporter.toCollapsibleStackHtml(t));
  }

  /** Executes a step and sneaky-throws checked exceptions. */
  public static <T> T sneaky(String name, Supplier<T> body) {
    try { return run(name, () -> body.get()); }
    catch (Throwable t) { return sneakyThrow(t); }
  }
  /** Executes a void step and sneaky-throws checked exceptions. */
  public static void sneaky(String name, Runnable body) {
    try { run(name, () -> body.run()); }
    catch (Throwable t) { sneakyThrow(t); }
  }
  @SuppressWarnings("unchecked")
  private static <E extends Throwable, R> R sneakyThrow(Throwable t) throws E { throw (E) t; }
}
