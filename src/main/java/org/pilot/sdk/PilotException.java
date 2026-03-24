package org.pilot.sdk;

/**
 * Pilot SDK exception.
 */
public class PilotException extends Exception {
    private final int m_httpCode;

    public PilotException(String message) {
        super(message);
        m_httpCode = 0;
    }

    public PilotException(String message, Throwable cause) {
        super(message, cause);
        m_httpCode = 0;
    }

    public PilotException(int httpCode, String message) {
        super(message);
        m_httpCode = httpCode;
    }

    public int getHttpCode() {
        return m_httpCode;
    }

    public boolean isNetworkError() {
        return m_httpCode == 0 && getCause() != null;
    }

    public boolean isSessionGone() {
        return m_httpCode == 410;
    }

    public boolean isUnauthorized() {
        return m_httpCode == 401;
    }
}
