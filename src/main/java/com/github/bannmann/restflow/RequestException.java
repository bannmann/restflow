package com.github.bannmann.restflow;

import java.net.http.HttpRequest;

import lombok.Getter;

/**
 * Thrown when the request could not be completed for some reason. Refer to {@link RequestStatusException} and
 * {@link RequestFailureException} for specific cases.
 */
@Getter
public abstract class RequestException extends RuntimeException
{
    private final transient HttpRequest request;

    protected RequestException(HttpRequest request, String message, Throwable cause)
    {
        super(message, cause);
        this.request = request;
    }
}
