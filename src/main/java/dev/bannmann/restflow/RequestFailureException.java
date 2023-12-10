package dev.bannmann.restflow;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * Thrown when the request failed without a response.
 */
@Getter
public class RequestFailureException extends RequestException
{
    private final HttpRequest request;

    public RequestFailureException(
        HttpRequest request,
        String message,
        Throwable cause,
        Map<String, Object> diagnosticsData,
        List<StackWalker.StackFrame> callerFrames)
    {
        super(message, cause, diagnosticsData, callerFrames);
        this.request = request;
    }
}
