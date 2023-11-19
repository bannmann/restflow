package com.github.bannmann.restflow;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.RequiredArgsConstructor;

import dev.failsafe.Failsafe;
import dev.failsafe.Policy;

@RequiredArgsConstructor
abstract class AbstractRequester<B, R> implements Requester<R>
{
    protected final HttpRequest request;
    protected final ClientConfig clientConfig;
    protected final ConcurrentMap<String, Object> diagnosticsData = new ConcurrentHashMap<>();

    @Override
    public final CompletableFuture<R> start()
    {
        var diagnosticsDataSupplier = clientConfig.getDiagnosticsDataSupplier();
        if (diagnosticsDataSupplier != null)
        {
            Map<String, Object> values = diagnosticsDataSupplier.get();
            diagnosticsData.putAll(values);
        }

        return send().thenApply(this::extractValue);
    }

    private CompletableFuture<HttpResponse<B>> send()
    {
        List<Policy<HttpResponse<?>>> policies = clientConfig.getPolicies();
        if (!policies.isEmpty())
        {
            return Failsafe.with(policies)
                .getStageAsync(context -> sendOnce());
        }

        return sendOnce();
    }

    private CompletableFuture<HttpResponse<B>> sendOnce()
    {
        return clientConfig.getHttpClient()
            .sendAsync(request, getBodyHandler())
            .handle(this::addDetailsForLowLevelExceptions)
            .thenApply(this::failOrPassThrough);
    }

    protected abstract HttpResponse.BodyHandler<B> getBodyHandler();

    private <T> T addDetailsForLowLevelExceptions(T result, Throwable throwable)
    {
        if (throwable != null)
        {
            String message = String.format("Request to URL %s failed", request.uri());
            throw new RequestFailureException(request, message, throwable, diagnosticsData);
        }
        return result;
    }

    private HttpResponse<B> failOrPassThrough(HttpResponse<B> response)
    {
        verifyNoErrors(response);
        return response;
    }

    protected abstract void verifyNoErrors(HttpResponse<B> response);

    protected abstract R extractValue(HttpResponse<B> response);

    protected boolean isFailure(int responseStatus)
    {
        return !isSuccess(responseStatus);
    }

    protected boolean isSuccess(int responseStatus)
    {
        return responseStatus >= 200 && responseStatus < 300;
    }

    protected <T> RequestStatusException createException(HttpResponse<T> response)
    {
        int status = response.statusCode();
        String body = getStringBody(response);
        String message = String.format("Got status %d with message '%s' for %s %s",
            status,
            body,
            request.method(),
            request.uri());

        return RequestStatusException.builder()
            .message(message)
            .request(request)
            .response(response)
            .status(status)
            .body(body)
            .diagnosticsData(diagnosticsData)
            .build();
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
}
