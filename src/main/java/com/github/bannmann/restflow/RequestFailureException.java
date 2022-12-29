package com.github.bannmann.restflow;

import java.net.http.HttpRequest;

/**
 * Thrown when the request failed without a response.
 *
 * @see RequestStatusException
 */
public class RequestFailureException extends RequestException
{
    public RequestFailureException(HttpRequest request, String message, Throwable cause)
    {
        super(request, message, cause);
    }
}
