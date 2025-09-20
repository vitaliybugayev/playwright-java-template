package configuration.reporter;

/**
 * Ensures AspectJ finds at least one {@link Step} join point, avoiding noisy warnings like
 * "adviceDidNotMatch" when the project compiles without any annotated methods.
 */
public class DummyStepUsage {
    @Step("dummy step")
    public void dummy() {
        // no-op
    }
}
