package io.ituknown.redis.support;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 限流维度解析：前缀为简单类名与方法名；表达式非空时追加求值结果。
 */
public class SpelKeyResolver {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
    private final ConcurrentHashMap<String, Expression> cache = new ConcurrentHashMap<>();

    public String resolve(Method method, Object[] args, String keyExpression) {
        String prefix = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        if (keyExpression == null || keyExpression.isEmpty()) {
            return prefix;
        }
        Expression expression = cache.computeIfAbsent(keyExpression, parser::parseExpression);
        MethodBasedEvaluationContext context =
                new MethodBasedEvaluationContext(null, method, args, discoverer);
        Object value = expression.getValue(context);
        return prefix + "#" + value;
    }
}
