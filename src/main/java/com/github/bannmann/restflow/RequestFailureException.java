package com.github.bannmann.restflow;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import lombok.Getter;

/**
 * Thrown when the request failed without a response.
 *
 * @see RequestStatusException
 */
@Getter
public class RequestFailureException extends RequestException
{
    private final transient HttpResponse.ResponseInfo responseInfo;

    public RequestFailureException(
        HttpRequest request, HttpResponse.ResponseInfo responseInfo, String message, Throwable cause)
    {
        super(request, message, cause);
        this.responseInfo = responseInfo;
    }
}
