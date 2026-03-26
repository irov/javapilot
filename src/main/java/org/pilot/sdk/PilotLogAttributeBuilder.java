package org.pilot.sdk;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder for log attributes — both static values and dynamic providers.
 * Static values are fixed at build time; dynamic providers are resolved at each log() call.
 *
 * <pre>{@code
 * PilotLogAttributeBuilder logAttrs = new PilotLogAttributeBuilder()
 *     .put("app_version", BuildConfig.VERSION_NAME)
 *     .putProvider("screen_type", () -> getScreenType());
 * }</pre>
 */
public final class PilotLogAttributeBuilder {
    private final Map<String, Object> m_staticAttributes = new LinkedHashMap<>();
    private final Map<String, PilotValueProvider> m_dynamicAttributes = new LinkedHashMap<>();

    @NonNull
    public PilotLogAttributeBuilder put(@NonNull String key, @NonNull Object value) {
        m_staticAttributes.put(key, value);
        return this;
    }

    @NonNull
    public PilotLogAttributeBuilder putProvider(@NonNull String key, @NonNull PilotValueProvider provider) {
        m_dynamicAttributes.put(key, provider);
        return this;
    }

    @NonNull
    Map<String, Object> getStaticAttributes() {
        return m_staticAttributes;
    }

    @NonNull
    Map<String, PilotValueProvider> getDynamicAttributes() {
        return m_dynamicAttributes;
    }
}
