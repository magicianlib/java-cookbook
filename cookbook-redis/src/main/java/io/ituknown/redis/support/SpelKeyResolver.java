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

    @SuppressWarnings("null")
    public String resolve(Method method, Object[] args, String keyExpression) {
        String prefix = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        if (keyExpression == null || keyExpression.isEmpty()) {
            return prefix;
        }
        Expression expression = cache.computeIfAbsent(keyExpression, parser::parseExpression);
        // 根对象刻意留空：维度仅按入参引用求值，留空根对象可使漏写入参引用的表达式直接求值失败，避免误取调用目标对象的属性作为维度
        MethodBasedEvaluationContext context =
                new MethodBasedEvaluationContext(null, method, args, discoverer);
        Object value = expression.getValue(context);
        return value == null ? prefix : prefix + "#" + value;
    }
}
