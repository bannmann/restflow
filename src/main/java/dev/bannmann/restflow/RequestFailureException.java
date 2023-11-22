package dev.bannmann.restflow;

import java.net.http.HttpRequest;
import java.util.Map;

/**
 * Thrown when the request failed without a response.
 *
 * @see RequestStatusException
 */
public class RequestFailureException extends RequestException
{
    public RequestFailureException(
        HttpRequest request, String message, Throwable cause, Map<String, Object> diagnosticsData)
    {
        super(request, message, cause, diagnosticsData);
    }
}
