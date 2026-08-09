package io.ituknown.aspect;

import io.ituknown.payload.AbstractRequest;
import io.ituknown.payload.Result;
import io.ituknown.payload.ResultCodes;
import io.ituknown.payload.ResultUtils;
import io.ituknown.payload.exception.BizForbiddenException;
import io.ituknown.payload.exception.BizNotFoundException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AbstractResultAspectTest.AppConfig.class)
class AbstractResultAspectTest {

    @Autowired
    EchoService echoService;

    @Autowired
    EchoAspect echoAspect;

    @BeforeEach
    void clearCalls() {
        echoAspect.calls.clear();
    }

    @Test
    void around_invokesPreThenPost_andPassesThroughResult() {
        EchoRequest request = newRequest("hi");

        Result<String> result = echoService.echo(request);

        assertEquals("000000", result.getCode());
        assertEquals("hi", result.getData());
        // 前置 -> 目标 -> 后置，且 Result 透传到后置
        assertEquals(List.of("pre:hi", "post:hi"), echoAspect.calls);
    }

    @Test
    void preProceed_bizException_wrappedToFailureResult() {
        EchoRequest request = newRequest("forbidden");

        Result<String> result = echoService.echo(request);

        assertEquals(ResultCodes.FORBIDDEN.code(), result.getCode());
        assertNull(result.getData());
        // 前置抛业务异常被捕获并封装为失败 Result，目标与后置均不执行
        assertEquals(List.of("pre:forbidden"), echoAspect.calls);
    }

    @Test
    void target_bizException_wrappedToFailureResult() {
        EchoRequest request = newRequest("missing");

        Result<String> result = echoService.echo(request);

        assertEquals(ResultCodes.NOT_FOUND.code(), result.getCode());
        // 前置已执行，目标抛业务异常被捕获并封装为失败 Result，后置不执行
        assertEquals(List.of("pre:missing"), echoAspect.calls);
    }

    @Test
    void nonBizException_propagates() {
        EchoRequest request = newRequest("unexpected");

        assertThrows(IllegalStateException.class, () -> echoService.echo(request));
        // 非业务异常不被捕获，原样上抛；前置已执行
        assertEquals(List.of("pre:unexpected"), echoAspect.calls);
    }

    private static EchoRequest newRequest(String payload) {
        EchoRequest request = new EchoRequest();
        request.payload = payload;
        return request;
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AppConfig {

        @Bean
        EchoService echoService() {
            return new EchoService();
        }

        @Bean
        EchoAspect echoAspect() {
            return new EchoAspect();
        }
    }
}

/** 测试用请求体。 */
class EchoRequest extends AbstractRequest {
    String payload;
}

/** 测试用目标 bean，方法契约：入参 EchoRequest，返回 Result。 */
class EchoService {

    Result<String> echo(EchoRequest request) {
        if ("missing".equals(request.payload)) {
            throw new BizNotFoundException();
        }
        if ("unexpected".equals(request.payload)) {
            throw new IllegalStateException("boom");
        }
        return ResultUtils.success(request.payload);
    }
}

/** 具体子类切面，记录调用顺序以验证模板。 */
@Aspect
@Order(10)
class EchoAspect extends AbstractResultAspect {

    final List<String> calls = new ArrayList<>();

    @Override
    @Pointcut("execution(* io.ituknown.aspect.EchoService.echo(..)) && args(request)")
    public void pointcut(AbstractRequest request) {
    }

    @Override
    protected void preProceed(ProceedingJoinPoint joinPoint, AbstractRequest request) {
        EchoRequest req = (EchoRequest) request;
        calls.add("pre:" + req.payload);
        if ("forbidden".equals(req.payload)) {
            throw new BizForbiddenException();
        }
    }

    @Override
    protected void postProceed(ProceedingJoinPoint joinPoint, AbstractRequest request, Result<?> result) {
        calls.add("post:" + result.getData());
    }
}
