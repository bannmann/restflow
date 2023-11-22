package dev.bannmann.restflow;

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

    public CompletableFuture<R> fetch()
    {
        return RegularRequester.forSpec(requestSpecification)
            .start();
    }

    public CompletableFuture<Optional<R>> tryFetch()
    {
        return OptionalRequester.forSpec(requestSpecification)
            .start();
    }
}
