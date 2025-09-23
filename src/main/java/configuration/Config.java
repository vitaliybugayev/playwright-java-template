package configuration;

/**
 * Centralised configuration loader for the test framework.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Resolve the current environment via system property {@code -Denv} (defaults to {@value DEFAULT_ENV}).</li>
 *   <li>Load {@code /envs/<env>.properties} from the classpath and overlay well‑known system properties
 *   (e.g. {@code -Dbrowser}, {@code -Dheadless}, {@code -Dbase_url}).</li>
 *   <li>Validate critical keys (e.g. {@code BASE_URL}) early and fail fast.</li>
 *   <li>Provide typed getters, artifact directories under {@code build/reports}, and helper methods
 *   for generating stable artifact file names.</li>
 * </ul>
 * The instance keeps a per‑environment cache and can be reloaded at runtime via {@link #reload()}.
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
    public static final String DEFAULT_ENV = "local";
    private static final Set<String> CRITICAL_KEYS = Set.of("BASE_URL");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final Map<String, Properties> envCache = new ConcurrentHashMap<>();
    private static volatile String currentEnv;
    private static volatile Properties properties;

    private Config() {}

    static { reload(); }

    /**
     * Reloads configuration for the active environment and validates critical keys.
     * Safe to call multiple times (idempotent) and from parallel tests.
     */
    public static synchronized void reload() {
        currentEnv = getCurrentEnvironment();
        properties = loadProperties(currentEnv);
        validateCriticalKeysInternal();
    }

    private static String getCurrentEnvironment() {
        String env = System.getProperty("env");
        if (env != null) return env;
        return DEFAULT_ENV;
    }

    private static Properties loadProperties(String env) {
        if (envCache.containsKey(env)) {
            return envCache.get(env);
        }
        Properties props = new Properties();
        loadPropertiesFile(props, "/envs/" + env + ".properties", env);
        overrideFromSystemProperties(props);
        envCache.put(env, props);
        return props;
    }

    private static void loadPropertiesFile(Properties props, String path, String identifier) {
        try (InputStream inputStream = Config.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new RuntimeException("Environment file not found: " + path + " (identifier: " + identifier + ")");
            }
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties from: " + path + " (identifier: " + identifier + ")", e);
        }
    }

    private static void overrideFromSystemProperties(Properties props) {
        checkAndOverride(props, "BASE_URL", "base_url");
        checkAndOverride(props, "HEADLESS", "headless");
        checkAndOverride(props, "SLOWMO", "slowmo");
        checkAndOverride(props, "SCREENSHOT", "screenshot");
        checkAndOverride(props, "SNAPSHOT", "snapshot");
        checkAndOverride(props, "BROWSER_TYPE", "browser");
    }

    private static void checkAndOverride(Properties props, String key, String systemPropertyName) {
        String sysValue = System.getProperty(systemPropertyName);
        if (sysValue != null) {
            props.setProperty(key, sysValue);
        }
    }

    /**
     * Validates presence of critical keys and returns the list of missing ones.
     * Does not throw; callers may decide how to handle misconfiguration.
     */
    public static List<String> validateCriticalKeys() {
        List<String> missingKeys = new ArrayList<>();
        for (String key : CRITICAL_KEYS) {
            if (!properties.containsKey(key) || properties.getProperty(key) == null || properties.getProperty(key).trim().isEmpty()) {
                missingKeys.add(key);
            }
        }
        return missingKeys;
    }

    private static void validateCriticalKeysInternal() {
        List<String> missingKeys = validateCriticalKeys();
        if (!missingKeys.isEmpty()) {
            throw new RuntimeException("Critical configuration keys are missing or empty: " + missingKeys + " for environment: " + currentEnv);
        }
    }

    /**
     * Returns a non-null string value for the given key or throws if absent.
     */
    public static String getString(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Configuration key not found: " + key);
        }
        return value;
    }

    /** Returns string value or default. */
    public static String getString(String key, String defaultValue) { return properties.getProperty(key, defaultValue); }
    /** Returns int value or throws. */
    public static int getInt(String key) { return Integer.parseInt(getString(key)); }
    /** Returns int value or default. */
    public static int getInt(String key, int defaultValue) { String v = properties.getProperty(key); return v == null ? defaultValue : Integer.parseInt(v); }
    /** Returns boolean value or throws. */
    public static boolean getBool(String key) { return Boolean.parseBoolean(getString(key)); }
    /** Returns boolean value or default. */
    public static boolean getBool(String key, boolean defaultValue) { String v = properties.getProperty(key); return v == null ? defaultValue : Boolean.parseBoolean(v); }
    /** Synonym for {@link #getBool(String, boolean)} to match conventional naming. */
    public static boolean getBoolean(String key, boolean defaultValue) { return getBool(key, defaultValue); }
    /** Generic accessor with default. */
    public static String get(String key, String defaultValue) { return getString(key, defaultValue); }

    // Convenience getters
    /** Base URL to be used by Playwright contexts. Required. */
    public static String getBaseUrl() { return getString("BASE_URL"); }
    /** Optional override for specific hosts; falls back to {@link #getBaseUrl()}. */
    public static String getMarketHost() { return getString("MARKET_HOST", getBaseUrl()); }
    /** Headless mode flag; defaults to {@code true}. */
    public static boolean isHeadless() { return getBool("HEADLESS", true); }
    /** SlowMo delay in milliseconds; defaults to {@code 0}. */
    public static int getSlowMo() { return getInt("SLOWMO", 0); }
    /** Whether to capture screenshots on steps; defaults to {@code true}. */
    public static boolean isScreenshotEnabled() { return getBool("SCREENSHOT", true); }
    /** Whether to capture Playwright snapshots; defaults to {@code true}. */
    public static boolean isSnapshotEnabled() { return getBool("SNAPSHOT", true); }
    /** Returns the resolved current environment name. */
    public static String getCurrentEnv() { return currentEnv; }
    /** Whether current execution is CI-like. */
    public static boolean isCi() {
        String ciEnv = System.getenv("CI");
        return "ci".equalsIgnoreCase(currentEnv) || (ciEnv != null && ciEnv.equalsIgnoreCase("true"));
    }
    /** Clears environment cache; effective after next {@link #reload()}. */
    public static void clearCache() { envCache.clear(); }

    // Artifact policies
    /** Returns whether to attach a screenshot when a step fails. */
    public static boolean isScreenshotOnStepFailure() { return getBoolean("ARTIFACT_SCREENSHOT_ON_STEP_FAIL", true); }
    /** Returns whether to attach a screenshot when a test fails. */
    public static boolean isScreenshotOnTestFailure() { return getBoolean("ARTIFACT_SCREENSHOT_ON_TEST_FAIL", true); }
    /** Trace policy: one of {@code always|on-failure|off}. */
    public static String getTracePolicy() { return get("ARTIFACT_TRACE_POLICY", "on-failure"); }
    /** Video policy: one of {@code always|on-failure|off}. */
    public static String getVideoPolicy() { return get("ARTIFACT_VIDEO_POLICY", "always"); }

    // Directories
    /** Root reports directory under the project {@code build/} folder. */
    public static Path getReportsDir() { return Paths.get(System.getProperty("user.dir"), "build", "reports"); }
    /** Default traces directory under reports. */
    public static Path getTracesDir() { return getReportsDir().resolve("traces"); }
    /** Default videos directory under reports. */
    public static Path getVideosDir() { return getReportsDir().resolve("videos"); }
    /** Default screenshots directory under reports. */
    public static Path getScreenshotsDir() { return getReportsDir().resolve("screenshots"); }
    /** Ensures the reports directory exists and returns it. */
    public static Path REPORTS_DIR() { return ensureDir("build/reports"); }
    /** Ensures the traces directory exists and returns it. */
    public static Path TRACES_DIR() { return ensureDir("build/reports/traces"); }
    /** Ensures the videos directory exists and returns it. */
    public static Path VIDEOS_DIR() { return ensureDir("build/reports/videos"); }
    /** Ensures the screenshots directory exists and returns it. */
    public static Path SCREENSHOTS_DIR() { return ensureDir("build/reports/screenshots"); }

    private static Path ensureDir(String path) {
        Path dir = Paths.get(System.getProperty("user.dir")).resolve(path);
        try { Files.createDirectories(dir); } catch (IOException e) { throw new RuntimeException("Failed to create directory: " + dir, e); }
        return dir;
    }

    /**
     * Creates the reports folder structure if missing.
     * Useful for CI and for early artifact preparation.
     */
    public static void ensureReportsSubdirs() {
        try {
            Files.createDirectories(getReportsDir());
            Files.createDirectories(getTracesDir());
            Files.createDirectories(getVideosDir());
            Files.createDirectories(getScreenshotsDir());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create reports directories", e);
        }
    }

    /**
     * Builds a stable artifact file name following the convention:
     * {@code <ClassSimpleName>-<methodName>-<yyyyMMdd-HHmmss><ext>}.
     * Null values are replaced with safe placeholders.
     */
    public static String buildArtifactFileName(String classSimpleName, String methodName, String ext) {
        String safeClass = classSimpleName == null ? "UnknownClass" : classSimpleName;
        String safeMethod = methodName == null ? "unknownMethod" : methodName;
        String ts = LocalDateTime.now().format(TS);
        return safeClass + "-" + safeMethod + "-" + ts + (ext == null ? "" : ext);
    }
}
