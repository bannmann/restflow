package dev.bannmann.restflow;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * Thrown when the response is invalid. Refer to subclasses for specific cases.
 */
@Getter
public abstract class InvalidResponseException extends RequestException
{
    private final transient HttpResponse<?> response;

    protected InvalidResponseException(
        String message,
        Throwable cause,
        HttpResponse<?> response,
        Map<String, Object> diagnosticsData,
        List<StackWalker.StackFrame> callerFrames)
    {
        super(message, cause, diagnosticsData, callerFrames);
        this.response = response;
    }

    @Override
    public HttpRequest getRequest()
    {
        return response.request();
    }

    public int getStatusCode()
    {
        return response.statusCode();
    }

    public String getRawBody()
    {
        Object body = response.body();
        if (body == null)
        {
            return null;
        }
        return body.toString();
    }
}
