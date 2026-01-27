package io.ituknown.validator;

import jakarta.validation.MessageInterpolator;

import java.util.Locale;

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
        // 强制使用外部传入的 Locale
        return defaultInterpolator.interpolate(messageTemplate, context, targetLocale);
    }
}