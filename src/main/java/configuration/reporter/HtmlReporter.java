package configuration.reporter;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Thin wrapper around ExtentReports with thread-local current test and HTML helpers.
 */
public class HtmlReporter {

    private final ExtentReports reports;
    public static final ThreadLocal<ExtentTest> reporter = new ThreadLocal<>();

    /** Creates an HTML report writer targeting the given file path. */
    public HtmlReporter(String fileName) {
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(fileName);
        reports = new ExtentReports();
        reports.attachReporter(sparkReporter);
    }

    /** Returns a per-thread {@link ExtentTest}, creating it if missing. */
    public ExtentTest createTest(String name) {
        ExtentTest test = reporter.get();
        if (test != null) {
            return test;
        }
        ExtentTest extentTest = reports.createTest(name);
        reporter.set(extentTest);
        return reporter.get();
    }

    /** Flushes pending report events to disk. */
    public void flush() { reports.flush(); }
    /** Returns the current thread's {@link ExtentTest}. */
    public ExtentTest getTest() { return reporter.get(); }

    /** Executes a step with the given name and returns the supplier result. */
    public static <T> T step(String name, java.util.function.Supplier<T> body) { return StepLifecycle.sneaky(name, body); }
    /** Executes a void step with the given name. */
    public static void step(String name, Runnable body) { StepLifecycle.sneaky(name, body); }

    /** Returns a short one-line error description. */
    public static String toShortError(Throwable t) {
        if (t == null) return "Unknown error";
        String message = t.getMessage();
        String type = t.getClass().getName();
        return message == null || message.isBlank() ? type : type + ": " + message;
    }

    /** Converts a Throwable stack trace to string. */
    public static String toStackTraceString(Throwable t) {
        if (t == null) return "";
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** Escapes a string for safe embedding into HTML. */
    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Wraps a pre-formatted stack trace in a collapsible HTML block. */
    public static String toCollapsibleStackHtml(Throwable t) {
        String stack = toStackTraceString(t);
        String escaped = escapeHtml(stack);
        return "<details><summary>Show full stack trace</summary><pre>" + escaped + "</pre></details>";
    }
}
