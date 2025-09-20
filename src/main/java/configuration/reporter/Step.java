package configuration.reporter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a logical reporting step.
 * <p>
 * The associated {@link configuration.reporter.StepAspect} intercepts annotated methods, formats the step name,
 * and delegates execution to {@link StepLifecycle}, handling failure attachments (e.g., screenshots) automatically.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Step {
    /**
     * Optional step name template. Supports placeholders like {@code {0}}, {@code {argName}} for method parameters.
     */
    String value() default "";
}
