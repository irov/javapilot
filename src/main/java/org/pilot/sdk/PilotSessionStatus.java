package org.pilot.sdk;

public enum PilotSessionStatus {
    DISCONNECTED,
    CONNECTING,
    WAITING_APPROVAL,
    ACTIVE,
    AUTH_FAILED,
    REJECTED,
    CLOSED,
    ERROR
}
