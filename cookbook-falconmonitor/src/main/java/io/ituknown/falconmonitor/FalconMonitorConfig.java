package io.ituknown.falconmonitor;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.StringJoiner;

@Configuration
@EnableConfigurationProperties(FalconMonitorConfigurationProperties.class)
public class FalconMonitorConfig {

    @Bean("falconMonitorMethodInterceptor")
    public FalconMonitorMethodInterceptor falconMonitorMethodInterceptor(FalconMonitorConfigurationProperties properties) {
        return new FalconMonitorMethodInterceptor(properties.getAppId(), properties.getReportServerUrl());
    }

    @Bean("falconMonitorAspectJExpressionPointcut")
    public AspectJExpressionPointcut falconMonitorAspectJExpressionPointcut(FalconMonitorConfigurationProperties properties) {

        StringJoiner joiner = new StringJoiner(" || ");
        for (String expression : properties.getPointcutExpression()) {
            joiner.add(expression);
        }

        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(joiner.toString());
        return pointcut;
    }

    @Bean("falconMonitorPointcutAdvice")
    public Advisor falconMonitorPointcutAdvice(FalconMonitorConfigurationProperties properties) {
        return new DefaultPointcutAdvisor(falconMonitorAspectJExpressionPointcut(properties), falconMonitorMethodInterceptor(properties));
    }
}