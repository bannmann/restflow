package com.github.bannmann.restflow;

import java.net.http.HttpRequest;

/**
 * Allows modifying an {@link HttpRequest} before it is sent.
 */
public interface RequestCustomizer
{
    /**
     * Allows modifying the request before it is sent. <br>
     * <br>
     * To retrieve the current value of a request property, invoke {@link HttpRequest.Builder#build()} and inspect the
     * resulting request.
     *
     * @param builder the builder representing the request to be sent.
     */
    void customize(HttpRequest.Builder builder);
}
