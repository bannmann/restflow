package com.github.bannmann.restflow;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import com.github.bannmann.restflow.util.Types;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class RequestHandle
{
    private final HttpRequest request;
    private final ClientConfig clientConfig;

    public <T> ConfigHandle<T> returning(Class<T> responseClass)
    {
        return new ConfigHandle<>(request,
            new RequesterConfig<>(HttpResponse.BodyHandlers.ofString(),
                s -> clientConfig.getJsonb()
                    .fromJson(s, responseClass)),
            clientConfig);
    }

    public <T> ConfigHandle<T> returning(Type runtimeType)
    {
        return new ConfigHandle<>(request,
            new RequesterConfig<>(HttpResponse.BodyHandlers.ofString(),
                s -> clientConfig.getJsonb()
                    .fromJson(s, runtimeType)),
            clientConfig);
    }

    public <T> ConfigHandle<List<T>> returningListOf(Class<T> elementClass)
    {
        return returning(Types.listOf(elementClass));
    }

    public ConfigHandle<String> returningString()
    {
        return new ConfigHandle<>(request,
            new RequesterConfig<>(HttpResponse.BodyHandlers.ofString(), string -> string),
            clientConfig);
    }

    public ConfigHandle<InputStream> returningInputStream()
    {
        return new ConfigHandle<>(request,
            new RequesterConfig<>(HttpResponse.BodyHandlers.ofInputStream(), inputStream -> inputStream),
            clientConfig);
    }

    public CompletableFuture<Void> execute()
    {
        // Note: we use ofString() handler because discarding() would also discard the body of an error response.
        return new ConfigHandle<Void>(request,
            new RequesterConfig<>(HttpResponse.BodyHandlers.ofString(), v -> null),
            clientConfig).fetch();
    }
}
