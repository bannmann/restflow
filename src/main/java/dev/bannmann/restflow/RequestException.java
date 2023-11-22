package dev.bannmann.restflow;

import java.net.http.HttpRequest;
import java.util.Map;

import lombok.Getter;

/**
 * Thrown when the request could not be completed for some reason. Refer to {@link RequestStatusException} and
 * {@link RequestFailureException} for specific cases.
 */
@Getter
public abstract class RequestException extends RuntimeException
{
    private final transient HttpRequest request;
    private final transient Map<String, Object> diagnosticsData;

    protected RequestException(
        HttpRequest request, String message, Throwable cause, Map<String, Object> diagnosticsData)
    {
        super(message, cause);
        this.request = request;
        this.diagnosticsData = diagnosticsData;
    }
}
