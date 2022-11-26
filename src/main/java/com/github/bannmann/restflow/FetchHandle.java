package com.github.bannmann.restflow;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class FetchHandle<R>
{
    private final RequestSpecification<?, R> requestSpecification;

    public FetchHandle<R> customizingRequest(RequestCustomizer requestCustomizer)
    {
        return new FetchHandle<>(requestSpecification.withCustomizer(requestCustomizer));
    }

    /**
     * TODO offer choice of
     *  - CompletableFuture<String> make(request).asynchronously().returningString().fetch()
     *  - String make(request).synchronously().returningString().fetch()
     *  and
     *  - CompletableFuture<String> post(dto).asynchronously().to("/foo").returningString().fetch()
     *  - String post(dto).synchronously().to("/foo").returningString().fetch()
     */
    public CompletableFuture<R> fetch()
    {
        return Requesters.createRegular(requestSpecification)
            .start();
    }

    public CompletableFuture<Optional<R>> tryFetch()
    {
        return Requesters.createOptional(requestSpecification)
            .start();
    }
}
