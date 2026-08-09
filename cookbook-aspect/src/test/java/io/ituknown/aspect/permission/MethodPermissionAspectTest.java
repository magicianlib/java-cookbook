package io.ituknown.aspect.permission;

import io.ituknown.payload.AbstractRequest;
import io.ituknown.payload.Result;
import io.ituknown.payload.ResultCodes;
import io.ituknown.payload.ResultUtils;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MethodPermissionAspectTest.AppConfig.class)
class MethodPermissionAspectTest {

    @Autowired
    SecuredService securedService;

    @Autowired
    FakeContextResolver resolver;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void granted_proceeds() {
        resolver.setAuthorities(Set.of("user:read"));
        bindRequest();

        Result<String> result = securedService.read(new ReadRequest());

        assertEquals("000000", result.getCode());
        assertEquals("ok", result.getData());
    }

    @Test
    void denied_wrapsToForbiddenResult() {
        resolver.setAuthorities(Set.of());
        bindRequest();

        Result<String> result = securedService.read(new ReadRequest());

        // 权限不足抛 BizForbiddenException，由 around() 封装为 000403
        assertEquals(ResultCodes.FORBIDDEN.code(), result.getCode());
    }

    private static void bindRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AppConfig {

        @Bean
        SecuredService securedService() {
            return new SecuredService();
        }

        @Bean
        FakeContextResolver resolver() {
            return new FakeContextResolver();
        }

        @Bean
        MethodPermissionAspect methodPermissionAspect(FakeContextResolver resolver) {
            return new MethodPermissionAspect(resolver);
        }
    }

    /** 测试用解析器，可切换当前 authorities。 */
    static class FakeContextResolver implements PermissionContextResolver {

        private Set<String> authorities = Set.of();

        void setAuthorities(Set<String> authorities) {
            this.authorities = authorities;
        }

        @Override
        public PermissionContext resolve(HttpServletRequest request) {
            return new PermissionContext("tester", authorities);
        }
    }
}

/** 测试用请求体。 */
class ReadRequest extends AbstractRequest {
}

/** 测试用目标 bean，方法标注所需权限。 */
class SecuredService {

    @RequirePermission("user:read")
    Result<String> read(ReadRequest request) {
        return ResultUtils.success("ok");
    }
}
