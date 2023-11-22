package dev.bannmann.restflow;

import java.util.concurrent.CompletableFuture;

interface Requester<T>
{
    CompletableFuture<T> start();
}
