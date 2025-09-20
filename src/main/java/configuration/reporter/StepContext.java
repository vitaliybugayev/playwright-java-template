package configuration.reporter;

/**
 * Thread-local context storing the current step index and name.
 * Used by the reporting extension to indicate which step failed.
 */
public final class StepContext {
    private StepContext() {}

    private static final class Data {
        int index = 0;
        String name = null;
    }

    private static final ThreadLocal<Data> TL = ThreadLocal.withInitial(Data::new);

    /** Resets step counters for a new test execution. */
    public static void reset() {
        Data d = TL.get();
        d.index = 0;
        d.name = null;
    }

    /** Clears thread-local storage. */
    public static void clear() { TL.remove(); }

    /** Increments the step index and records the current step name. */
    public static void next(String stepName) {
        Data d = TL.get();
        d.index += 1;
        d.name = stepName;
    }

    /** Returns the 1-based index of the current step. */
    public static int getIndex() { return TL.get().index; }
    /** Returns the last recorded step name. */
    public static String getName() { return TL.get().name; }
}
