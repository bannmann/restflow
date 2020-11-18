package com.github.bannmann.restflow;

import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ConfigHandle<R>
{
    private final HttpRequest request;
    private final RequesterConfig<?, R> requesterConfig;
    private final ClientConfig clientConfig;

    public CompletableFuture<R> fetch()
    {
        return Requesters.createRegular(clientConfig, requesterConfig, request)
            .start();
    }

    public CompletableFuture<Optional<R>> tryFetch()
    {
        return Requesters.createOptional(clientConfig, requesterConfig, request)
            .start();
    }
}
