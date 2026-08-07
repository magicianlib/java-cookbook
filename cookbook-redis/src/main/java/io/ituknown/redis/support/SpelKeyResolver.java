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
 * 限流键解析器。
 * <p>
 * 默认 key 是 类名#方法名，同一个方法的所有调用共用一个配额桶；
 * 填了 key 表达式就再拼上 #表达式结果，按入参把不同调用分到各自的桶。</p>
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

        // 根对象留空：表达式只能引用方法入参（#参数名），引用不到就直接报错，避免误读到目标对象的属性
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, method, args, discoverer);

        Expression expression = cache.computeIfAbsent(keyExpression, parser::parseExpression);
        Object value = expression.getValue(context);
        return value == null ? prefix : prefix + "#" + value;
    }
}
