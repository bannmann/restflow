package com.github.bannmann.restflow;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import lombok.Builder;
import lombok.Getter;

/**
 * Thrown when the response to a request has a non-2xx status code.
 *
 * @see RequestFailureException
 */
@Getter
public class RequestStatusException extends RequestException
{
    private final int status;
    private final transient HttpResponse<?> response;
    private final String body;

    @Builder
    public RequestStatusException(
        String message, Throwable cause, HttpRequest request, HttpResponse<?> response, int status, String body)
    {
        super(request, message, cause);

        this.response = response;
        this.status = status;
        this.body = body;
    }
}
