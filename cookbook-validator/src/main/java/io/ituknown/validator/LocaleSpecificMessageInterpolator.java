package io.ituknown.validator;

import jakarta.validation.MessageInterpolator;

import java.util.Locale;

/**
 * 固定 Locale 的消息插值器装饰器。
 *
 * <p>无论调用方传入何种 Locale，始终使用构造时指定的 {@code targetLocale} 进行消息解析。
 * 用于在 {@link ValidatorUtils} 中实现按 Locale 缓存 Validator 的场景。</p>
 */
public class LocaleSpecificMessageInterpolator implements MessageInterpolator {

    private final MessageInterpolator defaultInterpolator;
    private final Locale targetLocale;

    public LocaleSpecificMessageInterpolator(MessageInterpolator defaultInterpolator, Locale targetLocale) {
        this.defaultInterpolator = defaultInterpolator;
        this.targetLocale = targetLocale;
    }

    @Override
    public String interpolate(String messageTemplate, Context context) {
        return defaultInterpolator.interpolate(messageTemplate, context, targetLocale);
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        return defaultInterpolator.interpolate(messageTemplate, context, targetLocale);
    }
}
