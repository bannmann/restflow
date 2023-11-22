package dev.bannmann.restflow;

import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ExecuteHandle
{
    private final RequestSpecification<?, Void> requestSpecification;

    public ExecuteHandle customizingRequest(RequestCustomizer requestCustomizer)
    {
        return new ExecuteHandle(requestSpecification.withCustomizer(requestCustomizer));
    }

    public CompletableFuture<Void> execute()
    {
        return RegularRequester.forSpec(requestSpecification)
            .start();
    }
}
