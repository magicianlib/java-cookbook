package io.ituknown.redis.support;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流规则解析器
 * <p>
 * 默认使用 类名#方法名 作为限流规则。如果指定了 Key 表达式且能正确解析 Key 值，将
 * 使用 类名#方法名#Value 作为限流规则。</p>
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

        // 根对象刻意留空：维度仅按入参引用求值，留空根对象可使漏写入参引用的表达式直接求值失败，避免误取调用目标对象的属性作为维度
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, method, args, discoverer);

        Expression expression = cache.computeIfAbsent(keyExpression, parser::parseExpression);
        Object value = expression.getValue(context);
        return value == null ? prefix : prefix + "#" + value;
    }
}
