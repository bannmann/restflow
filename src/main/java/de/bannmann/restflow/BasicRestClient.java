package de.bannmann.restflow;

import java.net.http.HttpRequest;

import lombok.Builder;
import lombok.NonNull;

public final class BasicRestClient extends AbstractRestClient
{
    @Builder
    protected BasicRestClient(@NonNull ClientConfig clientConfig)
    {
        super(clientConfig);
    }

    public RequestHandle make(HttpRequest request)
    {
        return new RequestHandle(request);
    }
}
