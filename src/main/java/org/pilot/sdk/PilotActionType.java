package org.pilot.sdk;

import androidx.annotation.NonNull;

public enum PilotActionType {
    CLICK("click"),
    CHANGE("change"),
    TOGGLE("toggle"),
    UNKNOWN("");

    private final String m_value;

    PilotActionType(@NonNull String value) {
        m_value = value;
    }

    @NonNull
    public String getValue() {
        return m_value;
    }

    @NonNull
    static PilotActionType fromValue(@NonNull String value) {
        for (PilotActionType type : values()) {
            if (type.m_value.equals(value)) {
                return type;
            }
        }

        return UNKNOWN;
    }
}
