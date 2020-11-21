package com.github.bannmann.restflow.util;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpRequests
{
    public HttpRequest.Builder toBuilder(HttpRequest request)
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder();

        setExpectContinue(builder, request);
        setHeaders(builder, request);
        setMethodAndBody(builder, request);
        setTimeout(builder, request);
        setUri(builder, request);
        setVersion(builder, request);

        return builder;
    }

    private void setExpectContinue(HttpRequest.Builder builder, HttpRequest request)
    {
        builder.expectContinue(request.expectContinue());
    }

    private void setHeaders(HttpRequest.Builder builder, HttpRequest request)
    {
        for (Map.Entry<String, List<String>> headerEntry : request.headers()
            .map()
            .entrySet())
        {
            for (String value : headerEntry.getValue())
            {
                builder.header(headerEntry.getKey(), value);
            }
        }
    }

    private void setMethodAndBody(HttpRequest.Builder builder, HttpRequest request)
    {
        builder.method(request.method(),
            request.bodyPublisher()
                .orElse(HttpRequest.BodyPublishers.noBody()));
    }

    private void setTimeout(HttpRequest.Builder builder, HttpRequest request)
    {
        request.timeout()
            .ifPresent(builder::timeout);
    }

    private void setUri(HttpRequest.Builder builder, HttpRequest request)
    {
        builder.uri(request.uri());
    }

    private void setVersion(HttpRequest.Builder builder, HttpRequest request)
    {
        request.version()
            .ifPresent(builder::version);
    }
}
