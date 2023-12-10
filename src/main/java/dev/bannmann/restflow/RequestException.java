package dev.bannmann.restflow;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * Thrown when the request could not be completed for some reason. Refer to subclasses for specific cases.
 */
@Getter
public abstract class RequestException extends RuntimeException
{
    private final transient Map<String, Object> diagnosticsData;
    private final transient List<StackWalker.StackFrame> callerFrames;

    protected RequestException(
        String message,
        Throwable cause,
        Map<String, Object> diagnosticsData,
        List<StackWalker.StackFrame> callerFrames)
    {
        super(message, cause);
        this.diagnosticsData = diagnosticsData;
        this.callerFrames = callerFrames;
    }

    public abstract HttpRequest getRequest();
}
