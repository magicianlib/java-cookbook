package io.ituknown.falconmonitor;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * 性能监控方法拦截器
 *
 * @author magicianlib@gmail.com
 */
public class FalconMonitorMethodInterceptor
        implements MethodInterceptor, Ordered, InitializingBean, DisposableBean, ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(FalconMonitorMethodInterceptor.class);

    private final String appId;
    private final String reportServerUrl;

    public FalconMonitorMethodInterceptor(String appId, String reportServerUrl) {
        this.appId = appId;
        this.reportServerUrl = reportServerUrl;
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
    }

    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasLength(appId)) {
            throw new InvalidPropertyException(this.getClass(), "appId", "not set");
        }

        if (!StringUtils.hasLength(reportServerUrl)) {
            throw new InvalidPropertyException(this.getClass(), "reportServerUrl", "not set");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void destroy() {
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object that = invocation.getThis();
        Method method = invocation.getMethod();

        String clazzName = method.getDeclaringClass().getSimpleName();

        // 如果是接口方法, 进一步定位到具体实现类
        // 动态代理类的类名类似 "$Proxy34", 无意义
        if (method.getDeclaringClass().isInterface() && Objects.nonNull(that) && !Proxy.isProxyClass(that.getClass())) {
            clazzName = that.getClass().getSimpleName();
        }

        long start = System.currentTimeMillis();

        String fullMethod = clazzName + "#" + method.getName();

        try {
            return invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            LOGGER.info("Method {} elapsed {}ms", fullMethod, elapsed);
        }
    }
}