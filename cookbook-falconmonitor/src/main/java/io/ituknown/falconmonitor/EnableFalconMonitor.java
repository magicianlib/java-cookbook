package io.ituknown.falconmonitor;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(FalconMonitorConfig.class)
public @interface EnableFalconMonitor {
}