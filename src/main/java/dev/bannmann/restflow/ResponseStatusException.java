package dev.bannmann.restflow;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import lombok.Builder;

/**
 * Thrown when the response to a request has a non-2xx status code.
 */
public class ResponseStatusException extends InvalidResponseException
{
    @Builder
    public ResponseStatusException(
        String message,
        Throwable cause,
        HttpResponse<?> response,
        Map<String, Object> diagnosticsData,
        List<StackWalker.StackFrame> callerFrames)
    {
        super(message, cause, response, diagnosticsData, callerFrames);
    }
}
