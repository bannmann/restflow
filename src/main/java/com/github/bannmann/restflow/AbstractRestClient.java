package com.github.bannmann.restflow;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.github.bannmann.restflow.util.Types;
import com.github.mizool.core.exception.InvalidBackendReplyException;
import com.github.mizool.core.rest.errorhandling.HttpStatus;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Policy;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
abstract class AbstractRestClient
{
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public final class RequestHandle
    {
        private final HttpRequest request;

        public <T> ConfigHandle<String, T> returning(Class<T> responseClass)
        {
            return new ConfigHandle<>(request,
                HttpResponse.BodyHandlers.ofString(),
                s -> clientConfig.getJsonb()
                    .fromJson(s, responseClass));
        }

        public <T> ConfigHandle<String, T> returning(Type runtimeType)
        {
            return new ConfigHandle<>(request,
                HttpResponse.BodyHandlers.ofString(),
                s -> clientConfig.getJsonb()
                    .fromJson(s, runtimeType));
        }

        public <T> ConfigHandle<String, List<T>> returningListOf(Class<T> elementClass)
        {
            return returning(Types.listOf(elementClass));
        }

        public ConfigHandle<String, String> returningString()
        {
            return new ConfigHandle<>(request, HttpResponse.BodyHandlers.ofString(), string -> string);
        }

        public ConfigHandle<InputStream, InputStream> returningInputStream()
        {
            return new ConfigHandle<>(request, HttpResponse.BodyHandlers.ofInputStream(), inputStream -> inputStream);
        }

        public CompletableFuture<Void> execute()
        {
            // Note: we use ofString() handler because discarding() would also discard the body of an error response.
            return new ConfigHandle<String, Void>(request, HttpResponse.BodyHandlers.ofString(), v -> null).fetch();
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public final class ConfigHandle<B, R>
    {
        private final HttpRequest request;
        private final HttpResponse.BodyHandler<B> bodyHandler;
        private final Function<B, R> responseConverter;

        public CompletableFuture<R> fetch()
        {
            return makeRequest(request, bodyHandler, response -> rejectAllErrors(response, request)).thenApply(
                HttpResponse::body)
                .thenApply(responseConverter);
        }

        public CompletableFuture<Optional<R>> tryFetch()
        {
            return makeRequest(request, bodyHandler, response -> rejectUnexpectedErrors(response, request)).thenApply(
                AbstractRestClient.this::tryGetBody)
                .thenApply(body -> body.map(responseConverter));
        }
    }

    protected final @NonNull ClientConfig clientConfig;

    private <T> CompletableFuture<HttpResponse<T>> makeRequest(
        HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler, Consumer<HttpResponse<?>> errorHandler)
    {
        return getFuture(request, bodyHandler, errorHandler);
    }

    private <T> void rejectUnexpectedErrors(HttpResponse<T> response, HttpRequest request)
    {
        int responseStatus = response.statusCode();
        if (isFailure(responseStatus) && responseStatus != HttpStatus.NOT_FOUND)
        {
            throw createException(response, request);
        }
    }

    private <T> void rejectAllErrors(HttpResponse<T> response, HttpRequest request)
    {
        int responseStatus = response.statusCode();
        if (isFailure(responseStatus))
        {
            throw createException(response, request);
        }
    }

    private boolean isFailure(int responseStatus)
    {
        return !isSuccess(responseStatus);
    }

    private boolean isSuccess(int responseStatus)
    {
        return responseStatus >= 200 && responseStatus < 300;
    }

    private <T> CompletableFuture<HttpResponse<T>> getFuture(
        HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler, Consumer<HttpResponse<?>> errorHandler)
    {
        List<Policy<HttpResponse<?>>> policies = clientConfig.getPolicies();
        if (!policies.isEmpty())
        {
            return Failsafe.with(policies)
                .getStageAsync(context -> getSingleAttemptFuture(request, bodyHandler, errorHandler));
        }

        return getSingleAttemptFuture(request, bodyHandler, errorHandler);
    }

    private <T> CompletableFuture<HttpResponse<T>> getSingleAttemptFuture(
        HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler, Consumer<HttpResponse<?>> errorHandler)
    {
        var future = sendAsync(request, bodyHandler);
        future = addDetailsToException(future, request);
        future = future.thenApply(response -> {
            errorHandler.accept(response);
            return response;
        });

        return future;
    }

    private <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler)
    {
        return clientConfig.getHttpClient()
            .sendAsync(request, bodyHandler);
    }

    private <T> CompletableFuture<T> addDetailsToException(CompletableFuture<T> future, HttpRequest request)
    {
        return future.handle((result, throwable) -> {
            if (throwable != null)
            {
                throw new RequestFailureException(String.format("Request to URL %s failed", request.uri()), throwable);
            }
            return result;
        });
    }

    private <T> InvalidBackendReplyException createException(HttpResponse<T> response, HttpRequest request)
    {
        return new InvalidBackendReplyException(String.format("Got status %d with message '%s' for URL %s",
            response.statusCode(),
            getStringBody(response),
            request.uri()));
    }

    private <T> String getStringBody(HttpResponse<T> response)
    {
        T body = response.body();
        if (body != null)
        {
            return body.toString()
                .trim();
        }
        return null;
    }

    private <T> Optional<T> tryGetBody(HttpResponse<T> response)
    {
        if (response.statusCode() == HttpStatus.NOT_FOUND)
        {
            return Optional.empty();
        }
        return Optional.of(response.body());
    }
}
