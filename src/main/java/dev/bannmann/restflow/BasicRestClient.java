package dev.bannmann.restflow;

import java.net.http.HttpRequest;

import lombok.Builder;
import lombok.NonNull;

@Builder
public final class BasicRestClient
{
    private final @NonNull ClientConfig clientConfig;

    public RequestHandle make(HttpRequest request)
    {
        return new RequestHandle(request, clientConfig);
    }
}
