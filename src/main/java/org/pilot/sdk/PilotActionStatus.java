package org.pilot.sdk;

import androidx.annotation.NonNull;

public enum PilotActionStatus {
    PENDING("pending"),
    DELIVERED("delivered"),
    ACKNOWLEDGED("acknowledged"),
    UNKNOWN("");

    private final String m_value;

    PilotActionStatus(@NonNull String value) {
        m_value = value;
    }

    @NonNull
    public String getValue() {
        return m_value;
    }

    @NonNull
    static PilotActionStatus fromValue(@NonNull String value) {
        for (PilotActionStatus status : values()) {
            if (status.m_value.equals(value)) {
                return status;
            }
        }

        return UNKNOWN;
    }
}
