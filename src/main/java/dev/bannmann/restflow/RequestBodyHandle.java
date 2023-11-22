package dev.bannmann.restflow;

import java.net.http.HttpRequest;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class RequestBodyHandle
{
    private final ClientConfig clientConfig;
    private final RequestTemplate requestTemplate;
    private final String method;
    private final String contentType;
    private final HttpRequest.BodyPublisher bodyPublisher;

    public RequestHandle to(@NonNull String resourcePath)
    {
        HttpRequest request = createUploadRequest(resourcePath);
        return new RequestHandle(request, clientConfig);
    }

    private HttpRequest createUploadRequest(String resourcePath)
    {
        return requestTemplate.newBuilder(resourcePath)
            .header("Content-Type", contentType)
            .method(method, bodyPublisher)
            .build();
    }
}
