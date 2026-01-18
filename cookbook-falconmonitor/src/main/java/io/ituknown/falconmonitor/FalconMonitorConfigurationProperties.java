package io.ituknown.falconmonitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "falcon.monitor")
public class FalconMonitorConfigurationProperties {

    /**
     * 应用名称
     */
    @NotBlank
    private String appId;

    /**
     * 服务地址
     * <p>
     * 示例：{@code localhost:8080}
     */
    @NotBlank
    private String reportServerUrl;

    /**
     * 切点表达式
     * <p>
     * 示例：{@code * com.example.demo.web..*Web.*(..)}
     */
    @NotEmpty
    private List<String> pointcutExpression = new ArrayList<>();

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getReportServerUrl() {
        return reportServerUrl;
    }

    public void setReportServerUrl(String reportServerUrl) {
        this.reportServerUrl = reportServerUrl;
    }

    public List<String> getPointcutExpression() {
        return pointcutExpression;
    }

    public void setPointcutExpression(List<String> pointcutExpression) {
        this.pointcutExpression = pointcutExpression;
    }
}