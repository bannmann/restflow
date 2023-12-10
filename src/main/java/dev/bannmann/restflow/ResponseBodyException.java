package dev.bannmann.restflow;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import lombok.Builder;

/**
 * Thrown when the response body could not be processed.
 */
public class ResponseBodyException extends InvalidResponseException
{
    @Builder
    public ResponseBodyException(
        String message,
        Throwable cause,
        HttpResponse<?> response,
        Map<String, Object> diagnosticsData,
        List<StackWalker.StackFrame> callerFrames)
    {
        super(message, cause, response, diagnosticsData, callerFrames);
    }
}
