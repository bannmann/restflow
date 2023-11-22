package dev.bannmann.restflow;

import java.net.http.HttpResponse;
import java.util.Optional;

import com.github.mizool.core.rest.errorhandling.HttpStatus;

final class OptionalRequester<B, R> extends AbstractRequester<B, Optional<R>>
{
    public static <B, R> Requester<Optional<R>> forSpec(RequestSpecification<B, R> spec)
    {
        return new OptionalRequester<>(spec);
    }

    private final RequestSpecification<B, R> spec;

    private OptionalRequester(RequestSpecification<B, R> spec)
    {
        super(spec.createFinalRequest(), spec.getClientConfig());
        this.spec = spec;
    }

    @Override
    protected HttpResponse.BodyHandler<B> getBodyHandler()
    {
        return spec.getResponseBodyConfig()
            .getBodyHandler();
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
        R result = spec.getResponseBodyConfig()
            .getResponseConverter()
            .apply(body);
        return Optional.of(result);
    }
}
