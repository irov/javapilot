package org.pilot.sdk;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder for session attributes — both static values and dynamic providers.
 *
 * <pre>{@code
 * PilotSessionAttributeBuilder attrs = new PilotSessionAttributeBuilder()
 *     .put("install_id", installId)
 *     .put("session_index", String.valueOf(sessionIndex))
 *     .putProvider("acquisition_network", application::getAcquisitionNetwork);
 * }</pre>
 */
public final class PilotSessionAttributeBuilder {
    private final Map<String, String> m_staticAttributes = new LinkedHashMap<>();
    private final Map<String, PilotValueProvider> m_dynamicAttributes = new LinkedHashMap<>();

    @NonNull
    public PilotSessionAttributeBuilder put(@NonNull String key, @NonNull String value) {
        m_staticAttributes.put(key, value);
        return this;
    }

    @NonNull
    public PilotSessionAttributeBuilder putProvider(@NonNull String key, @NonNull PilotValueProvider provider) {
        m_dynamicAttributes.put(key, provider);
        return this;
    }

    @NonNull
    Map<String, String> getStaticAttributes() {
        return m_staticAttributes;
    }

    @NonNull
    Map<String, PilotValueProvider> getDynamicAttributes() {
        return m_dynamicAttributes;
    }
}
