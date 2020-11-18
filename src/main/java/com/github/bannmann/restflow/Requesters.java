package com.github.bannmann.restflow;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import com.github.mizool.core.exception.InvalidBackendReplyException;
import com.github.mizool.core.rest.errorhandling.HttpStatus;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Policy;

@Slf4j
@UtilityClass
class Requesters
{
    @RequiredArgsConstructor
    private abstract static class AbstractRequester<B, R> implements Requester<R>
    {
        protected final HttpRequest request;
        protected final ClientConfig clientConfig;

        @Override
        public final CompletableFuture<R> start()
        {
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

    private static class RegularRequester<B, R> extends AbstractRequester<B, R>
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

    private static class OptionalRequester<B, R> extends AbstractRequester<B, Optional<R>>
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

    public static <B, R> Requester<R> createRegular(
        ClientConfig clientConfig, RequesterConfig<B, R> requesterConfig, HttpRequest request)
    {
        return new RegularRequester<>(requesterConfig.getBodyHandler(),
            request,
            requesterConfig.getResponseConverter(),
            clientConfig);
    }

    public static <B, R> Requester<Optional<R>> createOptional(
        ClientConfig clientConfig, RequesterConfig<B, R> requesterConfig, HttpRequest request)
    {
        return new OptionalRequester<>(requesterConfig.getBodyHandler(),
            request,
            requesterConfig.getResponseConverter(),
            clientConfig);
    }
}
