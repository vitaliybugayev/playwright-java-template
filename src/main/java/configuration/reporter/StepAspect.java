package configuration.reporter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.platform.commons.util.StringUtils;

import java.util.Objects;

/**
 * Aspect intercepting methods annotated with {@link Step} and delegating execution to {@link StepLifecycle}.
 * <p>
 * Supports parameter interpolation in step names using either indexed placeholders (e.g. {0})
 * or parameter names (e.g. {userId}).
 */
@Aspect
public class StepAspect {

    @Pointcut("@annotation(Step) && execution(* *(..))")
    public void stepMethod() {}

    @Around("stepMethod()")
    public Object retryMethodAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        Step stepAnn = signature.getMethod().getAnnotation(Step.class);
        String userDefinedName = stepAnn == null ? null : stepAnn.value();

        String stepNameTemplate = !StringUtils.isBlank(userDefinedName) ? userDefinedName : methodName;
        String stepName = formatStepName(stepNameTemplate, signature.getParameterNames(), joinPoint.getArgs());

        return StepLifecycle.run(stepName, () -> joinPoint.proceed());
    }

    /** Replaces placeholders in the template with argument values. */
    private static String formatStepName(String template, String[] paramNames, Object[] args) {
        if (template == null) return "";
        String result = template;
        if (paramNames != null && args != null) {
            for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                String name = paramNames[i];
                String value = safeToString(args[i]);
                result = result.replace("{" + name + "}", value);
            }
            for (int i = 0; i < args.length; i++) {
                String value = safeToString(args[i]);
                result = result.replace("{" + i + "}", value);
            }
        }
        return result;
    }

    /** Safe toString that never throws. */
    private static String safeToString(Object o) {
        try { return Objects.toString(o); } catch (Throwable t) { return String.valueOf(o); }
    }
}
