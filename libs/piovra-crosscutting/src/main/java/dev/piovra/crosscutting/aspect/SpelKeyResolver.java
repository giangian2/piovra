package dev.piovra.crosscutting.aspect;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Evaluates the SpEL expressions of the annotations against the intercepted method's arguments.
 *
 * <p>One single place: {@code @Idempotent} and {@code @Audited} share the same syntax, and whoever
 * adds the third annotation will not have to reinvent parameter-name resolution (which requires
 * {@code -parameters} at compile time, an easy detail to get wrong).
 */
final class SpelKeyResolver {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    String resolve(ProceedingJoinPoint joinPoint, String expression) {
        if (expression == null || expression.isBlank()) {
            return "";
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] names = nameDiscoverer.getParameterNames(method);
        if (names != null) {
            for (int i = 0; i < names.length && i < args.length; i++) {
                context.setVariable(names[i], args[i]);
            }
        }
        // Positional too (#a0, #p0), so expressions keep working even without -parameters.
        for (int i = 0; i < args.length; i++) {
            context.setVariable("a" + i, args[i]);
            context.setVariable("p" + i, args[i]);
        }

        Object value = parser.parseExpression(expression).getValue(context);
        if (value == null) {
            throw new IllegalStateException(
                    "expression '" + expression + "' on " + method.getName() + " evaluated to null");
        }
        return value.toString();
    }
}
