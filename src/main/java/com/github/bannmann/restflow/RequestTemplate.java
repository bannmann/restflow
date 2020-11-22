package com.github.bannmann.restflow;

import java.net.http.HttpRequest;

import lombok.NonNull;

import com.github.mizool.core.UrlRef;

final class RequestTemplate
{
    private final UrlRef baseUrl;
    private final HttpRequest.Builder builder;

    /**
     * @throws IllegalStateException if no URI has been set on {@code builder}
     */
    public RequestTemplate(@NonNull HttpRequest.Builder builder)
    {
        baseUrl = obtainBaseUrl(builder);
        this.builder = builder;
    }

    /**
     * @throws IllegalStateException if no URI has been set on {@code requestTemplate}
     */
    private UrlRef obtainBaseUrl(HttpRequest.Builder requestTemplate)
    {
        String spec = requestTemplate.build()
            .uri()
            .toString();

        if (!spec.endsWith("/"))
        {
            // Ensure the last path segment is not overwritten when building URLs based on this base URL
            spec = spec + "/";
        }

        return new UrlRef(spec);
    }

    public HttpRequest.Builder newBuilder(String resourcePath)
    {
        UrlRef resourceUrl = baseUrl.resolve(resourcePath);

        return builder.copy()
            .uri(resourceUrl.toUri());
    }
}
