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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UrlPermissionAspectTest.AppConfig.class)
class UrlPermissionAspectTest {

    @Autowired
    UserController userController;

    @Autowired
    FakeContextResolver resolver;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void hitAndGranted_proceeds() {
        resolver.setAuthorities(Set.of("user:read"));
        bindRequest("/api/users/1");

        Result<String> result = userController.get(new UserRequest());

        assertEquals("000000", result.getCode());
        assertEquals("ok", result.getData());
    }

    @Test
    void hitButDenied_wrapsToForbiddenResult() {
        resolver.setAuthorities(Set.of());
        bindRequest("/api/users/1");

        Result<String> result = userController.get(new UserRequest());

        // URI 命中规则但不持有 authority，抛 BizForbiddenException，由 around() 封装为 000403
        assertEquals(ResultCodes.FORBIDDEN.code(), result.getCode());
    }

    @Test
    void notHit_whitelisted() {
        resolver.setAuthorities(Set.of());
        bindRequest("/api/orders/1");

        Result<String> result = userController.get(new UserRequest());

        // URI 不匹配任何规则，白名单放行
        assertEquals("000000", result.getCode());
    }

    private static void bindRequest(String uri) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI(uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AppConfig {

        @Bean
        UserController userController() {
            return new UserController();
        }

        @Bean
        FakeContextResolver resolver() {
            return new FakeContextResolver();
        }

        @Bean
        UrlPermissionAspect urlPermissionAspect(FakeContextResolver resolver) {
            return new UrlPermissionAspect(resolver, new FakeRuleProvider());
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

    /** 测试用规则来源：{@code /api/users/**} 需要 {@code user:read}。 */
    static class FakeRuleProvider implements UrlPermissionRuleProvider {

        @Override
        public List<UrlPermissionRule> rules() {
            return List.of(new UrlPermissionRule("/api/users/**", "user:read"));
        }
    }
}

/** 测试用请求体。 */
class UserRequest extends AbstractRequest {
}

/** 测试用目标 bean，类标 {@code @RestController}，方法入参为 {@link AbstractRequest}。 */
@RestController
class UserController {

    Result<String> get(UserRequest request) {
        return ResultUtils.success("ok");
    }
}
