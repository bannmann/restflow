package com.github.bannmann.restflow;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import com.github.mizool.core.exception.InvalidBackendReplyException;
import com.github.mizool.core.rest.errorhandling.HttpStatus;
import com.google.common.annotations.VisibleForTesting;

@Slf4j
@UtilityClass
class Requesters
{
    @VisibleForTesting
    public static class SpecialTimeoutException extends RuntimeException
    {
        public SpecialTimeoutException(URI uri)
        {
            super("OMG teh request timed out! url was " + uri);
        }
    }
    
    @RequiredArgsConstructor
    private abstract static class AbstractRequester<B, R> implements Requester<R>
    {
        private static final AtomicInteger threadCount = new AtomicInteger();
        private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10,
            runnable -> {
                String name = "restflow-" + threadCount.incrementAndGet();
                log.debug("Creating thread {}", name);

                Thread result = new Thread(runnable, name);
                result.setDaemon(true);
                return result;
            });
        protected final HttpRequest request;
        protected final ClientConfig clientConfig;

        @Override
        public final CompletableFuture<R> start()
        {
            return send().thenApply(this::extractValue);
        }

        private CompletableFuture<HttpResponse<B>> send()
        {
            /*
            List<Policy<HttpResponse<?>>> policies = clientConfig.getPolicies();
            if (!policies.isEmpty())
            {
                log.debug("Failsafe policies: {}", policies);
                return Failsafe.with(policies)
                    .with(EXECUTOR_SERVICE)
                    .getStageAsync(context -> sendOnce());
            }*/

            return sendOnceButAddTimeout();
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private CompletableFuture<HttpResponse<B>> sendOnceButAddTimeout()
        {
            CompletableFuture<HttpResponse<B>> future = sendOnce();
            CompletableFuture timeoutFuture = new CompletableFuture();
            EXECUTOR_SERVICE.schedule(() -> timeoutFuture.completeExceptionally(new SpecialTimeoutException(request.uri())),
                2_500,
                TimeUnit.MILLISECONDS);
            return (CompletableFuture) CompletableFuture.anyOf(future, timeoutFuture);
        }

        private CompletableFuture<HttpResponse<B>> sendOnce()
        {
            log.debug("sendOnce: {}", request.uri());
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
                throw new RequestFailureException(String.format("Request to URL %s failed", request.uri()), throwable);
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

        protected <T> InvalidBackendReplyException createException(HttpResponse<T> response)
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
    }

    private static final class RegularRequester<B, R> extends AbstractRequester<B, R>
    {
        @Getter
        private final HttpResponse.BodyHandler<B> bodyHandler;

        private final Function<B, R> responseConverter;

        private RegularRequester(
            HttpResponse.BodyHandler<B> bodyHandler,
            HttpRequest request,
            Function<B, R> responseConverter,
            ClientConfig clientConfig)
        {
            super(request, clientConfig);
            this.bodyHandler = bodyHandler;
            this.responseConverter = responseConverter;
        }

        @Override
        protected void verifyNoErrors(HttpResponse<B> response)
        {
            int responseStatus = response.statusCode();
            if (isFailure(responseStatus))
            {
                throw createException(response);
            }
        }

        @Override
        protected R extractValue(HttpResponse<B> response)
        {
            return responseConverter.apply(response.body());
        }
    }

    private static final class OptionalRequester<B, R> extends AbstractRequester<B, Optional<R>>
    {
        @Getter
        private final HttpResponse.BodyHandler<B> bodyHandler;

        private final Function<B, R> responseConverter;

        private OptionalRequester(
            HttpResponse.BodyHandler<B> bodyHandler,
            HttpRequest request,
            Function<B, R> responseConverter,
            ClientConfig clientConfig)
        {
            super(request, clientConfig);
            this.bodyHandler = bodyHandler;
            this.responseConverter = responseConverter;
        }

        @Override
        protected void verifyNoErrors(HttpResponse<B> response)
        {
            int responseStatus = response.statusCode();
            if (isFailure(responseStatus) && responseStatus != HttpStatus.NOT_FOUND)
            {
                throw createException(response);
            }
        }

        @Override
        protected Optional<R> extractValue(HttpResponse<B> response)
        {
            if (response.statusCode() == HttpStatus.NOT_FOUND)
            {
                return Optional.empty();
            }

            B body = response.body();
            R result = responseConverter.apply(body);
            return Optional.of(result);
        }
    }

    public static <B, R> Requester<R> createRegular(RequestSpecification<B, R> spec)
    {
        return new RegularRequester<>(spec.getResponseBodyConfig()
            .getBodyHandler(),
            spec.createFinalRequest(),
            spec.getResponseBodyConfig()
                .getResponseConverter(),
            spec.getClientConfig());
    }

    public static <B, R> Requester<Optional<R>> createOptional(RequestSpecification<B, R> spec)
    {
        return new OptionalRequester<>(spec.getResponseBodyConfig()
            .getBodyHandler(),
            spec.createFinalRequest(),
            spec.getResponseBodyConfig()
                .getResponseConverter(),
            spec.getClientConfig());
    }
}
