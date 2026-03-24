package org.pilot.sdk;

public enum PilotLogLevel {
    DEBUG("debug"),
    INFO("info"),
    WARNING("warning"),
    ERROR("error"),
    CRITICAL("critical"),
    EXCEPTION("exception");

    private final String m_value;

    PilotLogLevel(String value) {
        m_value = value;
    }

    public String getValue() {
        return m_value;
    }
}
